/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.files;

import de.dkfz.roddy.core.ExecutionContext;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * An object of this class represents a formerly created basefile.
 * As basefiles can have a variety of parameters and configurations, creating one out of read in information can be tricky.
 * Here only very basic information about a file ist stored.
 */
public class LoadedFile {
    private File path;

    private String createdByJob;

    private ExecutionContext context;

    private List<LoadedFile> parentFiles;

    private String baseFileImplClass;

    public LoadedFile(@NotNull File path,
                      String createdByJob,
                      @NotNull ExecutionContext context,
                      List<LoadedFile> parentFiles,
                      @NotNull String baseFileImplClass) {
        this.path = path;
        this.createdByJob = createdByJob;
        this.context = context;
        this.parentFiles = parentFiles;
        this.baseFileImplClass = baseFileImplClass;
    }

    public File getPath() {
        return path;
    }

    public String getCreatedByJob() {
        return createdByJob;
    }

    public ExecutionContext getContext() {
        return context;
    }

    public List<LoadedFile> getParentFiles() {
        return new LinkedList<>(parentFiles);
    }

    public String getBaseFileImplClass() {
        return baseFileImplClass;
    }
}
