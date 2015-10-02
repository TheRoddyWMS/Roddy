package de.dkfz.roddy.knowledge.examples;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.core.Workflow;

/**
 */
public class TestWorkflow extends Workflow {
    @Override
    public boolean execute(ExecutionContext context) {
        SimpleRuntimeService srs = (SimpleRuntimeService) context.getRuntimeService();
        TextFile initialTextFile = srs.createInitialTextFile(context);
        TextFile textFile1 = initialTextFile.test1();
        FileWithChildren fileWithChildren = initialTextFile.testFWChildren();
        TextFile textFile2 = textFile1.test2();
        TextFile textFile3 = textFile2.test3();
        return true;
    }
}
