package de.dkfz.roddy.tools;

import de.dkfz.roddy.Constants;
import groovy.lang.GroovySystem;

/**
 * A class which gives you access to several runtime specific values (e.g. from System.getProperty...)
 * Created by heinold on 29.02.16.
 */
public final class RuntimeTools {
    private RuntimeTools() {}

    public static String getRoddyRuntimeVersion() {
        String roddyRuntimeVersion = Constants.APP_CURRENT_VERSION_STRING.split("[.]")[0..1].join(".");
        roddyRuntimeVersion
    }

    public static String getJavaRuntimeVersion() {
        String javaRuntimeVersion = System.getProperty("java.version").split("[.]")[0..1].join(".")
        javaRuntimeVersion
    }

    public static String getGroovyRuntimeVersion() {
        String groovyRuntimeVersion = GroovySystem.version.split("[.]")[0..1].join(".")
        groovyRuntimeVersion
    }
}
