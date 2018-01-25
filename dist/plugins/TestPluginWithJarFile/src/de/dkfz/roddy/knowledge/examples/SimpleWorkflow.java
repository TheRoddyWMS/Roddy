/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.examples;


import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.core.Workflow;
import de.dkfz.roddy.execution.io.ExecutionService;
import de.dkfz.roddy.knowledge.files.BaseFile;
import de.dkfz.roddy.knowledge.files.Tuple4;

/**
 */
public class SimpleWorkflow extends Workflow {
    @Override
    public boolean execute(ExecutionContext context) {
        SimpleRuntimeService srs = (SimpleRuntimeService) context.getRuntimeService();
        BaseFile abc = getSourceFile(context, "/tmp/test.txt");
        SimpleTestTextFile initialTextFile = srs.createInitialTextFile(context);
        SimpleTestTextFile textFile1 = initialTextFile.test1(); //(TextFile) GenericMethod.callGenericTool("testScript", initialTextFile);
        call("testScriptWithParams", textFile1, "aJobParameter=A_JOB_PARAMETER_VALUE");
        SimpleTestTextFile textFile2 = textFile1.test2();//(TextFile) GenericMethod.callGenericTool("testScript", textFile1);
        if (getflag(context, "runWithErrors", true))
            textFile2.test3(); //(TextFile) GenericMethod.callGenericTool("testScriptExitBad", textFile2);
        Tuple4 mout1 = (Tuple4) call("testScriptWithMultiOut", textFile2);
        Tuple4 mout2 = (Tuple4) call("testScriptWithMultiOut2", textFile2);
        return true;
    }
}
