/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.tools

import groovy.transform.CompileStatic

/**
 * A complex line representing e.g. a command line or a code line
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

    /** The full line passed to the ComplexLine **/
    final String line;

    /** The parsed line **/
    LineNode parsedLine;

    /**
     * Basic class for line nodes. A String is converted to a tree of these.
     *
     * E.g. --abc --def="ab,def;efk=(test);anotherValue='equals'" --jki --a="something=def"
     *
     * will be represented like:
     *
     * Node 0:      [ TN:'--abc --def=', CN:Node 1, TN:'" --jki --a="', CN:Node 4, TN:'"' ]
     * Node 1:      [ TN:'ab,def;efk=(', CN:Node 2, TN:");anotherValue='", CN:Node 3, TN:"'", TN:'"' ]
     * Node 2:      [ TN:'test' ]
     * Node 3:      [ TN:'equals' ]
     * Node 4:      [ TN:'something-def' ]
     */
    static abstract class LineNode {

        abstract String reassemble()

        abstract String[] spliteNodeContentBy(String s)

        boolean isTextNode() { return false; }

        boolean isComplexNode() { return false; }

        @Override
        public String toString() {
            return reassemble()
        }
    }

    /**
     * Represents a block of text
     */
    static class TextNode extends LineNode {
        StringBuilder text = new StringBuilder()

        TextNode() {

        }

        TextNode(String start) {
            this.text << start
        }

        @Override
        boolean isTextNode() {
            return true;
        }

        @Override
        String reassemble() {
            return text.toString()
        }

        @Override
        String[] spliteNodeContentBy(String s) {
            return text.toString().split(s)
        }
    }

    /**
     * Represents a complex node which holds an instance of text and complex node objects.
     */
    static class ComplexNode extends LineNode {
        List<LineNode> blocks = []

        ComplexNode() {
            blocks << new TextNode()
        }

        @Override
        boolean isComplexNode() {
            return true;
        }

        @Override
        String reassemble() {
            StringBuilder _content = new StringBuilder();
            for (int i = 0; i < blocks.size(); i++) {
                _content << blocks[i].reassemble()
            }
            return _content.toString();
        }

        String[] spliteNodeContentBy(String s) {
            // Keep a list of splitted, prepared blocks. The list will be used to join results.
            List<StringBuilder> splittedBlocks = []
            for (int i = 0; i < blocks.size(); i++) {
                List<String> splitted = null
                if (blocks[i].isTextNode()) {
                    splitted = blocks[i].spliteNodeContentBy(s) as List<String>

                    // If our string ends with the splitter, the splitted entry would be lost. Append an empty entry to preserve it.
                    if (blocks[i].reassemble().endsWith(s))
                        splitted = splitted + [""]
                } else if (blocks[i].isComplexNode()) {
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


    static isOpeningOrClosingCharacter(Character c) {
        return possibleContainersByOpeningCharacter.containsKey(c) || possibleContainersByClosingCharacter.containsKey(c)
    }

    static fitsToOpeningCharacter(Character c, Character o) {
        return possibleContainersByClosingCharacter[c] == o
    }

    static LineNode parseLine(String line) {

        Stack<Character> openedStarts = new Stack<>();
        openedStarts.push('ö' as char) // Just put some character

        ComplexNode current = new ComplexNode();
        Stack<ComplexNode> lineNodes = new Stack<>()
        lineNodes.push(current)

        for (int i = 0; i < line.length(); i++) {

            char character = line.getChars()[i];

            if (!isOpeningOrClosingCharacter(character)) {
                ((TextNode) current.blocks[-1]).text << character;
            } else if (fitsToOpeningCharacter(character, openedStarts.peek())) {
                openedStarts.pop()
                if (lineNodes.size() > 0) // First, check if something is in the Stack, then pop from it
                    lineNodes.pop()
                if (lineNodes.size() > 0)  // If there is still something in the Stack, peek it.
                    current = lineNodes.peek()
                current.blocks << new TextNode(character as String);
            } else {
                ((TextNode) current.blocks[-1]).text << character;
                openedStarts.push(character)
                current.blocks << new ComplexNode()
                current = current.blocks[-1] as ComplexNode
                lineNodes.push(current);
            }
        }
        if (lineNodes.size() > 1) throw new IOException("The line $line is malformed. There is a closing literal missing.")
        return current;
    }

    ComplexLine(String line) {
        this.line = line;
        this.parsedLine = parseLine(line)
    }

    String[] splitBy(String s) {
        return parsedLine.spliteNodeContentBy(s)
    }

    @Override
    String toString() {
        return line
    }

}
