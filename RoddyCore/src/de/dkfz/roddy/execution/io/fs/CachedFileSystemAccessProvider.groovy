/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.io.fs

import de.dkfz.roddy.Roddy
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.tools.LoggerWrapper
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.WildcardFileFilter

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.sql.*
import java.util.logging.Logger

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING

/**
 * Cached version of the FileSystemAccessManager.
 * Caches some values but not everything.
 * Currently stores some parts of the cache in a local database (h2, hardcoded!)
 * TODO Make database configurable?
 * TODO pull out and simplify code for database access.
 */
@groovy.transform.CompileStatic
public class CachedFileSystemAccessProvider extends FileSystemAccessProvider {

    private static LoggerWrapper logger = LoggerWrapper.getLogger(CachedFileSystemAccessProvider.getClass().getSimpleName());
    private static final String TBL_FILECHACHEINFO = (CachedFileSystemAccessProvider.class.getSimpleName() + "_fileCacheInfo").toUpperCase();
    private static final String TBL_RUNOWNER = (CachedFileSystemAccessProvider.class.getSimpleName() + "_runOwner").toUpperCase();
    public static final String FORMATSTRING_FILELISTENTRY = "fileList_%08X"
    public static final String FORMATSTRING_TEXTFILEENTRY = "textFile_%08X"

    private class CacheEntry<F, T> {
        public long creationTimestamp;

        public F key;

        public T entry;

        CacheEntry(F key, T entry) {
            this.creationTimestamp = System.nanoTime();
            this.key = key
            this.entry = entry
        }

        public boolean isOutdated(long compValue) {
            long diff = System.nanoTime() - creationTimestamp;
            double age = diff / 10000000 / 100.0;
            boolean outDated = ((double) age) > compValue;
            if (outDated)
                logger.info("CacheEntry for ${key} is too old and will be refreshed. CacheEntrys age is ${age} [max:${compValue}] seconds.")
            return outDated;
        }
    }

    private Map<File, CacheEntry<File, List<File>>> _filesInDirectoryCache = [:];

    private Map<File, Object> _fileDeserializationCache = [:];

    private Map<File, String[]> _fileTextContentCache = [:];

    private Map<File, String> _pathOwnerCache = [:];

    private static Connection conn = null;

    private void initializeCacheDB() {
        if(conn != null)
            return;
        Class.forName("org.h2.Driver");
        String dbFile = Roddy.getFileCacheDirectory().getAbsolutePath() + File.separator + "filecache.h2.db";
        final dbConnString = "jdbc:h2:" + dbFile + ";AUTO_SERVER=TRUE"
        logger.info("Connecting to local caching database instance ${dbConnString}")
        conn = DriverManager.getConnection(dbConnString, "sa", "");
        final Statement statement = conn.createStatement()
        DatabaseMetaData dbm = conn.getMetaData();
        final ResultSet tables = dbm.getTables(null, null, TBL_FILECHACHEINFO.toUpperCase(), null);
        if (!tables.next()) {
            String sql = "CREATE TABLE ${TBL_FILECHACHEINFO} ( SRCNAME LONGVARCHAR(4096), TMPNAME LONGVARCHAR(4096), LASTLOCALACCESS VARCHAR(255), ACCESSCOUNT INT )";
            statement.execute(sql); //TODO: Handle errors

            sql = "CREATE TABLE ${TBL_RUNOWNER} ( RUNFOLDER LONGVARCHAR(4096), OWNER VARCHAR(255) )";
            statement.execute(sql); //TODO: Handle errors
        }
        tables.close();
    }

    public CachedFileSystemAccessProvider() {
        //Connect to a database
        initializeCacheDB()

//        conn.close();
    }

    @Override
    void destroy() {
        try {
            if(conn != null)
            conn.close();
        } catch (Exception ex) {
            logger.severe("Could not destroy " + getClass().getName() + "\n" + ex);
        }
    }

    @Override
    List<File> listFilesInDirectory(File f) {
        return listFilesInDirectory(f, []);
    }

    @Override
    List<File> listFilesInDirectory(File f, List<String> filters) {
        String id = String.format(FORMATSTRING_FILELISTENTRY, f.absolutePath.hashCode());
        synchronized (_filesInDirectoryCache) {
            if (!_filesInDirectoryCache.containsKey(f) || _filesInDirectoryCache[f].isOutdated(30)) {
                List<File> files = super.listFilesInDirectory(f, null);
                _filesInDirectoryCache[f] = new CacheEntry<File, List<File>>(f, files);
                fireCacheValueAddedEvent(id, "${f.absolutePath}: List<File> of size ${files.size()}");
            }
        }
        fireCacheValueReadEvent(id, -1);
        if (_filesInDirectoryCache[f].entry.size() > 0 && filters) {
            WildcardFileFilter wff = new WildcardFileFilter(filters.toArray(new String[0]));
            List<File> finalFiles = [];
            for (File file in _filesInDirectoryCache[f].entry) {
                if (wff.accept(file)) finalFiles.add(file);
            }
            return finalFiles;
        } else
            return new LinkedList<File>(_filesInDirectoryCache[f].entry);
    }

