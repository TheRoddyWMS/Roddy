package de.dkfz.roddy.knowledge.examples;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.core.Workflow;
import de.dkfz.roddy.knowledge.methods.GenericMethod;

/**
 */
public class SimpleWorkflow extends Workflow {
    @Override
    public boolean execute(ExecutionContext context) {
        SimpleRuntimeService srs = (SimpleRuntimeService) context.getRuntimeService();
        SimpleTestTextFile initialTextFile = srs.createInitialTextFile(context);
        SimpleTestTextFile textFile1 = initialTextFile.test1(); //(TextFile) GenericMethod.callGenericTool("testScript", initialTextFile);
        SimpleTestTextFile textFile2 = textFile1.test2();//(TextFile) GenericMethod.callGenericTool("testScript", textFile1);
        SimpleTestTextFile textFile3 = textFile2.test3(); //(TextFile) GenericMethod.callGenericTool("testScriptExitBad", textFile2);
        return true;
    }
}
