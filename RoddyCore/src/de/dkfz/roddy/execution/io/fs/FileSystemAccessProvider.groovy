/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.io.fs

import de.dkfz.roddy.Constants
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.SystemProperties
import de.dkfz.roddy.config.converters.ConfigurationConverter
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.execution.io.ExecutionResult
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.FileAttributes
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.tools.ComplexLine
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import groovy.io.FileType
import groovy.transform.CompileStatic
import org.apache.commons.io.filefilter.WildcardFileFilter

import java.util.concurrent.locks.ReentrantLock

/**
 * This class provides access to the file system and collects information for easy access.
 *
 * TODO: A lot of the methods below do not work with the standard java file system classes.
 * I.e. setting file access rights is only available with SSH. This needs to be completed!
 */
@CompileStatic
class FileSystemAccessProvider {
    private static LoggerWrapper logger = LoggerWrapper.getLogger(FileSystemAccessProvider.class.name)
    private static FileSystemAccessProvider fileSystemAccessProvider = null

    /**
     * Do you wonder why this is protected?
     * Well this value is to ensure backward compatibility for older Roddy workflows.
     * Formerly, this class was called FileSystemInfoProvider which is just not the right name for the class.
     * So we decided to rename it to FileSystemAccessProvider. This name covers everything!
     * However, the FileSystemAccessProvider class still resides for backward compatibility.
     * The lock is used in this class and in the FileSystemAccessProvider. It is not on a package level and also should not be!
     * Also it must not be public or private.
     */
    protected static ReentrantLock fileSystemAccessProviderLock = new ReentrantLock()

    /**
     * This is the command set assembler for the current target file system.
     * As currently only Linux target fs are supported it is assigned by default.
     * TODO Initialize this in a different way!
     */
    public final ShellCommandSet commandSet = new BashCommandSet()

    /**
     * The current users user name (logon on local or target system)
     */
    protected String _userName

    /**
     * The first group id in a list of groups on the target system.
     */
    protected String _groupID

    /**
     * The current users home directory (logon on local or target system)
     */
    protected File _userHome

    /**
     * The cached umask
     */
    protected Integer _default_umask

    protected Map<String, Integer> _groupIDsByGroup = [:]

    protected final Map<String, String> uidToUserCache = new HashMap<>()

    protected Object _appendLineToFileLock = new Object()

    FileSystemAccessProvider() {}

    static void initializeProvider(boolean fullSetup) {
        fileSystemAccessProviderLock.lock()
        try {
            if (!fullSetup) {
                fileSystemAccessProvider = new NoNoFileSystemAccessProvider()
            }
            try {
                Class fisClz = LibrariesFactory.getGroovyClassLoader().loadClass(
                        Roddy.applicationConfiguration.getOrSetApplicationProperty(
                                Roddy.getRunMode(), Constants.APP_PROPERTY_FILESYSTEM_ACCESS_MANAGER_CLASS,
                                FileSystemAccessProvider.class.name))
                fileSystemAccessProvider = (FileSystemAccessProvider) fisClz.constructors[0].newInstance()
            } catch (Exception ex) {
                logger.warning('Falling back to default file system info provider')
                fileSystemAccessProvider = new FileSystemAccessProvider()
            }
        } finally {
            fileSystemAccessProviderLock.unlock()
        }
    }

