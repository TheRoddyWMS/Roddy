/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.tools;

import de.dkfz.roddy.StringConstants
import org.apache.commons.io.filefilter.WildcardFileFilter

import java.lang.reflect.Field;

/**
 * A class which gives you access to several runtime specific values (e.g. from System.getProperty...)
 * Created by heinold on 29.02.16.
 */
@groovy.transform.CompileStatic
public final class RuntimeTools {
    private RuntimeTools() {}

    public static String getRoddyRuntimeVersion() {
        // Get from buildinfo file. If this is not available... don't know... take 2.2. then.
        def lines = getBuildinfoFile().readLines()
        if (lines) return lines.find { String line -> line.startsWith("Roddy") }.split(StringConstants.SPLIT_EQUALS)[1]
        return "2.2";
    }

    public static String getJavaRuntimeVersion() {
        String javaRuntimeVersion = System.getProperty("java.version").split("[.]")[0..1].join(".")
        javaRuntimeVersion
    }

    /**
     * Extract the groovy library version from the current class path
     * @return
     */
    public static String getGroovyRuntimeVersion() {
        File groovyLibrary = getGroovyLibrary()
        String version = groovyLibrary.name.split(StringConstants.SPLIT_MINUS)[2].split("[.]")[0..1].join(".")
        return version;
    }

    public static File getBuildinfoFile() {
        return new File(getCurrentDistFolder(), "buildinfo.txt");
    }

    public static File getCurrentDistFolder() {
        File f = getGroovyLibrary()
        f.getParentFile().getParentFile()
    }

    public static File getGroovyLibrary() {
        return new File(System.getProperty("java.class.path").split("[:]").find { new File(it).name.startsWith("groovy") })
    }
}
