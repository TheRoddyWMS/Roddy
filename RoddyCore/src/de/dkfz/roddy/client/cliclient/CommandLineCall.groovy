/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.cliclient

import static de.dkfz.roddy.StringConstants.SPLIT_COMMA
import static de.dkfz.roddy.client.RoddyStartupModes.help

import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.client.RoddyStartupModes
import de.dkfz.roddy.client.RoddyStartupOptions
import de.dkfz.roddy.client.cliclient.clioutput.*
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.Tuple2
import groovy.transform.CompileDynamic
import org.petitparser.context.Result
import org.petitparser.parser.Parser

/**
 * A helper class for command line calls.
 * Options and parameters are extracted and put into an object of this class.
 */
@groovy.transform.CompileStatic
class CommandLineCall {

    private static final Parser cvaluesParser = CommandLineParameterParser.cvalueParameterExpr.end("malformed content")
    private static final Parser otherParser = CommandLineParameterParser.parameterWithValueExpr.
            or(CommandLineParameterParser.parameterWithoutValueExpr).
            or(CommandLineParameterParser.arbitraryParameterExpr).end()


    private static LoggerWrapper logger = LoggerWrapper.getLogger(RoddyCLIClient.class.getSimpleName())
    public final RoddyStartupModes startupMode
    private final List<String> arguments
    private final List<String> parameters
    private Map<RoddyStartupOptions, Parameter> optionsMap
    final boolean malformed

    static RoddyStartupModes parseStartupMode(List<String> args) {
        // The following implies that parameters (without '--') and options (with '--') can be freely mixed.
        String putativeModeSpec = args.find { String arg -> !arg.startsWith('--') }
        Optional<RoddyStartupModes> mode = RoddyStartupModes.fromString(putativeModeSpec)
        if (mode.isPresent()) {
            return mode.get()
        } else {
            logger.postAlwaysInfo("The startupmode '${putativeModeSpec}' is not known.")
            return help
        }
    }

    @CompileDynamic
    private static Optional<Parameter> processParameter(final RoddyStartupOptions option, final ArbitraryParameter parameter, final List<String> errors) {
        throw new RuntimeException("Oops! Parsed ArbitraryParameter in 'parseOptions()'")
    }

    @CompileDynamic
    private static Optional<Parameter> processParameter(final RoddyStartupOptions option, final ParameterWithValue parameter, final List<String> errors) {
        // If the option is known ensure the option parsed as with value is indeed accepting values.
        if (option.acceptsParameters) {
            Optional.of(parameter)
        } else {
            errors << "The option " + option + " is malformed! Parameter value required."
            Optional.empty()
        }
    }

    @CompileDynamic
    private static Optional<Parameter> processParameter(final RoddyStartupOptions option, final ParameterWithoutValue parameter, final List<String> errors) {
        // If the option is known ensure the option parsed as with value is indeed accepting values.
        if (!option.acceptsParameters) {
            Optional.of(parameter)
        } else {
            errors << "The option " + option + " is malformed! No parameter value expected."
            Optional.empty()
        }
    }

    @CompileDynamic
    private static Optional<Parameter> processParameter(final RoddyStartupOptions option, final CValueParameter parameter, final List<String> errors) {
        Optional.of(parameter)
    }

    /** Check whether the parsed option is indeed accepted as option by Roddy. Errors are accumulated in the error argument.
     *  If it is an accepted option, add the option value. Errors are accumulated in the error variable. */
    @CompileDynamic
    private static Optional<MapEntry> handle(final Parameter parameter, final List<String> errors) {
        Optional<RoddyStartupOptions> res = RoddyStartupOptions.fromString(parameter.name)
        if (!res.isPresent()) {
            // Add error message, for unknown options
            errors << "Unknown option '" + parameter.name + "'"
            Optional.empty()
        } else {
            RoddyStartupOptions option = res.get() as RoddyStartupOptions
            processParameter(option, parameter, errors).
                    map { new MapEntry(option, it) }
        }
    }


    /** This method exists only because java-petitparser apparently has no notion of a fatally failing parse expression with or(). Instead it simply
     *  continues to parse with the next alternative and ignores the fatal error. Maybe there is a workaround using a different grammar.
     *
      * @param optArg
     * @return
     */
    private static Result parse(String optArg) {
        Result cvalueResult = cvaluesParser.parse(optArg)
        if (cvalueResult.success || cvalueResult.message.endsWith("content") || cvalueResult.message == "end of input expected") {
            // The second case is a fatal error: A cvalues parameter with corrupt content.
            return cvalueResult
        }
        return otherParser.parse(optArg)
    }



