package de.dkfz.roddy.execution.io.fs
/**
 * Provides a command generator for linux file systems / bash
 */
@groovy.transform.CompileStatic
public class LinuxFileSystemCommandSet extends FileSystemCommandSet {

    public static final String TRUE = "TRUE"

    public static final String SEPARATOR = "/";

    public static final String NEWLINE = "\n";

    @Override
    public String getFileExistsTestCommand(File f) {
        String path = f.getAbsolutePath();
        return "[[ -f ${path} ]] && echo ${TRUE}";
    }

    @Override String getDirectoryExistsTestCommand(File f) {
        String path = f.getAbsolutePath();
        return "[[ -d ${path} ]] && echo ${TRUE}";
    }

    @Override
    public String getReadabilityTestCommand(File f) {
        String path = f.getAbsolutePath();
        return "[[ -e ${path} && -r ${path} ]] && echo ${TRUE}";
    }

    @Override
    String getWriteabilityTestCommand(File f) {
        String path = f.getAbsolutePath();
        return "[[ -e ${path} && -w ${path} ]] && echo ${TRUE}";
    }

    @Override
    public String getExecutabilityTestCommand(File f) {
        String path = f.getAbsolutePath();
        return "[[ -e ${path} && -x ${path} ]] && echo ${TRUE}";
    }

    @Override
    String getReadibilityTestPositiveResult() {
        return TRUE;
    }

    @Override
    String getUserDirectoryCommand() {
        return 'echo ~';
    }

    @Override
    String getWhoAmICommand() {
        return "whoami";
    }

    @Override
    String getMyGroupCommand() { return "groups | cut -d \" \" -f 1"; }

    @Override
    String getOwnerOfPathCommand(File f) {
        return "stat -c %U ${f.absolutePath}";
    }

    @Override
    String getGroupIDCommand(String groupID) {
        return "getent group ${groupID} | cut -d \":\" -f 3";
    }

    @Override
    String getCheckDirectoryCommand(File f) {
        return getCheckDirectoryCommand(f, false, null, null);
    }

    @Override
    String getCheckDirectoryCommand(File file, boolean createMissing, String onCreateAccessRights, String onCreateFileGroup) {
        String path = file.absolutePath;
        if (!createMissing)
            return "[[ -e ${path} && -d ${path} && -r ${path} ]] && echo ${TRUE}";
        else
            return getCheckAndCreateDirectoryCommand(file, onCreateFileGroup, onCreateAccessRights);
    }

    @Override
    String getCheckAndCreateDirectoryCommand(File f, String onCreateAccessRights, String onCreateFileGroup) {
        if (onCreateAccessRights && onCreateFileGroup)
            return "install -g ${onCreateFileGroup} -m ${onCreateAccessRights} -d ${f.absolutePath}";
        else
            return "install -d ${f.absolutePath}";
    }

    @Override
    String getSetAccessRightsRecursivelyCommand(File f, String rightsForDirectories, String rightsForFiles, String fileGroup) {
        def path = "${f.getAbsolutePath()}"
        return "find ${path} -type d | xargs chmod ${rightsForDirectories}; find ${path} -type d | xargs chgrp ${fileGroup}; find ${path} -type f | xargs chmod ${rightsForFiles}; find ${path} -type f | xargs chgrp ${fileGroup};";
    }

    @Override
    String getCheckCreateAndReadoutExecCacheFileCommand(File f) {
        return "[[ ! -e ${f.absolutePath} ]] && find ${f.parent} -mindepth 1 -maxdepth 2 -name exec_* > ${f.absolutePath}; cat ${f.absolutePath}";
    }

    @Override
    String getReadOutTextFileCommand(File f) {
        return "cat ${f.absolutePath}";
    }
/**
 * Creates a list of all directories in a directory.
 * @param f
 * @return
 */
    @Override
    String getListDirectoriesInDirectoryCommand(File f) {
        return "ls -lL ${f.absolutePath} 2> /dev/null | grep '^d' | awk '{ print \$9 }' | uniq"
    }

    @Override
    String getListDirectoriesInDirectoryCommand(File f, List<String> filters) {
        if (filters == null || filters.size() == 0)
            return getListDirectoriesInDirectoryCommand(f);
        List<String> allLSLines = [];
        for (String filter in filters) {
            allLSLines << "ls -lL -d ${f.absolutePath}/${filter} 2> /dev/null | grep '^d' | awk '{ print \$9 }' | uniq".toString();
        }
        return allLSLines.join(" && ");
    }

    @Override
    String getListFilesInDirectoryCommand(File path) {
        return "find ${path.absolutePath} -type f -maxdepth 1";
    }

    @Override
    String getListFilesInDirectoryCommand(File file, List<String> filters) {
        String joined = filters.collect({ String f -> return "${file.absolutePath}${SEPARATOR}${f}" }).join(" ");
        return "ls -lL -d ${joined} 2> /dev/null | grep -v \"^d\" | awk '{ print \$9 }' | uniq";
    }

    @Override
    String getListFullDirectoryContentRecursivelyCommand(File f, int depth, boolean onlyDirectories) {
        String depthString = depth > 0 ? " -maxdepth ${depth}" : "";
        String dirString = onlyDirectories ? " -type d" : "";
        return "find ${f.absolutePath} ${depthString} ${dirString} -ls";
    }

    @Override
    String getListFullDirectoryContentRecursivelyCommand(List<File> directories, List<Integer> depth, boolean onlyDirectories) {
        List<String> commands = [];
        if (depth.size() != directories.size()) {
            depth = [];
            for (File f : directories) {
                depth << -1;
            }
        }
        for (int i = 0; i < directories.size(); i++) {
            File f = directories[i];
            int d = depth[i];

            commands << getListFullDirectoryContentRecursivelyCommand(f, d, onlyDirectories);
        }
        return commands.join(" && ")
    }

    @Override
    FileSystemInfoObject parseDetailedDirectoryEntry(String line) {
        if (line.trim().length() == 0) return null;
        if (line.startsWith("total")) return null;
        line = line.replaceAll("\\s+", " ");       //Replace multi white space with single whitespace
        String[] lines = line.split(" ");

        final int PATH = 10;
        final int RIGHTS = 2;
        final int USER = 4;
        final int GROUP = 5;
        final int SIZE = 6;
        File path = new File(lines[PATH]);
        return new FileSystemInfoObject(path, lines[USER], lines[GROUP], Long.parseLong(lines[SIZE]), lines[RIGHTS], lines[RIGHTS][0] == "d");
    }

    @Override
    String getPathSeparator() {
        return SEPARATOR;
    }

    @Override
    String getNewLineString() { return NEWLINE; }

    @Override
    String getCopyFileCommand(File _in, File _out) {
        return "cp ${_in.getAbsolutePath()} ${_out.getAbsolutePath()}";
    }

    @Override
    String getCopyDirectoryCommand(File _in, File _out) {
        return "cp -r ${_in.getAbsolutePath()} ${_out.getAbsolutePath()}";
    }

    @Override
    String getMoveFileCommand(File _in, File _out) { return "mv ${_in.getAbsolutePath()} ${_out.getAbsolutePath()}"; }

    @Override
    String getDefaultUMask() {
        return "007";
    }

    @Override
    String getDefaultAccessRightsString() {
        return "u+rwx,g+rwx,o-rwx";
    }

    @Override
    String getRemoveDirectoryCommand(File directory) {
        return "rm -rf ${directory.getAbsolutePath()}"
    }

    @Override
    String getRemoveFileCommand(File file) {
        return "rm -f ${file.getAbsolutePath()}"
    }
}
