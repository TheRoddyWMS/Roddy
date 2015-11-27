package de.dkfz.roddy.execution.io.fs

import de.dkfz.roddy.config.converters.ConfigurationConverter

/**
 */
@groovy.transform.CompileStatic
public abstract class ShellCommandSet {

    public abstract String getFileExistsTestCommand(File f)

    public abstract String getDirectoryExistsTestCommand(File f)

    public abstract String getReadabilityTestCommand(File f);

    public abstract String getWriteabilityTestCommand(File f);

    public abstract String getExecutabilityTestCommand(File f)

    public abstract String getReadabilityTestPositiveResult();

    public abstract String getUserDirectoryCommand();

    public abstract String getWhoAmICommand();

    public abstract String getMyGroupCommand();

    public abstract String getGetUsermaskCommand();

    public abstract String getGroupIDCommand(String groupID);

    public abstract String getOwnerOfPathCommand(File f);

    public abstract String getCheckDirectoryCommand(File f);

    public abstract String getCheckDirectoryCommand(File file, boolean createMissing, String onCreateAccessRights, String onCreateFileGroup);

    public abstract String getCheckAndCreateDirectoryCommand(File f, String onCreateAccessRights, String onCreateFileGroup)

    public abstract String getCheckChangeOfPermissionsPossibilityCommand(File f, String group)

    public abstract String getSetAccessRightsCommand(File f, String rightsForFiles, String fileGroup);

    public abstract String getSetAccessRightsRecursivelyCommand(File f, String rightsForDirectories, String rightsForFiles, String fileGroup);

    public abstract String getCheckCreateAndReadoutExecCacheFileCommand(File f);

    public abstract String getReadOutTextFileCommand(File f);

    public abstract String getReadLineOfFileCommand(File file, int lineIndex);

    public abstract String getListDirectoriesInDirectoryCommand(File file);

    public abstract String getListDirectoriesInDirectoryCommand(File file, List<String> filters);

    public abstract String getListFilesInDirectoryCommand(File file);

    public abstract String getListFilesInDirectoryCommand(File file, List<String> filters);

    public abstract String getListFullDirectoryContentRecursivelyCommand(File f, int depth, boolean onlyDirectories);

    public abstract String getListFullDirectoryContentRecursivelyCommand(List<File> directories, List<Integer> depth, boolean onlyDirectories);

    public abstract FileSystemInfoObject parseDetailedDirectoryEntry(String line);

    public abstract String getPathSeparator();

    public abstract String getNewLineString();

    public abstract String getCopyFileCommand(File _in, File _out);

    public abstract String getCopyDirectoryCommand(File _in, File _out);

    public abstract String getMoveFileCommand(File _from, File _to);

    public abstract String getDefaultUMask()

    public abstract String getDefaultAccessRightsString()

    public abstract String getRemoveDirectoryCommand(File file);

    public abstract String getRemoveFileCommand(File file);

    public abstract ConfigurationConverter getConfigurationConverter();

    public abstract String getExecuteScriptCommand(File file);
}
