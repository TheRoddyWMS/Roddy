/*
 * Copyright (c) 2017 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.plugins

import de.dkfz.roddy.StringConstants
import groovy.transform.CompileStatic

/**
 * Contains basic information about a plugin folder.
 * Created by heinold on 29.04.17.
 */
@CompileStatic
class PluginDirectoryInfo {

    final File directory

    final String pluginID

    final String version

    final PluginType type

    PluginDirectoryInfo(File file, PluginType pluginType) {
        String[] splitName = file.name.split(StringConstants.SPLIT_UNDERSCORE) // Split name and version
        directory = file
        pluginID = splitName[0]
        version = splitName.size() > 1 ? splitName[1] : LibrariesFactory.PLUGIN_VERSION_DEVELOP
        type = pluginType
    }

    boolean needs

    boolean needsBuildInfoFile() {
        return type.needsBuildInfoFile
    }
}
