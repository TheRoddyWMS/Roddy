/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.io.fs

/**
 * Stores information for files on a storage device. Is i.e. used for caching mechanisms
 */
class FileSystemInfoObject {
    final File path;
    final String filename;
    final String user;
    final String group;
    final long sizeInBytes;
    final String accessRightsMask;
    final boolean isPath;
    final boolean isFile;

    /**
     * Is set as soon as a subdirectory is added via addChild.
     * Is i.e. used for caching!
     */
    private boolean knowsSubDirectories;
    /**
     * Is set as soon as a file is added via addChild.
     * Is i.e. used for caching!
     */
    private boolean knowsFiles;

    private final List<FileSystemInfoObject> children = [];

    FileSystemInfoObject(File path, String user, String group, long sizeInBytes, String accessRightsMask, boolean isPath) {
        this.path = path
        this.filename = path.name
        this.user = user
        this.group = group
        this.sizeInBytes = sizeInBytes
        this.accessRightsMask = accessRightsMask
        this.isPath = isPath;
        this.isFile = !isPath;
    }

    public void addChild(FileSystemInfoObject child) {
        children.add(child);
        if (child.isPath)
            knowsSubDirectories = true;
        else
            knowsFiles = true;
    }

    List<FileSystemInfoObject> getFiles() {
        if (!knowsFiles) {
            throw new RuntimeException("This object [${path.absolutePath}] does not know about its files!");
        }
        return children.findAll({ it -> it.isFile })
    }

    List<FileSystemInfoObject> getSubdirectories() {
        if (!knowsSubDirectories) {
            throw new RuntimeException("This object [${path.absolutePath}] does not know about its subdirectories!");
        }
        return children.findAll({ it -> it.isPath })
    }

    boolean knowsSubDirectories() {
        return knowsSubDirectories
    }

    boolean knowsFiles() {
        return knowsFiles
    }

    public void setKnowsSubDirectories() {
        knowsSubDirectories = true;
    }

    public void setKnowsFiles() {
        knowsFiles = true;
    }
}
