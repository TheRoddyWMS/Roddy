/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.files;

import de.dkfz.roddy.config.ConfigurationError;
import de.dkfz.roddy.core.ExecutionContext;

import java.util.LinkedList;
import java.util.List;

/**
 * Filegroups group multiple files into one group. I.e. BamFiles can be grouped
 * to a BamFileGroup which can then be merged to a single BamFile
 *
 * @author michael
 */
public abstract class FileGroup<F extends BaseFile> extends FileObject {

    // In some cases a group has another group as a parent.
    protected FileGroup parentGroup;

    protected final List<F> filesInGroup = new LinkedList<F>();

    public FileGroup(List<F> files) {
        super(null);
        if (files != null) {
            addFiles(files);
        }
    }

    /**
     * Calls runDefaultOperations on the contained files.
     */
    @Override
    public void runDefaultOperations() throws ConfigurationError {
        for (F file : filesInGroup) {
            file.runDefaultOperations();
        }
    }

    @Override
    public ExecutionContext getExecutionContext() {
        if (filesInGroup.size() > 0) {
            return filesInGroup.get(0).getExecutionContext();
        } else {
            throw new RuntimeException("Cannot call getExecutionContext if FileGroup has no files in it.");
        }
    }

    public final void addFile(F file) {
        filesInGroup.add(file);
        file.addFileGroup(this);
    }

    public final void addFiles(List<F> files) {
        filesInGroup.addAll(files);
        for(BaseFile file : files)
            file.addFileGroup(this);
    }

    public final F getAt(int index) {
        return filesInGroup.get(index);
    }

    public final F get(int index) {
        return filesInGroup.get(index);
    }

    public List<F> getFilesInGroup() {
        return new LinkedList<F>(filesInGroup);
    }

    public FileGroup getParentGroup() {
        return parentGroup;
    }

    public void setParentGroup(FileGroup parentGroup) {

        this.parentGroup = parentGroup;
    }
}
