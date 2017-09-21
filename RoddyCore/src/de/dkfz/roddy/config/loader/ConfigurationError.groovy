/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.loader

import de.dkfz.roddy.config.Configuration
import groovy.transform.CompileStatic

/**
 */
@CompileStatic
class ConfigurationError extends Exception {

    Configuration configuration
    String id
    String description
    Exception exception

    ConfigurationError(String description, Configuration configuration, String id, Exception exception) {
        super(description, exception)
        this.description = description
        this.configuration = configuration
        this.id = id
        this.exception = exception
    }

    ConfigurationError(String description, Configuration configuration) {
        this(description, configuration, null, null)
    }
}
