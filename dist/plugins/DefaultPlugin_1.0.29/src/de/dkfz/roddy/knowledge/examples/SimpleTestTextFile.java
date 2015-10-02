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
public class SimpleTestTextFile extends BaseFile {

    public SimpleTestTextFile(File path, ExecutionContext executionContext, JobResult jobResult, List parentFiles, FileStageSettings settings) {
        super(path, executionContext, jobResult, parentFiles, settings);
    }

    public SimpleTestTextFile(BaseFile parentFile) {
        super(parentFile);
    }

    public SimpleTestTextFile(SimpleTestTextFile parentFile) {
        super(parentFile);
    }

    @ScriptCallingMethod
    public SimpleTestTextFile test1() {
        return GenericMethod.callGenericTool("testScript", this);
    }

    @ScriptCallingMethod
    public SimpleTestTextFile test2() {
        return GenericMethod.callGenericTool("testScript", this);
    }

    @ScriptCallingMethod
    public SimpleTestTextFile test3() {
        return GenericMethod.callGenericTool("testScriptExitBad", this);
    }

    @ScriptCallingMethod
    public FileWithChildren testFWChildren() { return GenericMethod.callGenericTool("testFileWithChildren", this); }
}
