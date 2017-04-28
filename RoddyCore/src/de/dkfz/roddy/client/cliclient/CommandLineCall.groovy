/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.cliclient

import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.client.RoddyStartupModes
import de.dkfz.roddy.client.RoddyStartupOptions
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.tools.LoggerWrapper

import static de.dkfz.roddy.StringConstants.SPLIT_COMMA
import static de.dkfz.roddy.client.RoddyStartupModes.help

/**
 * A helper class for command line calls.
 * Options and parameters are extracted and put into an object of this class.
 */
@groovy.transform.CompileStatic
public class CommandLineCall {
    private static LoggerWrapper logger = LoggerWrapper.getLogger(RoddyCLIClient.class.getSimpleName());
    public final RoddyStartupModes startupMode;
    private final List<String> arguments;
    private final List<String> parameters;
    private Map<RoddyStartupOptions, List<String>> optionsMap;
    final boolean malformed

    CommandLineCall(List<String> args) {
        parameters = [];
        optionsMap = [:];
        if (!args.size() == 0)
            args = [RoddyStartupModes.help.toString()];
        this.arguments = args;

        List<String> parameters = args.findAll { String arg -> !arg.startsWith("--") } as ArrayList<String>;
        Collection<String> options = args.findAll { String arg -> arg.startsWith("--") };

        // Try to extract the startup mode. If it not know, display the help message.
        try {
            startupMode = parameters ? parameters[0] as RoddyStartupModes : help;
        } catch (Exception ex) {
            logger.postAlwaysInfo("The startupmode " + parameters[0] + " is not known.");
            this.startupMode = help;
            return;
        }

        //Parse options
        List<String> errors = []
        Map<RoddyStartupOptions, List<String>> parsedOptions = [:];
        for (String optArg in options) {
            String[] split = optArg.split(StringConstants.SPLIT_EQUALS);
            try {
                RoddyStartupOptions option = split[0][2..-1] as RoddyStartupOptions;
                //TODO This needs to be reworked because:
                //i.e. --cvalues=test:abc,test2:(e,e,f),test3("A string, with a comma") will seriously make problems!
                //Leave it for now, but come back to it if it is necessary.
                List<String> values = option.acceptsParameters ? split[1].split(StringConstants.SPLIT_COMMA)?.toList() : null;
                parsedOptions[option] = values;
                if (option.acceptsParameters)
                    if (values)
                        parsedOptions[option] = values;
                    else {
                        errors << "The option " + option + " is malformed!"
                    }
            } catch (Exception ex) {
                errors << "The option with " + optArg + " is malformed!";
            }
        }

        if(errors)
            logger.severe(errors.join("\n\t"))
        malformed = errors

        // Store all parameters and remove the startup mode.
        if (parameters.size() > 1)
            this.parameters += parameters[1..-1];
        this.optionsMap.putAll(parsedOptions);
    }

    List<String> getParameters() {
        return new LinkedList(parameters);
    }

    String getAnalysisID() {
        return parameters[0];
    }

    List<String> getDatasetSpecifications() {
        return Arrays.asList(parameters[1].split(SPLIT_COMMA));
    }

    public boolean hasParameters() {
        return parameters.size() > 0;
    }

    public boolean isOptionSet(RoddyStartupOptions option) {
        return optionsMap.containsKey(option);
    }

    public List<RoddyStartupOptions> getOptionList() {
        return optionsMap.keySet().asList();
    }

    public String getOptionValue(RoddyStartupOptions option) {
        return optionsMap.get(option)?.first();
    }

    public List<String> getOptionList(RoddyStartupOptions option) {
        return optionsMap.get(option);
    }

    public List<String> getArguments() {
        return arguments;
    }

    /**
     * Returns all set configuration values as a list in the format:
     * [ a:a string, b:another string, c:123, ... ]
     * @return
     */
    public List<ConfigurationValue> getSetConfigurationValues() {
        def externalConfigurationValues = optionsMap.get(RoddyStartupOptions.cvalues, []);
        def configurationValues = []
        for (eVal in externalConfigurationValues) {
            String[] splitIDValue = eVal.split(StringConstants.SPLIT_COLON);
            //TODO Put in a better error checking when converting the split string to a configuration value.
            //Remark, if a value contains : / colons, it will not work!
            String cvalueId = splitIDValue[0];
            String value = (splitIDValue.size() >= 2 ? splitIDValue[1] : ""); // Surround value with single quotes '' to prevent string interpretation for e.g. execution in bash
            String type = splitIDValue.size() >= 3 ? splitIDValue[2] : null; // If null is set, type is guessed.

            configurationValues << new ConfigurationValue(cvalueId, value, type)
        }
        configurationValues
    }
}
