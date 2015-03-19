package de.dkfz.roddy.knowledge.files;

import java.util.List;

/**
 */
public class StreamingBufferFileGroup extends FileGroup<StreamingBufferConnectionFile> {


    private final List<StreamingBufferConnectionFile> inputBufferFiles;
    private final List<StreamingBufferConnectionFile> outputBufferFiles;

    public StreamingBufferFileGroup(List<StreamingBufferConnectionFile> inputBufferFiles, List<StreamingBufferConnectionFile> outputBufferFiles) {
        super(inputBufferFiles);
        this.inputBufferFiles = inputBufferFiles;
        this.outputBufferFiles = outputBufferFiles;
        addFiles(outputBufferFiles);
    }

    public List<StreamingBufferConnectionFile> getInputBufferFiles() {
        return inputBufferFiles;
    }

    public List<StreamingBufferConnectionFile> getOutputBufferFiles() {
        return outputBufferFiles;
    }
}
