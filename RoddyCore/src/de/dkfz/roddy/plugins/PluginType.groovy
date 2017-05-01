/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.plugins

import groovy.transform.CompileStatic

/**
 * A list of different possible types of plugin folders
 * Created by heinold on 29.04.17.
 */
@CompileStatic
enum PluginType {
    /**
     * Invalid folders
     */
    INVALID,

    /**
     * A regular Roddy workflow with or without a Jar file
     */
    RODDY,

    /**
     * Native workflows like e.g. Bash
     */
    NATIVE,

    /**
     * A snakemake based workflow
     */
    SNAKEMAKE,
}