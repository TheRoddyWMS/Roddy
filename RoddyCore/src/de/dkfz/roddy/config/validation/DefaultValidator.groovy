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

        boolean result = checkDataTypes(configurationValue, ev)
        result &= checkDollarSignUsage(configurationValue)

        this.result = result
        return result
    }

    private boolean checkDataTypes(ConfigurationValue configurationValue, EnumerationValue ev) {
        String evID = ev != null ? ev.getId() : "string"
        try {
            if (evID.equals("integer")) {
                configurationValue.toInt()
            } else if (evID.equals("boolean")) {
                configurationValue.toBoolean()
            } else if (evID.equals("float")) {
                configurationValue.toFloat()
            } else if (evID.equals("double")) {
                configurationValue.toDouble()
            } else if (evID.equals("string")) {
            }
        } catch (Exception e) {
            super.errors << new ConfigurationIssue(valueAndTypeMismatch, configurationValue.id, evID)
            return false
        }
        return true
    }

    /**
     * API Level 3.4+
     */
    boolean checkDollarSignUsage(ConfigurationValue configurationValue) {

        def TEXT = '(([^${}]){0,}|(\\$){0,}){0,}'
        def VARIABLE = '([$][{](.){0,}[}]){0,}'
        def matcher = /${TEXT}${VARIABLE}${TEXT}/

        String val = configurationValue.value
        boolean result = val ==~ matcher

        if (!result) {
            super.warnings << new ConfigurationIssue(unattachedDollarCharacter, configurationValue.id)
        }

        return result
    }
}
