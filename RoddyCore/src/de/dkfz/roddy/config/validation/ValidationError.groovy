/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.validation

import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationError

/**
 * Errors thrown during validation
 */
class ValidationError extends ConfigurationError {

    ValidationError(Configuration configuration, String id, String description, Exception exception) {
        super(description, configuration, id, exception)

    }

    @Override
    String toString() {
        String id = "" + id
        String description = "" + description
        String exceptionMsg = "" + (exception != null ? exception.getMessage() : null)
        return String.format("VE: %s: %s, %s", id, description, exceptionMsg)
    }
}
