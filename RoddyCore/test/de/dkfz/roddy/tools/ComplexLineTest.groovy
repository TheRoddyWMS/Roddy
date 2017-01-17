/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.tools

import groovy.transform.CompileStatic
import org.junit.Test

/**
 * A test class for the Complex Line class
 * Created by heinold on 16.01.17.
 */
@CompileStatic
class ComplexLineTest {


    public static final String commandLineExample = "--abc --def=\"ab,def;efk=test;anotherValue='equals'\" --jki --a=\"something=def\""
    public static final String[] splittedCommandLineExample = [
            "--abc",
            "--def=\"ab,def;efk=test;anotherValue='equals'\"",
            "--jki",
            "--a=\"something=def\""
    ]

    @Test
    public void testParseLine() {

        ComplexLine.ParsedLine parsed = ComplexLine.parseLine(commandLineExample);
        assert parsed.content == "--abc --def=\"###CHILD_0###\" --jki --a=\"###CHILD_1###\""
        assert parsed.children[0].content == "ab,def;efk=test;anotherValue='###CHILD_0###'"
        assert parsed.children[1].content == "something=def"
        assert parsed.children[0].children[0].content == "equals"
        assert !parsed.children[0].children[0].children
    }

    @Test
    public void testReassemble() {
        ComplexLine.ParsedLine parsed = ComplexLine.parseLine(commandLineExample)
        assert parsed.reassemble() == commandLineExample

        assert parsed.toString() == commandLineExample
    }

    @Test
    public void testSplitLine() {
        ComplexLine cls = new ComplexLine(commandLineExample);
        assert cls.splitBy(" ") == splittedCommandLineExample as List<String>
    }
}
