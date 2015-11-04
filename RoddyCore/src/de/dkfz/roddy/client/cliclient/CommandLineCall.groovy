package de.dkfz.roddy.client.cliclient

import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.client.RoddyStartupModes
import de.dkfz.roddy.client.RoddyStartupOptions
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.tools.LoggerWrapper

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

    CommandLineCall(List<String> args) {
        long t1 = ExecutionService.measureStart();
        parameters = [];
        optionsMap = [:];
        if (!args.size() == 0)
            args = [RoddyStartupModes.help.toString()];
        this.arguments = args;

        List<String> parameters = args.findAll { String arg -> !arg.startsWith("--") } as ArrayList<String>;
        Collection<String> options = args.findAll { String arg -> arg.startsWith("--") };
//        ExecutionService.measureStop(t1, "measure 1", LoggerWrapper.VERBOSITY_RARE);

        t1 = ExecutionService.measureStart();
        // Try to extract the startup mode. If it not know, display the help message.
        try {
            if(parameters.size() == 0)
                startupMode = help;
            else
                startupMode = Enum.valueOf(RoddyStartupModes.class, parameters[0]);
        } catch (Exception ex) {
            logger.postAlwaysInfo("The startupmode " + parameters[0] + " is not known.");
            this.startupMode = help;
            return;
        }
//        ExecutionService.measureStop(t1, "measure 2", LoggerWrapper.VERBOSITY_RARE);

        t1 = ExecutionService.measureStart();

        //Parse options
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
                    else
                        logger.severe("The option " + option + " is malformed!");
            } catch (Exception ex) {
                logger.postAlwaysInfo("The option with " + optArg + " is malformed!");
            }
        }
//        ExecutionService.measureStop(t1, "measure 3", LoggerWrapper.VERBOSITY_RARE);

        t1 = ExecutionService.measureStart();

        // Store all parameters and remove the startup mode.
        if (parameters.size() > 1)
            this.parameters += parameters[1..-1];
        this.optionsMap.putAll(parsedOptions);
//        ExecutionService.measureStop(t1, "measure 4", LoggerWrapper.VERBOSITY_RARE);
    }

    List<String> getParameters() {
        return new LinkedList(parameters);
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
    public List<String> getSetConfigurationValues() {
        return optionsMap.get(RoddyStartupOptions.cvalues, []);
    }
}