    /**
     * Parse the options (the portion of the args that start with '--').
     *
     * @param    options
     * @return   Pair with a Map RoddyStartupOptions and their values and a list of collected errors.
     */
    private static Tuple2<Map<RoddyStartupOptions, Parameter>, List<String>> parseOptions(List<String> args) {
        // The following implies that parameters (without '--') and options (with '--') can be freely mixed
        // but order within each group matters.
        List<String> options = args.findAll { String arg -> arg.startsWith('--') }
        Map<RoddyStartupOptions, Parameter> parsedOptions = [:]
        List<String> errors = []
        for (String optArg in options) {
            Result parseResult = parse(optArg)
            if (parseResult.failure) {
                errors << "Could not parse option string '" + optArg + "': ${parseResult.message}"
            } else {
                handle(parseResult.get() as Parameter, errors).map { MapEntry entry ->
                    parsedOptions.put(entry.key as RoddyStartupOptions, entry.value as Parameter)
                }
            }
        }
        new Tuple2(parsedOptions, errors)
    }

    CommandLineCall(List<String> args) {
        // Guard against empty parameter list. Assume help is requested.
        if (args.empty)
            args = [help.toString()]
        this.arguments = args

        // Store all parameters (do not start with '--') and remove the startup mode.
        List<String> allParameters = args.findAll { String arg -> !arg.startsWith('--') } as ArrayList<String>
        if (allParameters.size() > 1)
            this.parameters = allParameters[1..-1]
        else
            this.parameters = []
        startupMode = parseStartupMode(args)

        // Now process the options (that start with '--').
        Tuple2<Map<RoddyStartupOptions, Parameter>, List<String>> options = parseOptions(args)
        if (options.y) {
            logger.severe(options.y.join("\n\t"))
            malformed = true
        } else {
            malformed = false
        }
        this.optionsMap = options.x
    }

    List<String> getParameters() {
        new LinkedList(parameters)
    }

    String getAnalysisID() {
        parameters[0]
    }

    List<String> getDatasetSpecifications() {
        Arrays.asList(parameters[1].split(SPLIT_COMMA))
    }

    boolean hasParameters() {
        parameters.size() > 0
    }

    boolean isOptionSet(RoddyStartupOptions option) {
        optionsMap.containsKey(option)
    }

    /** Get list of options (i.e. parameter names). */
    List<RoddyStartupOptions> getOptionList() {
        optionsMap.keySet().asList()
    }

    /** Split by a non-escaped character. Leading even number of escapes are accounted for. Note that because a lookahead is used,
     *  at max 100 escapes are accounted for. */
    static List<String> splitByNonEscapedCharacter(String string, Character c) {
        string.split("(?<=(?:^|[^\\\\])(?:\\\\\\\\){0,100})${c}") as List<String>
    }

    /** Return a list of values associated with the parameter option. If none are allowed for the option, null is returned. */
    List<String> getOptionValueList(RoddyStartupOptions option) {
        if (!option.acceptsParameters) {
            null
        } else {
            Parameter p = optionsMap.get(option)
            if (p instanceof CValueParameter) {
                (p as CValueParameter).cvalues.collect { k, v ->
                    "${k}:${v}"
                } as List<String>
            } else if (p instanceof ParameterWithValue) {
                splitByNonEscapedCharacter((p as ParameterWithValue).value, ',' as Character)
            } else {
                throw new RuntimeException("Possible programming error.")
            }
        }
    }

    /** Return the option's values. Note that cvalue options are reconstructed, such that leading spaces of cvalue names are discarded. */
    String getOptionValue(RoddyStartupOptions option) {
        if (!option.acceptsParameters) {
            null
        } else {
            Parameter p = optionsMap.get(option)
            if (p instanceof CValueParameter) {
                   (p as CValueParameter).cvalues.collect { k, v ->
                    "${k}:${v}"
                }.join(",")
            } else if (p instanceof ParameterWithValue) {
                (p as ParameterWithValue).value
            } else {
                throw new RuntimeException("Possible programming error.")
            }
        }
    }

    List<String> getArguments() {
        arguments
    }

    /**
     * Returns all configuration values as a list in the format:
     * [ a:a string, b:another string, c:123, ... ]
     * @return
     */
    List<ConfigurationValue> getConfigurationValues() {
        (optionsMap.get(RoddyStartupOptions.cvalues, new CValueParameter([:])) as CValueParameter).cvalues.
                collect { k, cval ->
                    new ConfigurationValue(cval.name, cval.value, cval.type)
                }
    }
}
