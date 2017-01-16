/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.tools

import groovy.transform.CompileStatic
import org.junit.Test

/**
 *
 * Created by heinold on 16.01.17.
 */
@CompileStatic
class ComplexLineSplitterTest {

    @Test
    public void testParseLine() {
        def aLine="--abc --def=\"ab,def;efk=test;anotherValue='equals'\" --jki --a=\"something=def\""

        ComplexLineSplitter.ParsedLine parsed = ComplexLineSplitter.parseLine(aLine);
        assert parsed.content == "--abc --def=\"###CHILD_0###\" --jki --a=\"###CHILD_1###\""
        assert parsed.children[0].content == "ab,def;efk=test;anotherValue='###CHILD_0###'"
        assert parsed.children[1].content == "something=def"
        assert parsed.children[0].children[0].content == "equals"
        assert !parsed.children[0].children[0].children
    }

    @Test
    public void testSplitLine() {
        def aLine="--abc --def=\"ab,def;efk=test\" --jki"
        ComplexLineSplitter cls = new ComplexLineSplitter(aLine);
        def result = cls.splitBy(" ")
        assert result.size() == 3
        assert result[0] == "--abc"
        assert result[1] == "--def=\"ab,def;efk=test\""
        assert result[2] == "--jki"
    }
}
