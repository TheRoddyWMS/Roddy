package de.dkfz.roddy.execution.io.fs;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.knowledge.files.BaseFile;

import java.io.File;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Dummy file system info provider which is used in lean initialization mode.
 * It basically returns empty lists and positive results anywhere.
 */
public class NoNoFileSystemAccessManager extends FileSystemAccessManager {


    public NoNoFileSystemAccessManager() {
        super();
    }

    @Override
    public List<File> listDirectoriesInDirectory(File f, List<String> filters) {
        return new LinkedList<>();
    }

    @Override
    public List<File> listDirectoriesInDirectory(File f) {
        return new LinkedList<>();
    }

    @Override
    public List<File> listFilesInDirectory(File f, List<String> filters) {
        return new LinkedList<>();
    }

    @Override
    public List<File> listFilesInDirectory(File f) {
        return new LinkedList<>();
    }

    @Override
    public boolean initialize() {
        return true;
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public boolean isReadable(BaseFile bf) {
        return super.isReadable(bf);
    }

    @Override
    public boolean isReadable(File f) {
        return true;
    }

    @Override
    public boolean isExecutable(File f) {
        return true;
    }

    @Override
    public boolean checkDirectory(File f, ExecutionContext context, boolean createMissing) {
        return true;
    }

    @Override
    public File getUserDirectory() {
        return new File("/virtualhomedrive/nouser");
    }

    @Override
    public String callWhoAmI() {
        return "nouser";
    }

    @Override
    public String getOwnerOfPath(File file) {
        return callWhoAmI();
    }

    @Override
    public void writeTextFile(File file, String text) {

    }

    @Override
    public void writeBinaryFile(File file, Serializable serializable) {

    }

    @Override
    public void copyFile(File _in, File _out) {

    }

    @Override
    public void copyDirectory(File _in, File _out) {

    }

    @Override
    public boolean setAccessRights(File file, String accessString, String groupID) {
        return true;
    }

    @Override
    public Object loadBinaryFile(File file) {
        return null;
    }

    @Override
    public File getTemporaryFileForFile(File file) {
        return new File(getUserDirectory().getAbsolutePath() + "/temp");
    }

    @Override
    public String[] loadTextFile(File file) {
        return new String[0];
    }

    @Override
    public boolean setDefaultAccessRights(File file, ExecutionContext context) {
               return true;
    }

    @Override
    public void appendLinesToFile(boolean atomic, File filename, List<String> lines, boolean blocking) {
    }

    @Override
    public void appendLineToFile(boolean atomic, File filename, String line, boolean blocking) {
    }

    @Override
    public void validateAllFilesInContext(ExecutionContext context) {
    }

    @Override
    public String getPathSeparator() {
        return "/";
    }

    @Override
    public void releaseCache() {
    }

    @Override
    public String getNewLineString() {
        return "\n";
    }
}
