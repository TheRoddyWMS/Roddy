/*
 * Copyright (c) 2017 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.plugins

import de.dkfz.roddy.Constants
import groovy.transform.CompileStatic

/**
 * Represents standard Roddy plugins without a Jar file.
 * Created by heinold on 04.05.17.
 */
@CompileStatic
class JarLessPluginInfo extends PluginInfo{

    JarLessPluginInfo(String name, File directory, String version, Map<String, String> dependencies) {
        super(name, directory, version, Constants.UNKNOWN, null, dependencies)
    }

    @Override
    boolean isCompatibleToRuntimeSystem() {
        return true
    }
}
