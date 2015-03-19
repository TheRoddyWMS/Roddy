package de.dkfz.roddy.execution.io;

import java.io.File;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * A dummy execution service which returns empty things and positive results.
 */
public class NoNoExecutionService extends ExecutionService {

    public NoNoExecutionService() {
    }

    @Override
    protected List<String> _execute(String string, boolean waitFor, boolean ignoreErrors, OutputStream outputStream) {
        return new LinkedList<>();
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
    public boolean modifyAccessRights(File file, String rightsStr, String groupID) {
        return true;
    }

    @Override
    public void createFileWithRights(boolean atomic, File file, String accessRights, String groupID, boolean blocking) {

    }

    @Override
    public void appendLinesToFile(boolean atomic, File file, List<String> lines, boolean blocking) {

    }

    @Override
    public void appendLineToFile(boolean atomic, File file, String line, boolean blocking) {

    }

    @Override
    public Object loadBinaryFile(File file) {
        return null;
    }

    @Override
    public String[] loadTextFile(File file) {
        return new String[0];
    }

    @Override
    public List<File> listFiles(File file, List<String> filters) {
        return new LinkedList<>();
    }

    @Override
    public List<File> listFiles(List<File> file, List<String> filters) {
        return new LinkedList<>();
    }

    @Override
    public boolean isFileReadable(File f) {
        return true;
    }

    @Override
    public boolean isFileExecutable(File f) {
        return true;
    }

    @Override
    public FileAttributes queryFileAttributes(File file) {
        return new FileAttributes("000", "000");
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void releaseCache() {

    }
}
