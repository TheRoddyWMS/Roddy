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

    public static final String codeLineExample = "def multiline='a string', b={something[in a braquet]}, method(a(b()))"
    public static final String[] splittedCodeLineExample = [
            "def multiline='a string'",
            " b={something[in a braquet]}",
            " method(a(b()))"
    ]

    public static final String faultyLineExample = "a(b(c())"

    @Test
    public void testIsOpeningOrClosingCharacter() {
        assert ComplexLine.isOpeningOrClosingCharacter('\'' as Character);
        assert ComplexLine.isOpeningOrClosingCharacter('\"' as Character);
        assert ComplexLine.isOpeningOrClosingCharacter('[' as Character);
        assert ComplexLine.isOpeningOrClosingCharacter(']' as Character);
        assert ComplexLine.isOpeningOrClosingCharacter('{' as Character);
        assert ComplexLine.isOpeningOrClosingCharacter('}' as Character);
        assert ComplexLine.isOpeningOrClosingCharacter('(' as Character);
        assert ComplexLine.isOpeningOrClosingCharacter(')' as Character);
    }

    @Test
    public void testFitsToOpeningCharacter() {
        assert ComplexLine.fitsToOpeningCharacter('\'' as Character, '\'' as Character)
        assert ComplexLine.fitsToOpeningCharacter('\"' as Character, '\"' as Character)
        assert !ComplexLine.fitsToOpeningCharacter('\"' as Character, '\'' as Character)
        assert ComplexLine.fitsToOpeningCharacter('}' as Character, '{' as Character)
        assert ComplexLine.fitsToOpeningCharacter(']' as Character, '[' as Character)
        assert ComplexLine.fitsToOpeningCharacter(')' as Character, '(' as Character)
    }

    @Test
    public void testParseCommandLine() {
        ComplexLine.ParsedLine parsed = ComplexLine.parseLine(commandLineExample);
        assert parsed.content == "--abc --def=\"###CHILD_0###\" --jki --a=\"###CHILD_1###\""
        assert parsed.children[0].content == "ab,def;efk=test;anotherValue='###CHILD_0###'"
        assert parsed.children[1].content == "something=def"
        assert parsed.children[0].children[0].content == "equals"
        assert !parsed.children[0].children[0].children
    }

    @Test
    public void testParseCodeLine() {
        ComplexLine.ParsedLine parsed = ComplexLine.parseLine(codeLineExample);
        assert parsed.content == "def multiline='###CHILD_0###', b={###CHILD_1###}, method(###CHILD_2###)"
        assert parsed.children[0].content == "a string"
        assert parsed.children[1].content == "something[###CHILD_0###]"
        assert parsed.children[2].content == "a(###CHILD_0###)"
        assert parsed.children[1].children[0].content == "in a braquet"
        assert parsed.children[2].children[0].content == "b(###CHILD_0###)"
        assert parsed.children[2].children[0].children.size() == 1 // Whyever this is, the array is of size 1... Actually does not matter so ignore it.
    }

    @Test(expected = IOException)
    public void testFaultyLine() {
        ComplexLine.parseLine(faultyLineExample)
    }

    @Test
    public void testReassembleCommandLine() {
        ComplexLine.ParsedLine parsed = ComplexLine.parseLine(commandLineExample)
        assert parsed.reassemble() == commandLineExample

        assert parsed.toString() == commandLineExample
    }

    @Test
    public void testReassembleCodeLine() {
        ComplexLine.ParsedLine parsed = ComplexLine.parseLine(codeLineExample)
        assert parsed.reassemble() == codeLineExample

        assert parsed.toString() == codeLineExample
    }

    @Test
    public void testReassembleSplittedCommandLine() {
        ComplexLine cls = new ComplexLine(commandLineExample);
        assert cls.splitBy(" ") == splittedCommandLineExample as List<String>
    }

    @Test
    public void testReassembleSplittedCodeLine() {
        ComplexLine cls = new ComplexLine(codeLineExample);
        assert cls.splitBy(",") == splittedCodeLineExample as List<String>
    }
}
