/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs

import de.dkfz.roddy.core.MockupExecutionContextBuilder
import groovy.transform.CompileStatic
import org.junit.Ignore
import org.junit.Test

/**
 * Created by heinold on 14.07.16.
 */
@CompileStatic
class CommandTest extends GroovyTestCase {

    @Test
    @Ignore("Test to create")
    void testGetParametersForParameterFile() {
        def context = MockupExecutionContextBuilder.createSimpleContext(CommandTest)
//        Command mock = new Command(new FakeBEJob(context), context, "MockupCommand", [
//                "ParmA": "Value",
//                "arr"  : "(a b c )",
//                "int"  : "1"
//        ]) {}

//        def parameterList = mock.getParametersForParameterFile()
//        assert parameterList == [
//                new ConfigurationValue("ParmA", "Value", "string"),
//                new ConfigurationValue("arr", "(a b c )", "bashArray"),
//                new ConfigurationValue("int", "1", "integer"),
//        ]
    }
}
