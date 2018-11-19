/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.validation

import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationIssue
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.config.EnumerationValue
import groovy.transform.CompileStatic

import static de.dkfz.roddy.config.ConfigurationIssue.ConfigurationIssueTemplate.*

/**
 * This validator check values of type
 * string (not really checked)
 * integer
 * boolean
 */
@CompileStatic
class DefaultValidator extends ConfigurationValueValidator {
    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(DefaultValidator.class.getSimpleName())

    boolean result = false

    DefaultValidator(Configuration cfg) {
        super(cfg)
    }

    @Override
    boolean validate(ConfigurationValue configurationValue) {
        super.errors.clear()
        super.warnings.clear()
        EnumerationValue ev = configurationValue.getEnumerationValueType()

        boolean result = isConfigurationValueTypeCorrect(configurationValue, ev)
        result &= areDollarCharactersProperlyUsed(configurationValue)
        result &= areVariablesProperlyDefined(configurationValue)
        this.result = result
        return result
    }

    private boolean isConfigurationValueTypeCorrect(ConfigurationValue configurationValue, EnumerationValue ev) {
        String evID = ev != null ? ev.getId() : "string"
        try {
            if (evID == "integer") {
                configurationValue.toInt()
            } else if (evID == "boolean") {
                configurationValue.toBoolean()
            } else if (evID == "float") {
                configurationValue.toFloat()
            } else if (evID == "double") {
                configurationValue.toDouble()
            } else if (evID == "string") {
            }
        } catch (Exception e) {
            super.errors << new ConfigurationIssue(valueAndTypeMismatch, configurationValue.id, evID)
            return false
        }
        return true
    }

    /**
     * Trick: Remove escaped escape characters! This way only single escape characters will be used for the check.
     */
    static String removeEscapedEscapeCharacters(String temp) {
        // Why the loop? Because replaceAll won't do it and will totally replace e.g. odd escape sequence, e.g.:
        // \\abc => abc     and
        // \\\abc => abc    as well!
        while (temp.contains("\\\\"))
            temp = temp.replace("\\\\", "")
        temp
    }

    /** Match either a detached dollar (withouth esacpe and brace) somewhere in the text
     *   OR a non escaped detached dollar at line end.
     *   OR a detached dollar string '$'
     */
    static String matchDetachedDollars = /([^\\][$][^{])|([^\\][$]$)|(^[$]$)/

    boolean areDollarCharactersProperlyUsed(ConfigurationValue cv) {
        if (!cv.value.contains('$')) return true

        String temp = removeEscapedEscapeCharacters(cv.value)

        boolean result = !temp.findAll(matchDetachedDollars)

        if (!result) {
            super.warnings << new ConfigurationIssue(detachedDollarCharacter, cv.id)
        }

        return result
    }

    static final String VARIABLE = '([^\\\\{][$][{]([\\w[-]_]){1,}[}])|(^[$][{]([\\w[-]_]){1,}[}])'
    static final String VARIABLE_START = '([^\\\\|^{][$][{])|(^[$][{])'

    boolean areVariablesProperlyDefined(ConfigurationValue cv) {
        if (!cv.value.contains('$')) return true

        String temp = removeEscapedEscapeCharacters(cv.value)

        int countOfVariables = temp.findAll(VARIABLE).size()
        int countOfStartedVariables = temp.findAll(VARIABLE_START).size()

        boolean result = countOfVariables == countOfStartedVariables

        if (!result) {
            super.warnings << new ConfigurationIssue(inproperVariableExpression, cv.id)
        }

        return result
    }
}
