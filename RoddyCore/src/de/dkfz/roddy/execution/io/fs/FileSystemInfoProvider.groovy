package de.dkfz.roddy.execution.io.fs

import de.dkfz.roddy.Constants
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.config.RecursiveOverridableMapContainerForConfigurationValues
import de.dkfz.roddy.config.converters.ConfigurationConverter
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import de.dkfz.roddy.core.CacheProvider
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.execution.io.ExecutionResult
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.FileAttributes
import de.dkfz.roddy.knowledge.files.BaseFile
import org.apache.commons.io.filefilter.WildcardFileFilter

/**
 * This class provides access to the file system and collects information for easy access.
 *
 * TODO: A lot of the methods below do not work with the standard java file system classes.
 * I.e. setting file access rights is only available with SSH. This needs to be completed!
 */
@groovy.transform.CompileStatic
public class FileSystemInfoProvider extends CacheProvider {
    private static LoggerWrapper logger = LoggerWrapper.getLogger(FileSystemInfoProvider.getClass().getName());
    private static FileSystemInfoProvider fileSystemInfoProvider = null;

    /**
     * This is the command set assembler for the current target file system.
     * As currently only Linux target fs are supported it is assigned by default.
     * TODO Initialize this in a different way!
     */
    protected final FileSystemCommandSet commandSet = new LinuxFileSystemCommandSet();

    /**
     * The current users user name (logon on local or target system)
     */
    private String _userName;

    /**
     * The first group id in a list of groups on the target system.
     */
    private String _groupID;

    /**
     * The current users home directory (logon on local or target system)
     */
    private File _userHome;

    /**
     * A small cache which keeps track of checkDirectory queries
     */
    private Map<String, Boolean> _directoryExistsAndIsAccessible = new LinkedHashMap<>();

    public FileSystemInfoProvider() {
        super("FileSystemInfoProvider", true);
    }

    public static void initializeProvider(boolean fullSetup) {
        if (!fullSetup) {
            fileSystemInfoProvider = new NoNoFileSystemInfoProvider();
        }
        try {
            Class fisClz = LibrariesFactory.getGroovyClassLoader().loadClass(Roddy.getApplicationProperty(Roddy.getRunMode(), Constants.APP_PROPERTY_FILESYSTEM_INFO_PROVIDER_CLASS, FileSystemInfoProvider.class.getName()));
            fileSystemInfoProvider = (FileSystemInfoProvider) fisClz.getConstructors()[0].newInstance();
        } catch (Exception e) {
            logger.warning("Falling back to default file system info provider");
            fileSystemInfoProvider = new FileSystemInfoProvider();
        }
    }

    public static File writeTextToTempFile(String text) {
        File tempFile = File.createTempFile("roddy_sshservice", ".tmp");
        tempFile.deleteOnExit();
        BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
        bw.write(text);
        bw.flush();
        bw.close();
        return tempFile;
    }

