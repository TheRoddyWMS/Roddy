/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config

import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextError
import de.dkfz.roddy.knowledge.files.BaseFile
import groovy.transform.CompileStatic

import static de.dkfz.roddy.StringConstants.BRACE_RIGHT
import static de.dkfz.roddy.StringConstants.EMPTY
import static de.dkfz.roddy.StringConstants.EMPTY
import static de.dkfz.roddy.StringConstants.SPLIT_COMMA
import static de.dkfz.roddy.StringConstants.SPLIT_EQUALS

/**
 * Created by heinold on 10.01.17.
 */
@CompileStatic
class FilenamePatternHelper {

    public static class Command {
        public final String name;
        public final Map<String, CommandAttribute> attributes = new HashMap<String, CommandAttribute>();

        public Command(String name, Map<String, CommandAttribute> attributes) {
            this.name = name;
            if (attributes != null)
                this.attributes.putAll(attributes);
        }
    }

    public static class CommandAttribute {
        public final String name;
        public final String value;

        public CommandAttribute(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    public static Command extractCommand(ExecutionContext context, String commandID, String temp, int startIndex = -1) {
        if (startIndex == -1)
            startIndex = temp.indexOf(commandID);
        int endIndex = temp.indexOf(BRACE_RIGHT, startIndex);
        String command = temp[startIndex .. endIndex]

        Map<String, CommandAttribute> attributes = [:]

        // Split up a command like ${jobParameter,name=...,desc=...} by comma
        String[] split = command.split(SPLIT_COMMA);
        for (int i = 1; i < split.length; i++) { //Start with the first option.

            // Replace every character in the String which EXCEPT the ones in the square brackets with EMPTY.
            // Gets rid of illegal characters and makes further parsing easier
            String _split = split[i].replaceAll("[^0-9a-zA-Z=.-_+#]", EMPTY);
            String[] attributeSplit = _split.split(SPLIT_EQUALS);
            String name = attributeSplit[0]; //Get the tag name
            String value = EMPTY; // Get the tag value (might be empty)

            if (attributeSplit.length == 2) // If it has a value
                value = attributeSplit[1].replace('"', EMPTY); // Get rid of leading or trailing double quotes
            attributes[name] = new CommandAttribute(name, value);
        }

        // Check if the attribute at least has a name tag
        if(!attributes["name"]) {
            context.addErrorEntry(ExecutionContextError.EXECUTION_SETUP_INVALID.expand("The jobParameter for ${command} must have a name tag with a value."))
        }

        return new Command(command, attributes);
    }

    /**
     * Extract a list of similar commands from a string.
     * The code looks overly complicated due to missing functions in groovy or due to
     * funny behaviour of groovy methods. Maybe it would've been better to implement it in Java
     * I don't know.
     * @param commandID
     * @param temp
     * @return
     */
    public static List<Command> extractCommands(ExecutionContext context, String commandID, String temp) {
        def cmds = []
        // This simple method to get the no of instances is not possible! Groovy will try to compile a pattern from things like ${jobParameter
        // It seems, that $ and { trigger an internal mechanism when used with findAll.
        //        int no = temp.findAll(commandID).size()

        // Now go for a more complicated BUT secure way; (Still I hate it)
        String _temp = temp.replace("\${", "##;##")
        String _commandID = commandID.replace("\${", "##;##")
        int no = _temp.findAll(_commandID).size()

        // Groovy does not have a simple findAllIndexValuesOf or something like it.
        // Maybe with a pattern matching. But I don't want to waste too much time on it.
        int lastIndex = 0
        for (int i = 0; i < no; i++) {
            lastIndex = temp.indexOf(commandID, lastIndex + 1);
            cmds << extractCommand(context, commandID, temp, lastIndex);
        }
        return cmds
    }
}
