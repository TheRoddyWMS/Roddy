/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.loader

import groovy.transform.CompileStatic

/**
 * Created by heinold on 02.05.17.
 */
@CompileStatic
class ConfigurationLoaderException extends Throwable {
    ConfigurationLoaderException(String msg) {
        super(msg)
    }

    ConfigurationLoaderException(String msg, Throwable cause) {
        super(msg, cause)
    }
}