    static File writeTextToTempFile(String text) {
        File tempFile = File.createTempFile('roddy_sshservice', '.tmp')
        tempFile.deleteOnExit()
        BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile))
        bw.write(text)
        bw.flush()
        bw.close()
        return tempFile
    }

    static File serializeObjectToTempFile(Serializable serializable) {
        File tempFile = File.createTempFile('roddy_sshservice', '.tmp')
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempFile))
        oos.writeObject(serializable)
        oos.flush()
        oos.close()
        return tempFile
    }

    static Object deserializeObjectFromFile(File f) {
        if (f == null || !f.exists())
            return null
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))
        return ois.readObject()
    }

    static FileSystemAccessProvider getInstance() {
        FileSystemAccessProvider provider
        fileSystemAccessProviderLock.lock()
        try {
            provider = fileSystemAccessProvider
        } finally {
            fileSystemAccessProviderLock.unlock()
        }
        return provider
    }

    static void resetFileSystemAccessProvider(FileSystemAccessProvider provider) {
        fileSystemAccessProviderLock.lock()
        try {
            fileSystemAccessProvider = provider
        } finally {
            fileSystemAccessProviderLock.unlock()
        }
    }

    ShellCommandSet getCommandSet() {
        return commandSet
    }

    /**
     * Wrapper for isReadable(File)
     * @param bf
     * @return
     */
    boolean isReadable(BaseFile bf) {
        return isReadable(bf.path)
    }

    private boolean runFileTestCommand(String cmd) {
        ExecutionResult er = ExecutionService.instance.execute(cmd, true)
        return er.firstStdoutLine == commandSet.getReadabilityTestPositiveResult()
    }

    boolean isCachingAllowed(File file) {
        List<String> filters = ['.roddyExecCache', '.roddy', 'jobStateLogfile', 'JobCalls', 'zippedAnalysesMD5.txt']

        for (String f in filters) {
            if (file.absolutePath.contains(f))
                return false
        }
        return true
    }

    boolean fileExists(File f) {
        if (ExecutionService.instance.canQueryFileAttributes()) {
            return ExecutionService.instance.fileExists(f)
        }
        return runFileTestCommand(commandSet.getFileExistsTestCommand(f))
    }

    Long fileSize(File f) {
        def res = ExecutionService.instance.execute(
                commandSet.getFileSizeCommand(f), true)
        if (res.successful)
            return Long.parseLong(res.firstStdoutLine)
        else
            return -1
    }

    boolean directoryExists(File f) {
        if (ExecutionService.instance.canQueryFileAttributes()) {
            return ExecutionService.instance.directoryExists(f)
        }
        return runFileTestCommand(commandSet.getDirectoryExistsTestCommand(f))
    }

    /**
     * Tests if a file is readable
     * @param f
     * @return
     */
    boolean isReadable(File f) {
        if (ExecutionService.instance.canQueryFileAttributes()) {
            return ExecutionService.instance.isFileReadable(f)
        }
        return runFileTestCommand(commandSet.getReadabilityTestCommand(f))
    }

    boolean isWritable(BaseFile f) {
        return isWritable(f.path)
    }

    boolean isWritable(File f) {
        if (ExecutionService.instance.canQueryFileAttributes()) {
            return ExecutionService.instance.isFileWriteable(f)
        }
        return runFileTestCommand(commandSet.getWriteabilityTestCommand(f))
    }

    boolean isExecutable(File f) {
        ExecutionService eService = ExecutionService.instance
        if (eService.canQueryFileAttributes()) {
            return eService.isFileExecutable(f)
        } else {
            if (eService.isLocalService())
                return f.canExecute()
            else
                return eService.execute(
                        commandSet.getExecutabilityTestCommand(f),
                        true).
                        firstStdoutLine == commandSet.getReadabilityTestPositiveResult()
        }
    }

    /**
     * Lists all directory in a directory.
     * @param f
     * @return
     */
    List<File> listDirectoriesInDirectory(File f, List<String> filters = null) {
        List<File> allFiles = []
        List<File> folders = []
        List<String> res

        if (!ExecutionService.instance.canListFiles()) {
            String cmd = commandSet.getListDirectoriesInDirectoryCommand(f, filters)
            ExecutionResult er =
                    ExecutionService.instance.execute(cmd, true)
            res = er.stdout
            res.each({ String folder -> folders.add(new File(folder)) })
        } else {
            folders = ExecutionService.instance.listFiles(f, filters)
        }

        for (File folderIn in folders) {
            File folderFound = null;
            for (File folderExisting in allFiles) {
                if (folderIn.absolutePath == folderExisting.absolutePath) {
                    folderFound = folderIn
                }
            }

            if (folderFound == null)
                allFiles.add(folderIn)
        }
        return allFiles
    }

    /**
     * Lists all files in a directory.
     * @param f
     * @return
     */
    List<File> listFilesInDirectory(File f, List<String> filters = null) {
        if (filters == null) {
            filters = ["*"]
        }

        ExecutionService eService = ExecutionService.instance
        if (!eService.canListFiles()) {
            if (eService.isLocalService())
                return Arrays.asList(f.listFiles(new FilenameFilter() {
                    @Override
                    boolean accept(File dir, String name) {
                        return new WildcardFileFilter(filters).accept(dir, name)
                    }
                }))
            else {
                List<File> files = [];
                String cmd = commandSet.getListFilesInDirectoryCommand(f, filters)
                ExecutionResult er = eService.execute(cmd, true)
                if (er.successful) {
                    for (String l : er.stdout) {
                        files << new File(l)
                    }
                }
                return files
            }
        } else {
            return eService.listFiles(f, filters)
        }
    }

    List<File> listFilesUsingWildcards(File baseFolder, String wildcards) {
        ExecutionResult result = ExecutionService.instance.
                execute(commandSet.getFindFilesUsingWildcardsCommand(baseFolder, wildcards),
                        true)
        result.stdout.collect {
            new File(it)
        } as List<File>
    }

    /**
     * Scope for regular expressions. Used in listFilesUsingRegex
     */
    enum RegexSearchDepth {
        AbsolutePath,
        RelativeToSearchFolder,
        Filename
    }

    List<File> listFilesUsingRegex(File baseFolder, String regex, RegexSearchDepth scope) {
        ExecutionResult result = ExecutionService.instance.
                execute(commandSet.getListFullDirectoryContentRecursivelyCommand(
                        baseFolder, -1, FileType.FILES, false),
                        true)

        List<File> foundFiles = result.stdout.collect { new File(it) } as List<File>

        foundFiles.findAll {
            String comparable
            if (scope == RegexSearchDepth.AbsolutePath) {
                comparable = it.absolutePath
            } else if (scope == RegexSearchDepth.RelativeToSearchFolder) {
                comparable = it.absolutePath[baseFolder.absolutePath.length() + 1..-1]
            } else if (scope == RegexSearchDepth.Filename)
                comparable = it.name
            comparable ==~ regex
        }
    }

    boolean checkDirectories(List<File> files, ExecutionContext context, boolean createMissing) {
        boolean result = true
        for (File f in files) {
            result &= checkDirectory(f, context, createMissing)
        }
        return result
    }

    boolean checkDirectory(File f, boolean createMissing = false) {
        ExecutionService eService = ExecutionService.instance
        if (eService.canReadFiles()) {
            return eService.isFileReadable(f)
        } else {
            if (eService.isLocalService()) {
                boolean result = f.canRead() && f.isDirectory()
                if (!result && createMissing) {
                    f.mkdirs()
                    result = checkDirectory(f, false)
                }
                return result
            } else
                throw new RuntimeException('Not implemented yet!')
        }
    }

    /**
     * Checks if a directory is existing and accessible.
     * @param f The directory which should be checked.
     * @param createMissing true? Then create the directory if it not there.
     * @return true if the directory exists or false if not.
     */
    boolean checkDirectory(File f, ExecutionContext context, boolean createMissing) {
        String cmd
        if (createMissing) {
            String outputAccessRightsForDirectories = context.outputDirectoryAccess
            String outputFileGroup = context.outputGroupString
            cmd = commandSet.getCheckDirectoryCommand(f, true, outputFileGroup,
                    outputAccessRightsForDirectories)
        } else {
            cmd = commandSet.getCheckDirectoryCommand(f)
        }
        ExecutionResult er = ExecutionService.instance.execute(cmd, true)
        return (er.firstStdoutLine == commandSet.readabilityTestPositiveResult)
    }

    boolean checkBaseFiles(BaseFile... filesToCheck) {
        for (BaseFile bf : filesToCheck) {
            if (!checkFile(bf.path))
                return false
        }
        return true
    }

    boolean checkFile(File f, boolean create, ExecutionContext context) {
        if (checkFile(f))
            return true
        if (!create)
            return false
        createFileWithDefaultAccessRights(true, f, context, true)
        return true
    }

    /**
     * Checks if a file exists and if it is a file or link to a file...
     * @param f
     * @return
     */
    boolean checkFile(File f) {
        ExecutionService eService = ExecutionService.instance
        if (eService.canReadFiles()) {
            return eService.isFileReadable(f)
        } else {
            if (eService.isLocalService()) {
                return f.canRead() && f.isFile()
            } else
                throw new RuntimeException("Not implemented yet!")
        }
    }

    /**
     * The method returns the directory of the current user (or i.e. the ssh user's directory on the target system).
     * This method is called so often, that it is cached by default!
     * @return
     */
    File getUserDirectory() {
        ExecutionService eService = ExecutionService.instance
        if (_userHome == null) {
            if (eService.isLocalService()) {
                String jHomeVar = SystemProperties.userHome
                _userHome = new File(jHomeVar)
            } else {
                String cmd = commandSet.userDirectoryCommand
                ExecutionResult er = eService.execute(cmd, true)
                _userHome = new File(er.firstStdoutLine)
            }
        }
        return _userHome
    }

    private String getSingleCommandValueOnValueIsNull(String _value, String command, String cacheEventID) {
        if (_value == null) {
            ExecutionResult er = ExecutionService.instance.
                    execute(command, true)
            _value = er.firstStdoutLine
        }
        return _value
    }

    /**
     * The method returns the current user's name (or i.e. the ssh user's name on the target system).
     * This method is called so often, that it is cached by default!
     * @return
     */
    String callWhoAmI() {
        if (_userName == null) {
            if (ExecutionService.instance.doesKnowTheUsername())
                _userName = ExecutionService.instance.username
            else
                _userName = getSingleCommandValueOnValueIsNull(
                        _userName, commandSet.whoAmICommand, 'userName')
        }
        return _userName
    }

    List<String> getListOfGroups() {
        // Result could be a single line or several lines. So just combine and split again. This way, we are safe.
        new ComplexLine(ExecutionService.instance.
                execute(commandSet.listOfGroupsCommand,
                        true).stdout.join(' ')).splitBy(' ') as List<String>
    }

    String getMyGroup() {
        _groupID = getSingleCommandValueOnValueIsNull(_groupID, commandSet.myGroupCommand, 'groupID')
        return _groupID
    }

    int getGroupID() {
        return getGroupID(myGroup)
    }

    int getGroupID(String groupID) {
        synchronized (_groupIDsByGroup) {
            if (!_groupIDsByGroup.containsKey(groupID)) {
                ExecutionResult er = ExecutionService.instance.
                        execute(commandSet.getGroupIDCommand(groupID), true)
                _groupIDsByGroup[groupID] = er.firstStdoutLine.toInteger()
            }

            return _groupIDsByGroup[groupID]
        }
    }

    boolean isGroupAvailable(String groupID) {
        listOfGroups.find { it == groupID }
    }

    private String _getOwnerOfPath(File file) {
        String cmd = commandSet.getOwnerOfPathCommand(file)
        ExecutionResult er = ExecutionService.instance.execute(cmd, true)
        return er.firstStdoutLine
    }

    /**
     * Returns the creator / current owner of a path.
     */
    String getOwnerOfPath(File file) {
        if (ExecutionService.instance.canQueryFileAttributes()) {
            FileAttributes attributes = ExecutionService.instance.queryFileAttributes(file)
            if (attributes == null || attributes.userID == null)
                return _getOwnerOfPath(file)
            if (attributes.userID.isInteger()) {
                if (!uidToUserCache.containsKey(attributes.userID)) {
                    uidToUserCache[attributes.userID] = _getOwnerOfPath(file)
                }
                return uidToUserCache[attributes.userID]
            }
            return attributes.userID
        } else
            return _getOwnerOfPath(file)
    }


    boolean writeTextFile(File file, String text, ExecutionContext context) {
        return writeTextFile(file, text) &&
                setDefaultAccessRights(file, context)
    }

    boolean writeTextFile(File file, String text) {
        ExecutionService eService = ExecutionService.instance
        if (eService.canWriteFiles()) {
            return eService.writeTextFile(file, text)
        } else if (eService.isLocalService()) {
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs()
            file.write(text)
            return true
        } else
            throw new RuntimeException("Not implemented yet!");
    }

    boolean writeBinaryFile(File file, Serializable serializable, ExecutionContext context) {
        writeBinaryFile(file, serializable) && setDefaultAccessRights(file, context)
    }

    boolean writeBinaryFile(File file, Serializable serializable) {
        if (ExecutionService.instance.canWriteFiles()) {
            return ExecutionService.instance.writeBinaryFile(file, serializable)
        }
        throw new RuntimeException('Not implemented yet!')
    }

    boolean copyFile(File _in, File _out, ExecutionContext context) {
        copyFile(_in, _out) && setDefaultAccessRights(_out, context)
    }

    boolean copyFile(File _in, File _out) {
        ExecutionService eService = ExecutionService.instance
        if (eService.canCopyFiles()) {
            return eService.copyFile(_in, _out)
        } else {
            return eService.execute(commandSet.getCopyFileCommand(_in, _out), true)
        }
    }

    boolean moveFile(File _from, File _to, ExecutionContext context) {
        moveFile(_from, _to) && setDefaultAccessRights(_to, context)
    }

    boolean moveFile(File _from, File _to) {
        ExecutionService eService = ExecutionService.instance
        if (eService.canCopyFiles()) {
            return eService.moveFile(_from, _to);
        } else {
            return eService.execute(commandSet.getMoveFileCommand(_from, _to), true);
        }
    }

    boolean copyDirectory(File _in, File _out, ExecutionContext context) {
        copyDirectory(_in, _out) && setDefaultAccessRightsRecursively(_out, context)
    }

    boolean copyDirectory(File _in, File _out) {
        ExecutionService eService = ExecutionService.instance
        if (eService.canCopyFiles()) {
            return eService.copyDirectory(_in, _out)
        } else {
            def executionResult = eService.execute(commandSet.getCopyDirectoryCommand(_in, _out), true)
            return executionResult.successful
        }

    }

    /**
     * This check tries to find out whether access rights can be set on the target file system (or not).
     * This check can be quite hard, because not all folders and files for a context might be on the same
     * file system or root folder. Thus the check concentrates on files and folders in the project output
     * folder.
     *
     * The test is also performed in such a way that it tests both permissions and owner / group changes.
     * If one of both is not working, then false is returned.
     * @param context
     * @return
     */
    boolean checkIfAccessRightsCanBeSet(ExecutionContext context) {

        def outputDirectory = context.outputDirectory

        ExecutionService eService = ExecutionService.instance
        String command = commandSet.getCheckChangeOfPermissionsPossibilityCommand(outputDirectory, myGroup)
        def executionResult = eService.execute(command, true)
        return executionResult.successful &&
                executionResult.firstStdoutLine &&
                executionResult.firstStdoutLine == BashCommandSet.TRUE
    }

    boolean setDefaultAccessRightsRecursively(File path, ExecutionContext context) {
        if (context == null) {
            return setAccessRightsRecursively(path, commandSet.defaultAccessRightsString,
                    commandSet.defaultAccessRightsString, myGroup)
        } else {
            return setAccessRightsRecursively(path, context.outputDirectoryAccess,
                    context.outputFileAccessRights, context.outputGroupString)
        }
    }

    /**
     * Sets the rights for all files in the path and ints subpaths. Return true if the executed command finished
     * successfully, otherwise false. Only access rights that are provided as non-null strings are actually set.
     * If all three access right strings are null, the method returns true!
     * Permission strings must be valid for chmod. The group must exist. If an error occurs, false is returned.
     *
     * @param path                       path to change access rights (recursively)
     * @param accessStringDirectories    permission string for directories (string, such as "rwx", or octal)
     * @param accessStringFiles          permission string for files (string, such as "rwx", or octal)
     * @param group                      group name
     * @return
     */
    boolean setAccessRightsRecursively(File path, String accessStringDirectories, String accessStringFiles, String group) {
        commandSet.getSetAccessRightsRecursivelyCommand(path, accessStringDirectories, accessStringFiles, group)
                .map { ExecutionService.instance.execute(it, false).successful }
                .orElse(true)
    }

    boolean setDefaultAccessRights(File file, ExecutionContext context) {
        if (!context.isAccessRightsModificationAllowed())
            return true // an economic decision ...
        return setAccessRights(file, context.outputFileAccessRights, context.outputGroupString)
    }

    /**
     * Sets the rights for a file. Return true if the executed command finished successfully, otherwise false.
     * Only access rights that are provided as non-null strings are actually set. If all three access right strings
     * are null, the method returns true! Permission strings must be valid for chmod. The group must exist. If an error
     * occurs, false is returned.
     *
     * @param path                       fil to change access rights
     * @param accessString               permission string for file (string, such as "rwx", or octal)
     * @param group                      group name
     * @return
     */
    boolean setAccessRights(File file, String accessString, String group) {
        ExecutionService eService = ExecutionService.instance
        if (eService.canModifyAccessRights()) {
            return eService.modifyAccessRights(file, accessString, group)
        } else {
            return commandSet.getSetAccessRightsCommand(file, accessString, group)
                    .map { eService.execute(it, true).successful }
                    .orElse(true)
        }
    }

    Object loadBinaryFile(File file) {
        if (ExecutionService.instance.canReadFiles()) {
            return ExecutionService.instance.loadBinaryFile(file)
        }
        //TODO: Implement for the file system info provider.
        throw new RuntimeException('Load binary file not supported by the file system info provider.')
    }

    /**
     *
     */
    File getTemporaryFileForFile(File file) {
        if (ExecutionService.instance.canReadFiles()) {
            return ExecutionService.instance.getTemporaryFileForFile(file)
        }
    }

    String[] loadTextFile(File file) {
        ExecutionService eService = ExecutionService.instance
        try {
            if (eService.canReadFiles()) {
                return eService.loadTextFile(file)
            }

            if (eService.isLocalService()) {
                return file.readLines().toArray(new String[0])
            } else {
                return eService.execute(commandSet.
                        getReadOutTextFileCommand(file), true).
                        stdout.toArray(new String[0])
            }
        } catch (Exception ex) {
            logger.postAlwaysInfo("Error loading file '${file}'")
            return null
        }
    }

    boolean createFileWithDefaultAccessRights(boolean atomic, File filename, ExecutionContext context, boolean blocking) {
        ExecutionService eService = ExecutionService.instance
        try {
            if (eService.canWriteFiles()) {
                String accessRights = context.outputFileAccessRights
                String groupID = context.outputGroupString
                return eService.createFileWithRights(atomic, filename, accessRights, groupID, blocking)
            } else {
                if (eService.isLocalService()) {
                    boolean created = filename.createNewFile()
                    if (!created)
                        throw new IOException("File '${filename.absolutePath}' could not be created.")
                    return created
                } else
                    throw new RuntimeException('Not implemented yet!')
            }
        } catch (Exception ex) {
            logger.postAlwaysInfo("Error creating file '${filename}'")
            return false
        }
    }

    boolean appendLinesToFile(boolean atomic, File filename, List<String> lines, boolean blocking) {
        ExecutionService eService = ExecutionService.instance
        if (eService.canWriteFiles()) {
            return eService.appendLinesToFile(atomic, filename, lines, blocking)
        } else {
            lines.each {
                String line -> appendLineToFile(atomic, filename, line, blocking)
            }
            throw new RuntimeException('Not implemented yet!')
        }
    }


    boolean appendLineToFile(boolean atomic, File filename, String line, boolean blocking) {
        try {
            ExecutionService eService = ExecutionService.instance
            if (atomic) { // Work very safe and use a lockfile
                // TODO This also needs the lockfile command from the configuration.
                // TODO As we always used lockfile, we'll do it here as well for now
                eService.execute(commandSet.getLockedAppendLineToFileCommand(filename, line))
            } else { // Use possibly faster methods
                if (eService.canWriteFiles()) {
                    return eService.appendLineToFile(atomic, filename, line, blocking)
                } else {
                    if (eService.isLocalService())
                        synchronized (_appendLineToFileLock) {
                            return RoddyIOHelperMethods.appendLineToFile(filename, line)
                        }
                    else
                        throw new RuntimeException('Not implemented yet!')
                }
            }
        } catch (Exception ex) {
            logger.postAlwaysInfo("Error appending to file '${filename}'")
        }
    }

    void validateAllFilesInContext(ExecutionContext context) {
        ExecutionService eService = ExecutionService.instance
        if (eService.canListFiles()) {
            LinkedHashMap<File, BaseFile> baseFilesPerFile = [:]
            context.allFilesInRun.each { BaseFile bf -> baseFilesPerFile[bf.path] = bf }
            Map<File, Boolean> readability = eService.getReadabilityOfAllFiles(
                    context.allFilesInRun.collect { BaseFile bf -> return bf.path })

            baseFilesPerFile.each {
                File file, BaseFile bf ->
                    boolean exists = readability.get(file)
                    bf.isFileReadable(exists)
            }
        } else {
            //throw new RuntimeException('Not implemented yet!');
        }

        for (BaseFile bf : context.allFilesInRun) {
            bf.isFileValid()
        }

    }

    boolean removeDirectory(File directory) {
        ExecutionService eService = ExecutionService.instance
        if (eService.canDeleteFiles()) {
            return eService.removeDirectory(directory)
        } else {
            return eService.execute(commandSet.getRemoveDirectoryCommand(directory), true)
        }
    }

    String getPathSeparator() {
        commandSet.pathSeparator
    }

    String getNewLineString() {
        commandSet.newLineString
    }

    ConfigurationConverter getConfigurationConverter() {
        commandSet.configurationConverter
    }

    int getDefaultUserMask() {
        if (!_default_umask) {
            String command = commandSet.getUsermaskCommand
            ExecutionResult executionResult =
                    ExecutionService.instance.execute(command, true)
            _default_umask = Integer.decode(executionResult.firstStdoutLine)
        }
        return _default_umask
    }
}
