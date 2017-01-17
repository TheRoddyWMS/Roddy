/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.tools

import groovy.transform.CompileStatic
import org.apache.commons.cli.CommandLine

/**
 * A complex line representing e.g. a command line.
 *
 * The complex line will try to identify enclosing single or double ticks.
 *
 * Brackets are not detected yet! Maybe make that available in the future. The tests for brackets are a bit different to ticks.
 *
 * Created by heinold on 16.01.17.
 */
@CompileStatic
class ComplexLine {
    String line;
    ParsedLine parsedLine;

    /**
     * Parsed line represents a transformed / parsed line. Currently, single and double ticks will be used
     * to identify children.
     *
     * E.g. --abc --def="ab,def;efk=test;anotherValue='equals'" --jki --a="something=def"
     *
     * will be represented like:
     *
     *     --abc --def="###CHILD_0###" --jki --a="###CHILD_1###"
     *                       |                         |
     *                       ab,def;efk=test;anotherValue="###CHILD_0###"
     *                                                 |        |
     *                                                 something=def
     *                                                          |
     *                                                          equals
     */
    static class ParsedLine {
        String content = "";
        List<ParsedLine> children = [];

        ParsedLine() {
        }

        public String reassemble() {
            String _content = content;
            for (int i = 0; i < children.size(); i++) {
                _content = _content.replace("###CHILD_${i}###", children[i].reassemble());
            }
            return _content;
        }

        public String[] splitBy(String s) {
            def res = content.split(s);

            for (int i = 0; i < res.size(); i++) {
                def pl = new ParsedLine()
                pl.content = res[i];
                pl.children = children;
                res[i] = pl.reassemble()
            }
            return res;
        }

        @Override
        public String toString() {
            return reassemble()
        }
    }

    public static ParsedLine parseLine(String line) {
        List<Character> possibleContainers = ['\'' as char, '\"' as char]
        Stack<Character> openedStarts = new Stack<>();
        openedStarts.push('ö' as char)

        ParsedLine current = new ParsedLine();
        Stack<ParsedLine> parsedLines = new Stack<>()
        parsedLines.push(current)

        for (int i = 0; i < line.length(); i++) {

            char character = line.getChars()[i];

            if (!possibleContainers.contains(character)) {
                current.content += character;
                continue;
            } else if (openedStarts.peek() == character) {
                openedStarts.pop()
                if (parsedLines.size() > 0)
                    parsedLines.pop()
                if (parsedLines.size() > 0)
                    current = parsedLines.peek()
                current.content += character;
            } else {
                current.content += character;
                openedStarts.push(character)
                current.content += "###CHILD_${current.children.size()}###"
                current = new ParsedLine()
                parsedLines.peek().children << current
                parsedLines.push(current);
            }
        }
        return current;
    }

    ComplexLine(String line) {
        this.line = line;
        this.parsedLine = parseLine(line)
    }

    String[] splitBy(String s) {
        return parsedLine.splitBy(s)
    }

    @Override
    public String toString() {
        return line
    }
}
