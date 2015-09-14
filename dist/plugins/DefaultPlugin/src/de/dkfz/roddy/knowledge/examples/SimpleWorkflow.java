package de.dkfz.roddy.knowledge.examples;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.core.Workflow;

/**
 */
public class SimpleWorkflow extends Workflow {
    @Override
    public boolean execute(ExecutionContext context) {
        SimpleRuntimeService srs = (SimpleRuntimeService) context.getRuntimeService();
        TextFile initialTextFile = srs.createInitialTextFile(context);
        TextFile textFile1 = return GenericMethod.callGenericTool("testScript", initialTextFile);
        TextFile textFile2 = return GenericMethod.callGenericTool("testScript", textFile2);
        TextFile textFile3 = return GenericMethod.callGenericTool("testScriptExitBad", textFile3);
    }
}
