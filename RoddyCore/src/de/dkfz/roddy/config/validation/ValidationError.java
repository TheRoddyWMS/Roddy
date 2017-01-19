/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.validation;

import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.ConfigurationError;

/**
 * Errors thrown during validation
 */
public class ValidationError extends ConfigurationError {

    public ValidationError(Configuration configuration, String id, String description, Exception exception) {
        super(description, configuration, id, exception);

    }

    @Override
    public String toString() {
        String id = "" + this.id;
        String description = "" + this.description;
        String exceptionMsg = "" + (this.exception != null ? exception.getMessage() : null);
        return String.format("VE: %s: %s, %s", id, description, exceptionMsg);
    }
}
