/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.io.fs

import de.dkfz.roddy.config.converters.BashConverter
import de.dkfz.roddy.config.converters.ConfigurationConverter
import groovy.io.FileType
import org.apache.commons.io.filefilter.WildcardFileFilter

/**
 * Provides a command generator for linux file systems / bash
 */
@groovy.transform.CompileStatic
class BashCommandSet extends ShellCommandSet {

    public static final String TRUE = "TRUE"

    public static final String FALSE = "FALSE"

    public static final String SEPARATOR = "/"

    public static final String NEWLINE = "\n"

    public static final String TRUE_OR_FALSE = "&& echo ${TRUE} || echo ${FALSE}"

    @Override
    String getFileExistsTestCommand(File f) {
        String path = f.getAbsolutePath()

        return "[[ -f ${path} ]] " + TRUE_OR_FALSE
    }

    @Override
    String getDirectoryExistsTestCommand(File f) {
        String path = f.getAbsolutePath()
        return "[[ -d ${path} ]]" + TRUE_OR_FALSE
    }

    @Override
    String getReadabilityTestCommand(File f) {
        String path = f.getAbsolutePath()
        return "[[ -e ${path} && -r ${path} ]] " + TRUE_OR_FALSE
    }

    @Override
    String getWriteabilityTestCommand(File f) {
        String path = f.getAbsolutePath()
        return "[[ -e ${path} && -w ${path} ]] " + TRUE_OR_FALSE
    }

    @Override
    String getExecutabilityTestCommand(File f) {
        String path = f.getAbsolutePath()
        return "[[ -e ${path} && -x ${path} ]] " + TRUE_OR_FALSE
    }

    @Override
    String getReadabilityTestPositiveResult() {
        return TRUE
    }

    @Override
    String getUserDirectoryCommand() {
        return 'echo ~'
    }

    @Override
    String getWhoAmICommand() {
        return "whoami"
    }

    @Override
    String getListOfGroupsCommand() { return "groups" }

    @Override
    String getMyGroupCommand() { return "groups | cut -d \" \" -f 1" }

    @Override
    String getOwnerOfPathCommand(File f) {
        return "stat -c %U ${f.absolutePath}"
    }

    @Override
    String getCheckForInteractiveConsoleCommand() {
        String separator = "\n"
        StringBuilder builder = new StringBuilder()
        builder << 'if [[ -z "${PS1-}" ]]; then' << separator << '\t echo "non interactive process!" >> /dev/stderr' << separator << 'else' <<
                separator << '\t echo "interactive process" >> /dev/stderr'
        return builder.toString()
    }

    @Override
    String getSetPathCommand() {
        return 'if [[ "${SET_PATH-}" != "" ]]; then export PATH=${SET_PATH}; fi'
    }

    @Override
    String getGroupIDCommand(String groupID) {
        return "getent group ${groupID} | cut -d \":\" -f 3"
    }

    @Override
    String getGetUsermaskCommand() {
        return "umask"
    }

    @Override
    String getCheckDirectoryCommand(File f) {
        return getCheckDirectoryCommand(f, false, null, null)
    }

    @Override
    String getCheckDirectoryCommand(File file, boolean createMissing, String onCreateAccessRights, String onCreateFileGroup) {
        String path = file.absolutePath
        if (!createMissing)
            return "[[ -e ${path} && -d ${path} && -r ${path} ]]" + TRUE_OR_FALSE
        else
            return getCheckAndCreateDirectoryCommand(file, onCreateFileGroup, onCreateAccessRights) + TRUE_OR_FALSE;
    }

    @Override
    String getCheckAndCreateDirectoryCommand(File f, String onCreateAccessRights, String onCreateFileGroup) {
        String path = f.absolutePath
        String checkExistence = "[[ -e ${path} ]]"
        if (onCreateAccessRights && onCreateFileGroup)
            return "sg ${onCreateFileGroup} -c \"${checkExistence} || umask ${onCreateAccessRights} && mkdir -p ${path}\""
        else
            return "${checkExistence} || install -d \"${path}\" || echo ''"
    }

    @Override
    String getCheckChangeOfPermissionsPossibilityCommand(File f, String group) {
        File testFile = new File(f, ".roddyPermissionsTestFile")
        return "(touch ${testFile}; chmod u+rw ${testFile} &> /dev/null && chgrp ${group} ${testFile} &> /dev/null) $TRUE_OR_FALSE; rm ${testFile} 2>/dev/null; echo ''"
    }

    @Override
    Optional<String> getSetAccessRightsCommand(File f, String rightsForFiles, String fileGroup) {
        def path = "${f.absolutePath}"
        String result = ""
        if (null != rightsForFiles)
            result += "chmod ${rightsForFiles} ${path}; "
        if (null != fileGroup)
            result += "chgrp ${fileGroup} ${path}"
        return Optional.ofNullable(result == "" ? null : result)
    }

    @Override
    Optional<String> getSetAccessRightsRecursivelyCommand(File f, String rightsForDirectories, String rightsForFiles, String fileGroup) {
        def path = "${f.absolutePath}"
        String result = ""
        if (null != fileGroup)
            result += "find '${path}' -type d -or type f | xargs chgrp ${fileGroup}; "
        if (null != rightsForDirectories)
            result += "find '${path}' -type d | xargs chmod ${rightsForDirectories}; "
        if (null != rightsForFiles)
            result += "find '${path}' -type f | xargs chmod ${rightsForFiles}; "
        return Optional.ofNullable(result == "" ? null : result)
    }

