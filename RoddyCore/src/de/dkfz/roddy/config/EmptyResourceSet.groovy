/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config

import de.dkfz.roddy.config.ResourceSet
import de.dkfz.roddy.config.ResourceSetSize
import de.dkfz.roddy.tools.BufferValue
import de.dkfz.roddy.tools.TimeUnit
import groovy.transform.CompileStatic

import java.time.Duration

/**
 * Created by heinold on 25.04.17.
 */
@CompileStatic
class EmptyResourceSet extends ResourceSet{
    EmptyResourceSet() {
        super(null, null, null, null, null as Duration, null, null, null)
    }
}
