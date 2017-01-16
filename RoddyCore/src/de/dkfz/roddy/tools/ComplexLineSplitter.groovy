/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.tools

import groovy.transform.CompileStatic
import org.apache.commons.cli.CommandLine

/**
 * Created by heinold on 16.01.17.
 */
@CompileStatic
class ComplexLineSplitter {
    String line;
    def parsedLine;


    static class ParsedLine {
        String content = "";
        List<ParsedLine> children = [];

        ParsedLine() {
        }

        public String toString() {

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

    ComplexLineSplitter(String line) {
        this.line = line;
        this.parsedLine = parseLine(line)
    }

    List<String> splitBy(String s) {

    }
}
