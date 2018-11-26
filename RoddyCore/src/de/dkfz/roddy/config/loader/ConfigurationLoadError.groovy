/*
 * Copyright (c) 2017 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.loader

import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationError
import groovy.transform.CompileStatic

/**
 * Errors which occur during configuration load are represented by this class.
 */
@CompileStatic
class ConfigurationLoadError extends ConfigurationError {

    ConfigurationLoadError(Configuration configuration, String id, String description, Exception exception) {
        super(description, configuration, id, exception)
    }


    @Override
    String toString() {
        return "${id.padRight(20)}: ${description}";
    }
}
