/*
 * Copyright (c) 2021 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

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
    protected ExecutionResult _execute(String string, boolean waitFor, boolean ignoreErrors, OutputStream outputStream) {
        return new ExecutionResult(true, 0, new LinkedList<>(), "");
    }

    @Override
    public boolean writeTextFile(File file, String text) {
        return true;
    }

    @Override
    public boolean writeBinaryFile(File file, Serializable serializable) {
        return true;
    }

    @Override
    public boolean copyFile(File _in, File _out) {
        return true;
    }

    @Override
    public boolean copyDirectory(File _in, File _out) {
        return true;
    }

    @Override
    public boolean modifyAccessRights(File file, String rightsStr, String groupID) {
        return true;
    }

    @Override
    public boolean createFileWithRights(boolean atomic, File file, String accessRights, String groupID, boolean blocking) {
        return true;
    }

    @Override
    public boolean appendLinesToFile(boolean atomic, File file, List<String> lines, boolean blocking) {
        return true;
    }

    @Override
    public boolean appendLineToFile(boolean atomic, File file, String line, boolean blocking) {
        return true;
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
    public File queryWorkingDirectory() {
        return null;
    }

}
