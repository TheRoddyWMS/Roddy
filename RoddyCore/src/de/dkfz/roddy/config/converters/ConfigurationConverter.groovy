/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.converters;

import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationConstants
import de.dkfz.roddy.config.ConfigurationValue;
import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.tools.LoggerWrapper

import java.util.logging.Logger

import static de.dkfz.roddy.StringConstants.COLON
import static de.dkfz.roddy.StringConstants.EMPTY
import static de.dkfz.roddy.StringConstants.SPLIT_UNDERSCORE
import static de.dkfz.roddy.StringConstants.UNDERSCORE;

/**
 * Base class for all kinds of configuration conversion / export classes.
 * Created by heinold on 18.06.15.
 */
@groovy.transform.CompileStatic
public abstract class ConfigurationConverter {
    public static final LoggerWrapper logger = LoggerWrapper.getLogger(ConfigurationConverter.class.getSimpleName());

    public abstract String convert(ExecutionContext context, Configuration configuration);

    public static String convertAutomatically(ExecutionContext context, Configuration configuration) {
        return FileSystemAccessProvider.getInstance().getConfigurationConverter().convert(context, configuration);
    }

    /**
     * Converts a string to a proper variable name i.e.
     * alignSampeSort will be TOOL_ALIGN_SAMPE_SORT
     * replaces colon with underscore.
     * @param prefix
     * @param str
     * @param delimiter
     * @return
     */
    public static String createVariableName(String prefix = ConfigurationConstants.CVALUE_PREFIX_TOOL, String str, String delimiter = "_") {
        String res = prefix + str.split("(?=\\p{Upper})").join(delimiter).toUpperCase()
        res = res.replace(COLON, UNDERSCORE);
        return res;
    }

    /**
     * Tries to convert back a converted variable name to its original form.
     * Does not convert back colon though!
     *
     * TOOL_ALIGN_SAMPE_SORT will be alignSampeSort
     * @param str
     * @param delimiter
     * @return
     */
    public static String convertBackVariableName(String str, String delimiter = SPLIT_UNDERSCORE) {
        try {
            String oName = EMPTY;
            String[] split = str.split(delimiter);
            for (int i = 1; i < split.length; i++) {
                oName += split[i][0].toUpperCase() + split[i][1..-1].toLowerCase();
            }
            oName = oName[0].toLowerCase() + oName[1..-1];
            return oName;
        } catch (Exception ex) {
            println("Could not convert value ${str} to variable name.");
            return "UNKNOWN_${str}";
        }
    }

    public abstract StringBuilder convertConfigurationValue(ConfigurationValue cv, ExecutionContext context);

    /**
     * Converts "easy" files to xml configs.
     * @param text
     * @return
     */
    public abstract String convertToXML(File file);
}