    @Override
    String getCheckCreateAndReadoutExecCacheFileCommand(File f) {
        return "[[ ! -e ${f.absolutePath} ]] && find ${f.parent} -mindepth 1 -maxdepth 2 -name exec_* > ${f.absolutePath}; cat ${f.absolutePath}"
    }

    @Override
    String getReadOutTextFileCommand(File f) {
        return "cat ${f.absolutePath}"
    }

    @Override
    String getReadLineOfFileCommand(File file, int lineIndex) {
        return "tail -n +${lineIndex + 1} ${file.getAbsolutePath()} | head -n 1"
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
            return getListDirectoriesInDirectoryCommand(f)
        List<String> allLSLines = []
        for (String filter in filters) {
            allLSLines << "ls -lL -d ${f.absolutePath}/${filter} 2> /dev/null | grep '^d' | awk '{ print \$9 }' | uniq".toString()
        }
        return allLSLines.join(" && ")
    }

    @Override
    String getListFilesInDirectoryCommand(File path) {
        return "find -L ${path.absolutePath} -type f -maxdepth 1"
    }

    @Override
    String getListFilesInDirectoryCommand(File file, List<String> filters) {
        String joined = filters.collect({ String f -> return "${file.absolutePath}${SEPARATOR}${f}" }).join(" ")
        return "ls -lL -d ${joined} 2> /dev/null | grep -v \"^d\" | awk '{ print \$9 }' | uniq"
    }

    @Override
    String getListFullDirectoryContentRecursivelyCommand(File f, int depth, FileType selectedType, boolean detailed) {
        StringBuilder sb = new StringBuilder("find -L ")
        sb << "\"" << f.absolutePath << "\""

        if (depth > 0) sb << " -maxdepth ${depth}"
        if (selectedType == FileType.DIRECTORIES) sb << " -type d"
        if (selectedType == FileType.FILES) sb << " -type f"
        if (detailed) sb << " -ls"

        return sb.toString()
    }

    @Override
    String getListFullDirectoriesContentRecursivelyCommand(List<File> directories, List<Integer> depth, FileType selectedType, boolean detailed) {
        List<String> commands = []
        directories = directories.sort().unique()
        if (depth.size() != directories.size()) {
            depth = directories.collect { -1 } as List<Integer>
        }
        for (int i = 0; i < directories.size(); i++) {
            File f = directories[i]
            int d = depth[i]

            commands << getListFullDirectoryContentRecursivelyCommand(f, d, selectedType, detailed)
        }
        return commands.join(" && ")
    }

    @Override
    String getFindFilesUsingWildcardsCommand(File baseFolder, String wildcards) {
        return "for f in \$(ls \"${baseFolder}/\"${wildcards} | sort); do echo \"\${f}\"; done"
    }

    @Override
    FileSystemInfoObject parseDetailedDirectoryEntry(String line) {
        if (line.trim().length() == 0) return null
        if (line.startsWith("total")) return null
        line = line.replaceAll("\\s+", " ")       //Replace multi white space with single whitespace
        String[] lines = line.split(" ")

        final int PATH = 10
        final int RIGHTS = 2
        final int USER = 4
        final int GROUP = 5
        final int SIZE = 6
        File path = new File(lines[PATH])
        return new FileSystemInfoObject(path, lines[USER], lines[GROUP], Long.parseLong(lines[SIZE]), lines[RIGHTS], lines[RIGHTS][0] == "d")
    }

    @Override
    String getPathSeparator() {
        return SEPARATOR
    }

    @Override
    String getNewLineString() { return NEWLINE }

    @Override
    String getCopyFileCommand(File _in, File _out) {
        return "cp -p ${_in.getAbsolutePath()} ${_out.getAbsolutePath()}"
    }

    @Override
    String getCopyDirectoryCommand(File _in, File _out) {
        return "cp -pr ${_in.getAbsolutePath()} ${_out.getAbsolutePath()}"
    }

    @Override
    String getMoveFileCommand(File _in, File _out) { return "mv ${_in.getAbsolutePath()} ${_out.getAbsolutePath()}" }

    @Override
    String getLockedAppendLineToFileCommand(File file, String line) {
        return "lockfile ${file}~; echo \"${line}\" >> ${file}; rm -rf ${file}~"
    }

    @Override
    String getDefaultUMask() {
        return "007"
    }

    @Override
    String getDefaultAccessRightsString() {
        return "u+rwx,g+rwx,o-rwx"
    }

    @Override
    String getRemoveDirectoryCommand(File directory) {
        return "rm -rf ${directory.getAbsolutePath()}"
    }

    @Override
    String getRemoveFileCommand(File file) {
        return "rm -f ${file.getAbsolutePath()}"
    }

    @Override
    ConfigurationConverter getConfigurationConverter() {
        return new BashConverter()
    }

    @Override
    String getExecuteScriptCommand(File file) {
        return "/bin/bash ${file.absolutePath}"
    }

    @Override
    String singleQuote(String text) {
        return "'${text}'"
    }

    @Override
    String doubleQuote(String text) {
        return "\"${text}\""
    }

    @Override
    List<String> getShellExecuteCommand(String... commands) {
        return ["bash", "-c"] + (commands as List<String>)
    }

    @Override
    boolean validate() {
        def file = new File("/bin/bash")
        return file.exists() && file.canExecute()
    }

    @Override
    String getFileSizeCommand(File file) {
        return "stat --printf='%s' '${file}'"
    }
}
