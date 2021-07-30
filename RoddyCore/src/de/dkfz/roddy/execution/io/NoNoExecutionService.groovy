/*
 * Copyright (c) 2021 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.io

import groovy.transform.CompileStatic

import java.time.Duration

/**
 * A dummy execution service which returns empty things and positive results.
 */
@CompileStatic
class NoNoExecutionService extends ExecutionService {

    NoNoExecutionService() {
    }

    @Override
    protected ExecutionResult _execute(String command, boolean waitFor,
                                       Duration timeout = Duration.ZERO,
                                       OutputStream outputStream) {
        return new ExecutionResult([command], true, 0,
                new LinkedList<>(), new LinkedList<>(), "")
    }

    @Override
    boolean writeTextFile(File file, String text) {
        return true
    }

    @Override
    boolean writeBinaryFile(File file, Serializable serializable) {
        return true
    }

    @Override
    boolean copyFile(File _in, File _out) {
        return true
    }

    @Override
    boolean copyDirectory(File _in, File _out) {
        return true
    }

    @Override
    boolean modifyAccessRights(File file, String rightsStr, String groupID) {
        return true
    }

    @Override
    boolean createFileWithRights(boolean atomic, File file, String accessRights, String groupID, boolean blocking) {
        return true
    }

    @Override
    boolean appendLinesToFile(boolean atomic, File file, List<String> lines, boolean blocking) {
        return true
    }

    @Override
    boolean appendLineToFile(boolean atomic, File file, String line, boolean blocking) {
        return true
    }

    @Override
    Object loadBinaryFile(File file) {
        return null
    }

    @Override
    String[] loadTextFile(File file) {
        return new String[0]
    }

    @Override
    List<File> listFiles(File file, List<String> filters) {
        return new LinkedList<>()
    }

    @Override
    List<File> listFiles(List<File> file, List<String> filters) {
        return new LinkedList<>()
    }

    @Override
    boolean isFileReadable(File f) {
        return true
    }

    @Override
    boolean isFileExecutable(File f) {
        return true
    }

    @Override
    FileAttributes queryFileAttributes(File file) {
        return new FileAttributes("000", "000")
    }

    @Override
    boolean isAvailable() {
        return true
    }

    @Override
    File queryWorkingDirectory() {
        return null
    }

}
