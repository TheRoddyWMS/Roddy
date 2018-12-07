/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.validation

import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationIssue
import de.dkfz.roddy.config.ConfigurationValue
import groovy.transform.CompileStatic

/**
 * Base class for configuration value validation.
 * Extend from this class to create custom validators.
 */
@CompileStatic
abstract class ConfigurationValueValidator extends ConfigurationValidator implements ConfigurationIssue.IConfigurationIssueContainer {

    protected final List<ConfigurationIssue> warnings = []

    protected final List<ConfigurationIssue> errors = []

    ConfigurationValueValidator(Configuration cfg) {
        super(cfg)
    }

    @Override
    boolean validate() {
        return true
    }

    /**
     * Validate a configuration value
     * @param configurationValue The configuration value which should be validated
     * @return true if it was validated
     */
    abstract boolean validate(ConfigurationValue configurationValue);


    @Override
    final boolean hasWarnings() {
        warnings
    }

    @Override
    final void addWarning(ConfigurationIssue warning) {
        if (warning) warnings << warning
    }

    @Override
    final List<ConfigurationIssue> getWarnings() {
        return new LinkedList<>(warnings)
    }

    @Override
    boolean hasErrors() {
        errors
    }

    @Override
    final void addError(ConfigurationIssue error) {
        if (error) errors << error
    }

    @Override
    final List<ConfigurationIssue> getErrors() {
        return new LinkedList<>(errors)
    }
}
