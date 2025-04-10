/*
 * Copyright (c) 2023 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.execution.CommandI;
import de.dkfz.roddy.knowledge.files.LoadedFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Stores information about jobs which were read out from Roddys XML file.
 */
public class LoadedJob extends ReadOutJob {

    /** This is probably only for the debugger. */
    private List<LoadedFile> loadedFiles;

    public LoadedJob(@NotNull ExecutionContext context,
                     @NotNull String jobName,
                     String jobID,
                     @NotNull ToolIdCommand command,
                     Map<String, String> parameters,
                     List<LoadedFile> loadedFiles,
                     List<BEJobID> parentJobIDs) {
        super(context, jobName, command, jobID, parameters, parentJobIDs);
        this.loadedFiles = loadedFiles;
    }

}
