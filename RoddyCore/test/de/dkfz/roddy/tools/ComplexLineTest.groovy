/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.tools

import groovy.transform.CompileStatic
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

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

    @Test
    void testIsOpeningOrClosingCharacter() {
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
    void testFitsToOpeningCharacter() {
        assert ComplexLine.fitsToOpeningCharacter('\'' as Character, '\'' as Character)
        assert ComplexLine.fitsToOpeningCharacter('\"' as Character, '\"' as Character)
        assert !ComplexLine.fitsToOpeningCharacter('\"' as Character, '\'' as Character)
        assert ComplexLine.fitsToOpeningCharacter('}' as Character, '{' as Character)
        assert ComplexLine.fitsToOpeningCharacter(']' as Character, '[' as Character)
        assert ComplexLine.fitsToOpeningCharacter(')' as Character, '(' as Character)
    }

    @Test
    void testParseSimpleLine() {
        ComplexLine.ComplexNode parsed = ComplexLine.parseLine("a'bcd'e f'gh'i") as ComplexLine.ComplexNode
        assert parsed.blocks.size() == 5
        assert parsed.blocks[0] instanceof ComplexLine.TextNode && parsed.blocks[0].reassemble() == "a'"
        assert parsed.blocks[1] instanceof ComplexLine.ComplexNode && parsed.blocks[1].reassemble() == "bcd"
        assert parsed.blocks[2] instanceof ComplexLine.TextNode && parsed.blocks[2].reassemble() == "'e f'"
        assert parsed.blocks[3] instanceof ComplexLine.ComplexNode && parsed.blocks[3].reassemble() == "gh"
        assert parsed.blocks[4] instanceof ComplexLine.TextNode && parsed.blocks[4].reassemble() == "'i"
    }

    @Test
    void testParseCommandLine() {
        ComplexLine.ComplexNode parsed = ComplexLine.parseLine(commandLineExample) as ComplexLine.ComplexNode;
        assert parsed.blocks.size() == 5
        assert parsed.blocks[0] instanceof ComplexLine.TextNode && parsed.blocks[0].reassemble() == "--abc --def=\""
        assert ((ComplexLine.ComplexNode) parsed.blocks[1]).blocks[0].reassemble() == "ab,def;efk=test;anotherValue='"
        assert parsed.blocks[2] instanceof ComplexLine.TextNode && parsed.blocks[2].reassemble() == "\" --jki --a=\""
        assert parsed.blocks[3] instanceof ComplexLine.ComplexNode && parsed.blocks[3].reassemble() == "something=def"
        assert parsed.blocks[4] instanceof ComplexLine.TextNode && parsed.blocks[4].reassemble() == "\""
    }

    @Test
    void testParseCodeLine() {
        ComplexLine.ComplexNode parsed = ComplexLine.parseLine(codeLineExample) as ComplexLine.ComplexNode;
        assert parsed.blocks.size() == 7
        assert parsed.blocks[0] instanceof ComplexLine.TextNode && parsed.blocks[0].reassemble() == "def multiline='"
        assert ((ComplexLine.ComplexNode) parsed.blocks[1]).blocks[0].reassemble() == "a string"
        assert parsed.blocks[2] instanceof ComplexLine.TextNode && parsed.blocks[2].reassemble() == "', b={"
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none()

    @Test
    void testMissingClosingParenthesis() {
        String faultyLineExample = "a(b(c())"
        thrown.expect(IOException)
        thrown.expectMessage("The line $faultyLineExample is malformed. There is a closing literal missing.")
        ComplexLine.parseLine(faultyLineExample)

        thrown.expect(IOException)
        ComplexLine.parseLine("(blabla")

        thrown.expect(IOException)
        ComplexLine.parseLine(")")
    }

    @Test
    void testConfusedQuotes() {
        thrown.expect(IOException)
        ComplexLine.parseLine("(a-\"b)-\"")
    }

    @Test
    void testMissingOpeningBrace() {
        thrown.expect(IOException)
        ComplexLine.parseLine("blabla}")
    }

    @Test
    void testMissingClosingQuote() {
        thrown.expect(IOException)
        ComplexLine.parseLine("\"blabla'")
    }

    @Test
    void testOpeningBraceOnly() {
        thrown.expect(IOException)
        ComplexLine.parseLine("{")
    }

    @Test
    void testReassembleCommandLine() {
        ComplexLine.LineNode parsed = ComplexLine.parseLine(commandLineExample)
        assert parsed.reassemble() == commandLineExample

        assert parsed.toString() == commandLineExample
    }

    @Test
    void testReassembleCodeLine() {
        ComplexLine.LineNode parsed = ComplexLine.parseLine(codeLineExample)
        assert parsed.reassemble() == codeLineExample

        assert parsed.toString() == codeLineExample
    }

    @Test
    void testSplitCommandLine() {
        ComplexLine cls = new ComplexLine(commandLineExample);
        assert cls.splitBy(" ") == splittedCommandLineExample as List<String>
    }

    @Test
    void testSplitCodeLine() {
        ComplexLine cls = new ComplexLine(codeLineExample);
        assert cls.splitBy(",") == splittedCodeLineExample as List<String>
    }
}
