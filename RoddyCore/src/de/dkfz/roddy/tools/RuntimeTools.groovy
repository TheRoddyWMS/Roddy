/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.tools

import de.dkfz.roddy.Constants
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.tools.versions.Version

/**
 * A class which gives you access to several runtime specific values (e.g. from System.getProperty...)
 * Created by heinold on 29.02.16.
 */
@groovy.transform.CompileStatic
final class RuntimeTools {
    private RuntimeTools() {}

    private static final LoggerWrapper logger = LoggerWrapper.getLogger(RuntimeTools)

    static String getRoddyRuntimeVersion() {
        return Version.fromString(Constants.APP_CURRENT_VERSION_STRING).toString(Version.VersionLevel.MINOR)
    }

    static String getJavaRuntimeVersion() {
        String javaRuntimeVersion = System.getProperty("java.version").split("[.]")[0..1].join(".")
        javaRuntimeVersion
    }

    static File getBuildinfoFile() {
        return new File(getCurrentDistFolder(), LibrariesFactory.BUILDINFO_TEXTFILE);
    }

    static File getCurrentDistFolder() {
        return new File(System.getProperty("user.dir"), "dist/bin/current")
    }

}