    public static File serializeObjectToTempFile(Serializable serializable) {
        File tempFile = File.createTempFile("roddy_sshservice", ".tmp");
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempFile));
        oos.writeObject(serializable);
        oos.flush();
        oos.close();
        return tempFile;
    }

    public static Object deserializeObjectFromFile(File f) {
        if (f == null || !f.exists())
            return null;
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
        return ois.readObject();
    }

    public static FileSystemInfoProvider getInstance() {
        return fileSystemInfoProvider;
    }

    @Override
    boolean initialize() {

    }

    @Override
    void destroy() {

    }

    public boolean isAccessRightsModificationAllowed(ExecutionContext context) {
        return context.getConfiguration().getConfigurationValues().getBoolean("outputAllowAccessRightsModification", true);
    }

    /**
     * Wrapper for isReadable(File)
     * @param bf
     * @return
     */
    public boolean isReadable(BaseFile bf) {
        return isReadable(bf.path);
    }

    private boolean runFileTestCommand(String cmd) {
        ExecutionResult er = ExecutionService.getInstance().execute(cmd);
        return er.firstLine == commandSet.getReadibilityTestPositiveResult();
    }

    public boolean isCachingAllowed(File file) {
        List<String> filters = [".roddyExecCache", ".roddy", "jobStateLogfile", "JobCalls", "zippedAnalysesMD5.txt"];

        for (String f in filters) {
            if (file.absolutePath.contains(f))
                return false;
        }
        return true;
    }

    public boolean fileExists(File f) {
        if (ExecutionService.getInstance().canQueryFileAttributes()) {
            return ExecutionService.getInstance().fileExists(f);
        }
        return runFileTestCommand(commandSet.getFileExistsTestCommand(f));
    }

    public boolean directoryExists(File f) {
        if (ExecutionService.getInstance().canQueryFileAttributes()) {
            return ExecutionService.getInstance().directoryExists(f);
        }
        return runFileTestCommand(commandSet.getDirectoryExistsTestCommand(f));
    }

    /**
     * Tests if a file is readable
     * @param f
     * @return
     */
    public boolean isReadable(File f) {
        if (ExecutionService.getInstance().canQueryFileAttributes()) {
            return ExecutionService.getInstance().isFileReadable(f);
        }
        return runFileTestCommand(commandSet.getReadabilityTestCommand(f));
    }

    public boolean isWritable(BaseFile f) {
        return isWritable(f.path);
    }

    public boolean isWritable(File f) {
        if (ExecutionService.getInstance().canQueryFileAttributes()) {
            return ExecutionService.getInstance().isFileWriteable(f);
        }
        return runFileTestCommand(commandSet.getWriteabilityTestCommand(f));
    }

    public boolean isExecutable(File f) {
        ExecutionService eService = ExecutionService.getInstance()
        if (eService.canQueryFileAttributes()) {
            return eService.isFileExecutable(f);
        } else {
            if (eService.isLocalService())
                return f.canExecute();
            else
                return eService.execute(commandSet.getExecutabilityTestCommand(f)).firstLine == commandSet.getReadibilityTestPositiveResult();
        }
    }

    /**
     * Lists all directory in a directory.
     * @param f
     * @return
     */
    public List<File> listDirectoriesInDirectory(File f, List<String> filters = null) {
        List<File> allFiles = []
        List<File> folders = []
        List<String> res;

        if (!ExecutionService.getInstance().canListFiles()) {
            String cmd = commandSet.getListDirectoriesInDirectoryCommand(f, filters);
            ExecutionResult er = ExecutionService.getInstance().execute(cmd);
            res = er.resultLines;
            res.each({ String folder -> folders.add(new File("${folder}")) })
        } else {
            folders = ExecutionService.getInstance().listFiles(f, filters);
        }

        for (File folderIn in folders) {
            File folderFound = null;
            for (File folderExisting in allFiles) {
                if (folderIn.absolutePath == folderExisting.absolutePath) {
                    folderFound = folderIn;
                }
            }

            if (folderFound == null)
                allFiles.add(folderIn);
        }
        return allFiles;
    }
    /**
     * Lists all files in a directory.
     * @param f
     * @return
     */
    public List<File> listFilesInDirectory(File f, List<String> filters = null) {
        if (filters == null) {
            filters = ["*"];
        }

        ExecutionService eService = ExecutionService.getInstance()
        if (!eService.canListFiles()) {
            if (eService.isLocalService())
                return Arrays.asList(f.listFiles(new FilenameFilter() {
                    @Override
                    boolean accept(File dir, String name) {
                        return new WildcardFileFilter(filters).accept(dir, name);
                    }
                }))
            else {
                List<File> files = [];
                String cmd = commandSet.getListFilesInDirectoryCommand(f, filters);
                ExecutionResult er = eService.execute(cmd);
                if (er.successful) {
                    for (String l : er.resultLines) {
                        files << new File(l);
                    }
                }
                return files;
            }
        } else {
            return eService.listFiles(f, filters);
        }
    }

    public boolean checkDirectories(List<File> files, ExecutionContext context, boolean createMissing) {
        boolean result = true;
        files.parallelStream().each {
            File f ->
                result &= checkDirectory(f, context, createMissing);
        }
        return result;
    }

    /**
     * Checks if a directory is existing and accessible.
     * @param f The directory which should be checked.
     * @param createMissing true? Then create the directory if it not there.
     * @return true if the directory exists or false if not.
     */
    public boolean checkDirectory(File f, ExecutionContext context, boolean createMissing) {
        final String path = f.absolutePath
        String id = String.format("checkDirectory_%08X", path.hashCode());
        if (!_directoryExistsAndIsAccessible.containsKey(path)) {
            String outputAccessRightsForDirectories = getContextSpecificAccessRightsForDirectory(context);
            String outputFileGroup = getContextSpecificGroupString(context);
            String cmd = commandSet.getCheckDirectoryCommand(f, createMissing, outputFileGroup, outputAccessRightsForDirectories);
            ExecutionResult er = ExecutionService.getInstance().execute(cmd);
            _directoryExistsAndIsAccessible[path] = (er.firstLine == commandSet.getReadibilityTestPositiveResult());
            fireCacheValueAddedEvent(id, path);
        }
        fireCacheValueReadEvent(id, -1);
        return _directoryExistsAndIsAccessible[path];
    }

    public boolean checkBaseFiles(BaseFile... filesToCheck) {
        for (BaseFile bf : filesToCheck) {
            if (!checkFile(bf.getPath()))
                return false;
        }
        return true;
    }

    public boolean checkFile(File f, boolean create, ExecutionContext context) {
        if (checkFile(f))
            return true;
        if (!create)
            return false;
        createFileWithDefaultAccessRights(true, f, context, true);
        return true;
    }

    /**
     * Checks if a file exists and if it is a file or link to a file...
     * @param f
     * @return
     */
    public boolean checkFile(File f) {
        if (ExecutionService.getInstance().canReadFiles()) {
            return ExecutionService.getInstance().isFileReadable(f);
        } else {
            if (ExecutionService.getInstance().isLocalService()) {
                return f.canRead();
            } else
                throw new RuntimeException("Not implemented yet!");
        }
    }

