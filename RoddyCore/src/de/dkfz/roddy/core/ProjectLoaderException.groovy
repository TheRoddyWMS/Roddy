/*
 * Copyright (c) 2017 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core

import groovy.transform.CompileStatic

/**
 * A custom exception class for ProjectLoader based exceptions
 * Created by heinold on 11.04.17.
 */
@CompileStatic
class ProjectLoaderException extends Exception {
    ProjectLoaderException(String message) {
        super(message)
    }
}
