/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.plugins

import groovy.transform.CompileStatic

/**
 * A default Java based Roddy plugin
 * Created by heinold on 04.05.17.
 */
@CompileStatic
class JarFulPluginInfo extends PluginInfo {

    final File jarFile

    JarFulPluginInfo(String name, File directory, File jarFile, String version, String roddyAPIVersion, String jdkVersion, Map<String, String> dependencies) {
        super(name, directory, version, roddyAPIVersion, jdkVersion, dependencies)
        this.jarFile = jarFile
    }
}
