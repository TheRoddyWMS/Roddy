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
public class TextFile extends BaseFile {

    public TextFile(File path, ExecutionContext executionContext, JobResult jobResult, List parentFiles, FileStageSettings settings) {
        super(path, executionContext, jobResult, parentFiles, settings);
    }

    public TextFile(BaseFile parentFile) {
        super(parentFile);
    }

    public TextFile(TextFile parentFile) {
        super(parentFile);
    }

    @ScriptCallingMethod
    public TextFile test1() {
        return GenericMethod.callGenericTool("testScript", this);
    }

    @ScriptCallingMethod
    public TextFile test2() {
        return GenericMethod.callGenericTool("testScript", this);
    }

    @ScriptCallingMethod
    public TextFile test3() {
        return GenericMethod.callGenericTool("testScriptExitBad", this);
    }

    @ScriptCallingMethod
    public FileWithChildren testFWChildren() { return GenericMethod.callGenericTool("testFileWithChildren", this); }
}