    /**
     * Check if a file is either available locally or if not is available in a cache directory
     * Returns the path to the local cache file.
     * @param f
     * @return
     */
    private File isFileAvailableLocally(File f) {
        // If we have a local execution service, we can directly open the file.
        if (ExecutionService.getInstance().isLocalService())
            return f;

//        RemoteExecutionService res = (RemoteExecutionService) Roddy.getInstance();

        // If we have a remote service the file could have been downloaded once. We check this here.
        File result = null;
        String sql = "SELECT * FROM ${TBL_FILECHACHEINFO} WHERE SRCNAME='${f.absolutePath}' ";
        final Statement statement = conn.createStatement();
        final ResultSet query = statement.executeQuery(sql);
        if (query.next()) {
            query.getString("SRCNAME");
            result = new File(query.getString("TMPNAME"));
            query.getString("LASTLOCALACCESS");
            query.getInt("ACCESSCOUNT");
        }
        query.close();
        statement.close();

        return result;
    }

    private void putFileToLocalCache(File f) {
        File tempFile = getTemporaryFileForFile(f);
        String originalPath = f.absolutePath;
        String targetPath = null;
        if (f && tempFile) {
            Path src = FileSystems.getDefault().getPath(tempFile.absolutePath);
            final String dstFileName = Roddy.getFileCacheDirectory().getAbsolutePath() + File.separator + tempFile.getName()
            Path dst = FileSystems.getDefault().getPath(dstFileName);
            Files.copy(src, dst, REPLACE_EXISTING);
            targetPath = dstFileName;
        }

        String sql = "INSERT INTO ${TBL_FILECHACHEINFO} (SRCNAME, TMPNAME, LASTLOCALACCESS, ACCESSCOUNT) VALUES ('${originalPath}', '${targetPath}', '${System.nanoTime()}', 1) ";
        final Statement statement = conn.createStatement();
        statement.execute(sql);
        statement.close();
    }

    private String[] readTextFromLocalCacheFile(File f) {
        if (!f.exists()) return null;
        return FileUtils.readLines(f).toArray(new String[0]);
    }

    private Object readObjectFromLocalCacheFile(File f) {
        if (f.exists())
            return FileSystemAccessProvider.deserializeObjectFromFile(f);
        return null;
    }

    @Override
    Object loadBinaryFile(File file) {
        String id = String.format("binaryFile_%08X", file.absolutePath.hashCode());
        if (!_fileDeserializationCache.containsKey(file)) {
            Object o = null;
            File localFile = null;
            if ((localFile = isFileAvailableLocally(file)) != null) {
                //Load temporary file from file system info provider (or executionService)
                o = readObjectFromLocalCacheFile(localFile);
            } else {
                o = super.loadBinaryFile(file);
                putFileToLocalCache(file);
            }
            _fileDeserializationCache[file] = o;
            if (o != null)
                fireCacheValueAddedEvent(id, "Object of type ${o.getClass().getName()}");
            else
                fireCacheValueAddedEvent(id, "Invalid object with null value");
        }
        fireCacheValueReadEvent(id, -1);
        return _fileDeserializationCache[file];
    }

    @Override
    String[] loadTextFile(File file) {
        String id = String.format(FORMATSTRING_TEXTFILEENTRY, file.absolutePath.hashCode());
        if (!_fileTextContentCache.containsKey(file)) {
            String[] text = null;
            if (isCachingAllowed(file)) {
                File localFile = null;
                if ((localFile = isFileAvailableLocally(file)) != null) {
                    //Load temporary file from file system info provider (or executionService)
                    text = readTextFromLocalCacheFile(localFile);
//                o = getFileFromLocalCache(file);
                } else {
                    text = super.loadTextFile(file);
                    putFileToLocalCache(file);
                }
            } else {
                text = super.loadTextFile(file);
            }
            _fileTextContentCache.put(file, text);
            if (text != null)
                try {
                    fireCacheValueAddedEvent(id, "Text array of size ${_fileTextContentCache[file].length}");
                } catch(Exception ex) {
                    logger.severe(ex.toString());
                }
            else
                fireCacheValueAddedEvent(id, "Invalid (null) text array, size 0");
        }
        fireCacheValueReadEvent(id, -1);
        return _fileTextContentCache[file];
    }

    private String getOwnerFromLocalCache(File path) {
        final Statement statement = conn.createStatement();
        final ResultSet resultSet = statement.executeQuery("SELECT RUNFOLDER, OWNER FROM ${TBL_RUNOWNER} WHERE RUNFOLDER='${path.absolutePath}'");
        String owner = null;
        if (resultSet.next()) {
            owner = resultSet.getString("OWNER");
        }

        resultSet.close();
        statement.close();
        return owner;
    }

    private void putOwnerToLocalCache(File path, String owner) {
        final Statement statement = conn.createStatement();
        statement.execute("INSERT INTO ${TBL_RUNOWNER} (RUNFOLDER, OWNER) VALUES ('${path.absolutePath}', '${owner}')");
        statement.close();
    }

    @Override
    String getOwnerOfPath(File file) {
        String id = String.format("pathOwner_%08X", file.absolutePath.hashCode());
        if (!_pathOwnerCache.containsKey(file)) {
            String owner = null;
            owner = getOwnerFromLocalCache(file);
            if ("null".equals(owner)) {
                owner = null;
            } else if (owner == null) {
                owner = super.getOwnerOfPath(file)
                putOwnerToLocalCache(file, owner);
            }
            _pathOwnerCache.put(file, owner);
            fireCacheValueAddedEvent(id, "${owner}");
        }
        fireCacheValueReadEvent(id, -1);
        return _pathOwnerCache.get(file)    //To change body of overridden methods use File | Settings | File Templates.
    }
//
//    @Override
//    boolean providesCacheMechanisms() {
//        return true;
//    }
//
//    @Override
//    void eraseCache() {
//    }
}
