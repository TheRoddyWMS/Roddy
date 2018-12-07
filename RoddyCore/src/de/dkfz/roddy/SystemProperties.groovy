/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */
package de.dkfz.roddy

import groovy.transform.CompileStatic

/**
 * Facade for different system properties to reduce code duplication and typos.
 */
@CompileStatic
class SystemProperties {
    static String getUserName() {
        return System.properties["user.name"]
    }

    static String getUserHome() {
        return System.properties["user.home"]
    }

    static String getUserDir() {
        return System.properties["user.dir"]
    }

    static String getLineSeparator() {
        return System.properties["line.separator"]
    }

    static String getJavaVersion() {
        return System.properties["java.version"]
    }

    static String getJavaClasspath() {
        return System.properties["java.class.path"]
    }
}
