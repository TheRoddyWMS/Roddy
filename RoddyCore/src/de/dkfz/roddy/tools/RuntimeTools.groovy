package de.dkfz.roddy.tools;

import de.dkfz.roddy.Constants
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.StringConstants;
import groovy.lang.GroovySystem
import org.apache.commons.io.filefilter.WildcardFileFilter;

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
        File f = new File(Roddy.getRoddyBinaryFolder(), "lib").listFiles((FilenameFilter)new WildcardFileFilter("groovy*.jar"))[0]
        String version = f.name.split(StringConstants.SPLIT_MINUS)[2].split("[.]")[0..1].join(".")
        return version;
    }
}