//    public void checkDirectories(List<String> directories) {
//        String cmd = commandSet.getCheckAndCreateDirectoryCommand()
//    }

    /**
     * The method returns the directory of the current user (or i.e. the ssh user's directory on the target system).
     * This method is called so often, that it is cached by default!
     * @return
     */
    public File getUserDirectory() {
        if (_userHome == null) {
            String cmd = commandSet.getUserDirectoryCommand();
            ExecutionResult er = ExecutionService.getInstance().execute(cmd);
            _userHome = new File(er.resultLines[0]);
            fireCacheValueAddedEvent("userHome", _userHome.getAbsolutePath());
        }
        fireCacheValueReadEvent("userHome", -1);
        return _userHome;
    }

    private String getSingleCommandValueOnValueIsNull(String _value, String command, String cacheEventID) {
        if (_value == null) {
            ExecutionResult er = ExecutionService.getInstance().execute(command);
            _value = er.resultLines[0];
            fireCacheValueAddedEvent(cacheEventID, _value);
        }
        fireCacheValueReadEvent(cacheEventID, -1);
        return _value;
    }

    /**
     * The method returns the current user's name (or i.e. the ssh user's name on the target system).
     * This method is called so often, that it is cached by default!
     * @return
     */
    public String callWhoAmI() {
        if (_userName == null) {
            if (ExecutionService.getInstance().doesKnowTheUsername())
                _userName = ExecutionService.getInstance().getUsername();
            else
                _userName = getSingleCommandValueOnValueIsNull(_userName, commandSet.getWhoAmICommand(), "userName");
        }
        return _userName;
    }

    public String getMyGroup() {
        _groupID = getSingleCommandValueOnValueIsNull(_groupID, commandSet.getMyGroupCommand(), "groupID");
        return _groupID;
    }

    int getGroupID() {
        return getGroupID(getMyGroup());
    }

    private Map<String, Integer> _groupIDsByGroup = [:];

    int getGroupID(String groupID) {
        synchronized (_groupIDsByGroup) {
            if (!_groupIDsByGroup.containsKey(groupID)) {
                ExecutionResult er = ExecutionService.getInstance().execute(commandSet.getGroupIDCommand(groupID));
                _groupIDsByGroup[groupID] = er.resultLines[0].toInteger();
            }

            return _groupIDsByGroup[groupID];
        }
    }

    private final Map<String, String> uidToUserCache = new HashMap<>();

    private String _getOwnerOfPath(File file) {
        String cmd = commandSet.getOwnerOfPathCommand(file);
        ExecutionResult er = ExecutionService.getInstance().execute(cmd);
        return er.resultLines[0];
    }

    /**
     * Returns the creator / current owner of a path.
     */
    public String getOwnerOfPath(File file) {
        if (ExecutionService.getInstance().canQueryFileAttributes()) {
            FileAttributes attributes = ExecutionService.getInstance().queryFileAttributes(file);
            if (attributes == null || attributes.userID == null)
                return _getOwnerOfPath(file);
            if (attributes.userID.isInteger()) {
                if (!uidToUserCache.containsKey(attributes.userID)) {
                    uidToUserCache[attributes.userID] = _getOwnerOfPath(file);
                }
                return uidToUserCache[attributes.userID]
            }
            return attributes.userID;
        } else
            return _getOwnerOfPath(file);
    }


    public void writeTextFile(File file, String text, ExecutionContext context) {
        writeTextFile(file, text);
        setDefaultAccessRights(file, context);
    }

    public void writeTextFile(File file, String text) {
        ExecutionService eService = ExecutionService.getInstance()
        if (eService.canWriteFiles()) { //Let the execution service do this.
            eService.writeTextFile(file, text);
            return;
        } else if (eService.isLocalService()) {

            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            file.write(text);
        } else
            throw new RuntimeException("Not implemented yet!");
    }

    public void writeBinaryFile(File file, Serializable serializable, ExecutionContext context) {
        writeBinaryFile(file, serializable);
        setDefaultAccessRights(file, context);
    }

    public void writeBinaryFile(File file, Serializable serializable) {
        if (ExecutionService.getInstance().canWriteFiles()) { //Let the execution service do this.
            ExecutionService.getInstance().writeBinaryFile(file, serializable);
        }
        throw new RuntimeException("Not implemented yet!");
    }

    public void copyFile(File _in, File _out, ExecutionContext context) {
        copyFile(_in, _out);
        setDefaultAccessRights(_out, context);
    }

    public void copyFile(File _in, File _out) {
        ExecutionService eService = ExecutionService.getInstance();
        if (eService.canCopyFiles()) { //Let the execution service do this.
            eService.copyFile(_in, _out);
        } else {
            eService.execute(commandSet.getCopyFileCommand(_in, _out));
        }
    }

    public void moveFile(File _from, File _to, ExecutionContext context) {
        moveFile(_from, _to);
        setDefaultAccessRights(_to, context);
    }

    public void moveFile(File _from, File _to) {
        ExecutionService eService = ExecutionService.getInstance();
        if (eService.canCopyFiles()) { //Let the execution service do this.
            eService.moveFile(_from, _to);
        } else {
            eService.execute(commandSet.getMoveFileCommand(_from, _to));
        }
    }

    public void copyDirectory(File _in, File _out, ExecutionContext context) {
        copyDirectory(_in, _out);
        setDefaultAccessRightsRecursively(_out, context);
    }

    public void copyDirectory(File _in, File _out) {
        ExecutionService eService = ExecutionService.getInstance()
        if (eService.canCopyFiles()) { //Let the execution service do this.
            eService.copyDirectory(_in, _out);
        } else
            eService.execute(commandSet.getCopyDirectoryCommand(_in, _out));
    }

    public boolean setDefaultAccessRightsRecursively(File path, ExecutionContext context) {
        return setAccessRightsRecursively(path, getContextSpecificAccessRightsForDirectory(context), getContextSpecificAccessRights(context), getContextSpecificGroupString(context));
    }

    public String getContextSpecificAccessRightsForDirectory(ExecutionContext context) {
        if (!context) return null;
        if (!isAccessRightsModificationAllowed(context)) return null;
        context.getConfiguration().getConfigurationValues().get("outputAccessRightsForDirectories", getDefaultAccessRightsString()).toString()
    }

    public String getContextSpecificAccessRights(ExecutionContext context) {
        if (!context) return null;
        if (!isAccessRightsModificationAllowed(context)) return null;
        context.getConfiguration().getConfigurationValues().get("outputAccessRights", getDefaultAccessRightsString()).toString()
    }

    public String getContextSpecificGroupString(ExecutionContext context) {
        if (!context) return null;
        if (!isAccessRightsModificationAllowed(context)) return null;
        return context.getConfiguration().getConfigurationValues().get("outputFileGroup", getMyGroup()).toString()
    }
    /**
     * Sets the rights for all files in the path and ints subpaths.
     * @param path
     * @param accessString
     * @return
     */
    public boolean setAccessRightsRecursively(File path, String accessStringDirecties, String accessString, String group) {
        return ExecutionService.getInstance().execute(commandSet.getSetAccessRightsRecursivelyCommand(path, accessStringDirecties, accessString, group), false);
    }

    public boolean setDefaultAccessRights(File file, ExecutionContext context) {
        if (!isAccessRightsModificationAllowed(context))
            return true;
        return setAccessRights(file, getContextSpecificAccessRights(context), getContextSpecificGroupString(context));
    }

    public boolean setAccessRights(File file, String accessString, String groupID) {
        if (ExecutionService.getInstance().canModifyAccessRights()) {
            return ExecutionService.getInstance().modifyAccessRights(file, accessString, groupID);
        } else {
            commandSet.getSetAccessRightsCommand(file, accessString, groupID)
        }
    }

    public String getDefaultAccessRightsString() {
        return commandSet.getDefaultAccessRightsString();
    }

    public Object loadBinaryFile(File file) {
        if (ExecutionService.getInstance().canReadFiles()) { //Let the execution service do this.
            return ExecutionService.getInstance().loadBinaryFile(file);
        }
        //TODO: Implement for the file system info provider.
        throw new RuntimeException("Load binary file not supported by the file system info provider.");
    }

    /**
     *
     */
    public File getTemporaryFileForFile(File file) {
        if (ExecutionService.getInstance().canReadFiles()) {
            return ExecutionService.getInstance().getTemporaryFileForFile(file);
        }
    }

    public String[] loadTextFile(File file) {
        try {
            if (ExecutionService.getInstance().canReadFiles()) { //Let the execution service do this.
                return ExecutionService.getInstance().loadTextFile(file);
            }

            if (ExecutionService.getInstance().isLocalService()) {

                try {
                    return file.readLines().toArray(new String[0]);
                } catch (Exception ex) {
                    return new String[0];
                }
            } else {
                return ExecutionService.getInstance().execute(commandSet.getReadOutTextFileCommand(file), true).resultLines.toArray(new String[0]);
            }
        } catch (Exception ex) {
            logger.postAlwaysInfo("There was an error while trying to load file " + file);
        }
    }

    public String getLineOfFile(File file, int lineIndex) {
        try {
            return ExecutionService.getInstance().execute(commandSet.getReadLineOfFileCommand(file, lineIndex), true).resultLines[0];
        } catch (Exception ex) {
            logger.postAlwaysInfo("There was an error while trying to read a line of file " + file);
        }
    }

    void createFileWithDefaultAccessRights(boolean atomic, File filename, ExecutionContext context, boolean blocking) {
        try {
            if (ExecutionService.getInstance().canWriteFiles()) {

                String accessRights = getContextSpecificAccessRights(context);
                String groupID = getContextSpecificGroupString(context);
                ExecutionService.getInstance().createFileWithRights(atomic, filename, accessRights, groupID, blocking);
            } else {
                if (ExecutionService.getInstance().isLocalService()) {
                    boolean created = filename.createNewFile();
                    if (!created)
                        throw new IOException("The file ${filename.absolutePath} could not be created.")
                } else
                    throw new RuntimeException("Not implemented yet!");
            }
        } catch (Exception ex) {
            logger.postAlwaysInfo("There was an error while trying to create file " + filename);
        }
    }

    void appendLinesToFile(boolean atomic, File filename, List<String> lines, boolean blocking) {
        if (ExecutionService.getInstance().canWriteFiles()) {
            ExecutionService.getInstance().appendLinesToFile(atomic, filename, lines, blocking);
        } else {
            lines.each {
                String line -> appendLineToFile(atomic, filename, line, blocking);
            }
            throw new RuntimeException("Not implemented yet!");
        }
    }

    private Object _appendLineToFileLock = new Object();

    void appendLineToFile(boolean atomic, File filename, String line, boolean blocking) {
        try {
            ExecutionService eService = ExecutionService.getInstance()
            if (eService.canWriteFiles()) {
                eService.appendLineToFile(atomic, filename, line, blocking);
            } else {
                if (eService.isLocalService())
                    synchronized (_appendLineToFileLock) {
                        RoddyIOHelperMethods.appendLineToFile(filename, line)
                    }
                else
                    throw new RuntimeException("Not implemented yet!");
            }
        } catch (Exception ex) {
            logger.postAlwaysInfo("There was an error during the attempt to append to file " + filename);
        }
    }

    void validateAllFilesInContext(ExecutionContext context) {
        if (ExecutionService.getInstance().canListFiles()) {
            Map<File, BaseFile> baseFilesPerFile = new LinkedHashMap<>();
            context.getAllFilesInRun().each { BaseFile bf -> baseFilesPerFile[bf.path] = bf; }
            Map<File, Boolean> readability = ExecutionService.getInstance().getReadabilityOfAllFiles(context.getAllFilesInRun().collect { BaseFile bf -> return bf.getPath(); })

            baseFilesPerFile.each {
                File file, BaseFile bf ->
                    boolean exists = readability.get(file);
                    bf.isFileReadable(exists);
            }
        } else {
            //throw new RuntimeException("Not implemented yet!");
        }

        for (BaseFile bf : context.allFilesInRun) {
            bf.isFileValid();
        }

    }

    public void removeDirectory(File directory) {
        if (ExecutionService.getInstance().canDeleteFiles()) {
            ExecutionService.getInstance().removeDirectory(directory);
        } else {
            ExecutionService.getInstance().execute(commandSet.getRemoveDirectoryCommand(directory));
        }
    }

    public String getPathSeparator() {
        return commandSet.getPathSeparator();
    }

    @Override
    void releaseCache() {
        _userHome = null;
        _userName = null;
        _directoryExistsAndIsAccessible.clear();
    }

    public String getNewLineString() {
        return commandSet.getNewLineString();
    }

    ConfigurationConverter getConfigurationConverter() {
        return commandSet.getConfigurationConverter();
    }
}
