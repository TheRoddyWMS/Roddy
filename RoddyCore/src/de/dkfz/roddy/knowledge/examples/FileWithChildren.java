package de.dkfz.roddy.knowledge.examples;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.execution.jobs.JobResult;
import de.dkfz.roddy.execution.jobs.ScriptCallingMethod;
import de.dkfz.roddy.knowledge.files.BaseFile;
import de.dkfz.roddy.knowledge.files.FileStageSettings;
import de.dkfz.roddy.knowledge.methods.GenericMethod;

import java.io.File;
import java.util.List;

/**
 * A very simple text file containing nothing or some text.
 */
public class FileWithChildren extends BaseFile {

    private TextFile childFile0;

    private TextFile childFile1;

    public FileWithChildren(TextFile parentFile) {
        super(parentFile);
    }

    public TextFile getChildFile0() {
        return childFile0;
    }

    public void setChildFile0(TextFile childFile0) {
        this.childFile0 = childFile0;
    }

    public TextFile getChildFile1() {
        return childFile1;
    }

    public void setChildFile1(TextFile childFile1) {
        this.childFile1 = childFile1;
    }
}
