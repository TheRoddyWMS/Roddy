/*
 * Copyright (c) 2017 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
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
    INVALID(false),

    /**
     * A regular Roddy workflow with or without a Jar file
     */
    RODDY(true),

    /**
     * Native workflows like e.g. Bash
     */
    NATIVE(false),

    /**
     * A snakemake based workflow
     */
    SNAKEMAKE(false);

    final boolean needsBuildInfoFile

    PluginType(boolean needsBuildInfoFile) {
        this.needsBuildInfoFile = needsBuildInfoFile
    }
}