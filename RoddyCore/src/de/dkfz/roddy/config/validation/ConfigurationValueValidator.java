/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.validation;

import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.ConfigurationValue;

/**
 * Base class for configuration value validation.
 * Extend from this class to create custom validators.
 */
public abstract class ConfigurationValueValidator extends ConfigurationValidator {
    public ConfigurationValueValidator(Configuration cfg) {
        super(cfg);
    }

    @Override
    public boolean validate() {
        return true;
    }

    /**
     * Validate a configuration value
     * @param configurationValue The configuration value which should be validated
     * @return true if it was validated
     */
    public abstract boolean validate(ConfigurationValue configurationValue);
}
