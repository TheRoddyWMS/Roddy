/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.examples;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.core.Workflow;
import de.dkfz.roddy.knowledge.files.Tuple4;

/**
 */
public class TestWorkflow extends Workflow {
    @Override
    public boolean execute(ExecutionContext context) {
        SimpleRuntimeService srs = (SimpleRuntimeService) context.getRuntimeService();
        SimpleTestTextFile initialTextFile = srs.createInitialTextFile(context);
        SimpleTestTextFile textFile1 = initialTextFile.test1();
        FileWithChildren fileWithChildren = initialTextFile.testFWChildren();
        SimpleTestTextFile textFile2 = textFile1.test2();
        SimpleTestTextFile textFile3 = textFile2.test3();
        Tuple4 mout = (Tuple4) call("testScriptWithMultiOut", textFile3);
        return true;
    }
}
