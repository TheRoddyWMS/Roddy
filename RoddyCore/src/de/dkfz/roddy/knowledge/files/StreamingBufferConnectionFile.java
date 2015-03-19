package de.dkfz.roddy.knowledge.files;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.execution.jobs.JobResult;

import java.io.File;
import java.util.List;

/**
 */
public class StreamingBufferConnectionFile extends BaseFile {
    public StreamingBufferConnectionFile(File path, ExecutionContext runningProcess, JobResult jobResult, List<BaseFile> parentFiles, FileStageSettings settings) {
        super(path, runningProcess, jobResult, parentFiles, settings);
    }
}
