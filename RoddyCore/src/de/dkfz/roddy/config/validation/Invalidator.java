/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.validation;

import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.ConfigurationValue;

import java.util.logging.Logger;

/**
 * This validator invalidates everything. It is used if a validator class could not be loaded.
 */
public class Invalidator extends ConfigurationValueValidator {
    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(Invalidator.class.getSimpleName());

    public Invalidator(Configuration cfg) {
        super(cfg);
    }

    @Override
    public boolean validate(ConfigurationValue configurationValue) {
        return false;
    }
}
