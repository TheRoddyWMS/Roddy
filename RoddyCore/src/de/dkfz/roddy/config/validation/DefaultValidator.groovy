/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.validation

import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationIssue
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.config.EnumerationValue
import groovy.transform.CompileStatic

import static de.dkfz.roddy.config.ConfigurationConstants.*
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

        if (configurationValue.value == null) return true

        boolean result = isConfigurationValueTypeCorrect(configurationValue, ev)
        result &= areDollarCharactersAttached(configurationValue)
        result &= areVariablesProperlyDefined(configurationValue)
        this.result = result
        return result
    }

    private boolean isConfigurationValueTypeCorrect(ConfigurationValue cv, EnumerationValue ev) {
        String evID = ev != null ? ev.getId() : CVALUE_TYPE_STRING
        try {
            if (evID == CVALUE_TYPE_INTEGER) {
                cv.toInt()
            } else if (evID == CVALUE_TYPE_BOOLEAN) {
                cv.toBoolean()
            } else if (evID == CVALUE_TYPE_FLOAT) {
                cv.toFloat()
            } else if (evID == CVALUE_TYPE_DOUBLE) {
                cv.toDouble()
            } else if (evID == CVALUE_TYPE_STRING) {
            }
        } catch (Exception e) {
            super.errors << new ConfigurationIssue(valueAndTypeMismatch, cv.id, cv?.configuration?.file?.toString(), evID)
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

    /** Match either a detached dollar (without escape and brace) somewhere in the text
     *   OR a non escaped detached dollar at line end.
     *   OR a detached dollar string '$'.
     *   OR a detached dollar at the beginning of the line.
     */
    static String matchDetachedDollars = /([^\\][$][^{])|([^\\][$]$)|(^[$]$)|(^[$][^{])/

    boolean areDollarCharactersAttached(ConfigurationValue cv) {
        if (!cv.value.contains('$')) return true
        if (cv.value == '${$}') return true   // $$ is the current process ID variable, which is fine as value.

        String temp = removeEscapedEscapeCharacters(cv.value)

        boolean result = !temp.findAll(matchDetachedDollars)

        if (!result) {
            String fileName = cv?.configuration?.file?.toString()
            if (fileName == null)
                fileName = "applicationProperties.ini or commandline"
            super.warnings << new ConfigurationIssue(detachedDollarCharacter, cv?.id, fileName)
        }

        return result
    }

    static final String VARIABLE = '(^|(?<=[^\\\\{]))[$][{]([\\w[-]_]){1,}[}]'
    static final String VARIABLE_START = '([^\\\\|^{][$][{])|(^[$][{])'

    boolean areVariablesProperlyDefined(ConfigurationValue cv) {
        if (!cv.value.contains('$')) return true
        if (cv.value == '${$}') return true   // $$ is the current process ID variable, which is fine as value.

        String temp = removeEscapedEscapeCharacters(cv.value)

        int countOfVariables = temp.findAll(VARIABLE).size()
        int countOfStartedVariables = temp.findAll(VARIABLE_START).size()

        boolean result = countOfVariables == countOfStartedVariables

        if (!result) {
            super.warnings << new ConfigurationIssue(malformedVariableExpression, cv.id, cv?.configuration?.file?.toString())
        }

        return result
    }
}
