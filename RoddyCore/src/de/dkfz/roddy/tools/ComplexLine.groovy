/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.tools

import groovy.transform.CompileStatic

/**
 * A complex line representing e.g. a command line.
 *
 * The complex line will try to identify enclosing single or double ticks.
 *
 * Created by heinold on 16.01.17.
 */
@CompileStatic
class ComplexLine {

    static Map<Character, Character> possibleContainersByOpeningCharacter = [:]
    static Map<Character, Character> possibleContainersByClosingCharacter = [:]

    static {
        possibleContainersByOpeningCharacter['\'' as char] = '\'' as Character
        possibleContainersByOpeningCharacter['\"' as char] = '\"' as Character
        possibleContainersByOpeningCharacter['{' as char] = '}' as Character
        possibleContainersByOpeningCharacter['[' as char] = ']' as Character
        possibleContainersByOpeningCharacter['(' as char] = ')' as Character

        possibleContainersByOpeningCharacter.each { Character key, Character value -> possibleContainersByClosingCharacter[value] = key }
    }

    String line;
    LineNode parsedLine;

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
    static abstract class LineNode {

        abstract String reassemble()

        abstract String[] splitBy(String s)

        @Override
        public String toString() {
            return reassemble()
        }
    }

    static class TextNode extends LineNode {
        StringBuilder text = new StringBuilder()

        TextNode() {

        }

        TextNode(String start) {
            this.text << start
        }

        @Override
        String reassemble() {
            return text.toString()
        }

        @Override
        String[] splitBy(String s) {
            return text.toString().split(s)
        }
    }

    static class ComplexNode extends LineNode {
        List<LineNode> blocks = []

        ComplexNode() {
            blocks << new TextNode()
        }


        @Override
        String reassemble() {
            StringBuilder _content = new StringBuilder();
            for (int i = 0; i < blocks.size(); i++) {
                _content << blocks[i].reassemble()
            }
            return _content.toString();
        }

        public String[] splitBy(String s) {
            // Keep a list of splitted, prepared blocks. The list will be used to join results.
            List<StringBuilder> splittedBlocks = []
            for (int i = 0; i < blocks.size(); i++) {
                List<String> splitted = null
                if (blocks[i] instanceof TextNode) {
                    splitted = blocks[i].splitBy(s) as List<String>

                    // If our string ends with the splitter, the splitted entry would be lost. Append an empty entry to preserve it.
                    if (blocks[i].reassemble().endsWith(s))
                        splitted = splitted + [""]
                }
                if (blocks[i] instanceof ComplexNode) {
                    splitted = [blocks[i].reassemble()] // Complex nodes will just be reassembled. We don't want to split over multi levels!
                }

                // Convert the found strings to builders
                List<StringBuilder> newBuilders = splitted.collect { new StringBuilder(it) }

                // If there already is a splitted entry, append the first new found string to the last entry.
                if (splittedBlocks) {
                    splittedBlocks[-1] << newBuilders[0]
                    if (newBuilders.size() > 1)
                        splittedBlocks += newBuilders[1..-1]
                } else
                    splittedBlocks += newBuilders // If there is nothing, just take all entries as new entries.
            }
            // Collect, convert, return
            def res = splittedBlocks.collect { new String(it) }
            return res as String[]
        }
    }


    public static isOpeningOrClosingCharacter(Character c) {
        return possibleContainersByOpeningCharacter.containsKey(c) || possibleContainersByClosingCharacter.containsKey(c)
    }

    public static fitsToOpeningCharacter(Character c, Character o) {
        return possibleContainersByClosingCharacter[c] == o
    }

    public static LineNode parseLine(String line) {

        Stack<Character> openedStarts = new Stack<>();
        openedStarts.push('ö' as char)

        ComplexNode current = new ComplexNode();
        Stack<ComplexNode> lineNodes = new Stack<>()
        lineNodes.push(current)

        for (int i = 0; i < line.length(); i++) {

            char character = line.getChars()[i];

            if (!isOpeningOrClosingCharacter(character)) {
                ((TextNode) current.blocks[-1]).text << character;
                continue;
            } else if (fitsToOpeningCharacter(character, openedStarts.peek())) {
                openedStarts.pop()
                if (lineNodes.size() > 0)
                    lineNodes.pop()
                if (lineNodes.size() > 0)
                    current = lineNodes.peek()
                current.blocks << new TextNode(character as String);
            } else {
                ((TextNode) current.blocks[-1]).text << character;
                openedStarts.push(character)
                current.blocks << new ComplexNode()
//                lineNodes.peek().children << current
                current = current.blocks[-1] as ComplexNode
                lineNodes.push(current);
            }
        }
        if (lineNodes.size() > 1) throw new IOException("The line $line is malformed. There is an enclosing literal missing.")
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
