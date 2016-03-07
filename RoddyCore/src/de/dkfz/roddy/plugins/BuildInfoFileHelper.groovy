package de.dkfz.roddy.plugins

import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.tools.LoggerWrapper
import static de.dkfz.roddy.plugins.LibrariesFactory.*

/**
 * Created by heinold on 03.03.16.
 */
@groovy.transform.CompileStatic
public class BuildInfoFileHelper {
    private static LoggerWrapper logger = LoggerWrapper.getLogger(LibrariesFactory.class.getSimpleName());

    public static final String DEFAULT_GROOVY_VERSION = "2.3" // Groovy was 2.3.x for a long time.
    public static final String DEFAULT_JDK_VERSION = "1.8" // For backward compatibility, the api versions are set for older plugins
    public static final String DEFAULT_RODDY_VERSION = "2.2"

    private Map<String, List<String>> entries = [:];
    private final String pluginName

    public static String[] validEntries = [
            LibrariesFactory.BUILDINFO_RUNTIME_APIVERSION, // Full reference, otherwise Groovy makes a String out of the entry and does not take the constants content
            LibrariesFactory.BUILDINFO_RUNTIME_GROOVYVERSION,
            LibrariesFactory.BUILDINFO_RUNTIME_JDKVERSION,
            LibrariesFactory.BUILDINFO_DEPENDENCY,
            LibrariesFactory.BUILDINFO_STATUS_BETA,
            LibrariesFactory.BUILDINFO_COMPATIBILITY,
    ]

    /** This constructor is mainly for testing **/
    BuildInfoFileHelper(String pluginName, List<String> lines) {
        this.pluginName = pluginName
        def invalid = []
        if (!lines) lines = []
        for (String _line in lines) {
            String[] line = _line.split(StringConstants.SPLIT_EQUALS);
            if (!validEntries.contains(line[0]))
                invalid << line[0]
            else
                entries.get(line[0], []) << line[1];
        }

        if (invalid)
            logger.postSometimesInfo("There are invalid entries in file buildinfo.txt for plugin ${pluginName}:\n  " + invalid.join("\n "));

//            assert entries[BUILDINFO_RUNTIME_GROOVYVERSION].findIn
    }

    /** This constructor is the "real" constructor **/
    BuildInfoFileHelper(String pluginName, File buildinfoFile) {
        this(pluginName, buildinfoFile.readLines())
    }

    Map<String, List<String>> getEntries() {
        return entries
    }

    public Map<String, String> getDependencies() {
        Map<String, String> dependencies = [:]
        for (String entry in entries.get(BUILDINFO_DEPENDENCY, [])) {
            if (!LibrariesFactory.isPluginIdentifierValid(entry)) continue
            List<String> split = entry?.split(StringConstants.SPLIT_COLON) as List;
            String workflow = split[0];
            String version = split.size() > 1 ? split[1] : PLUGIN_VERSION_CURRENT;
            dependencies[workflow] = version;
        }
        return dependencies;
    }

    public boolean isCompatibleTo(PluginInfo previousPlugin) {
        // Not revision but compatible. Check, if the former plugin id (excluding the revision number) is
        // set as compatible.
        // Ignore malformed entries!! Use a regex for that.
        boolean isCompatible = false;
        String entry = entries.get(BUILDINFO_COMPATIBILITY, [])[0] as String;
        if (!(entry ==~ ~/.*-\d+$/)) //Ends with something like -123 or so... if not append it.
            entry += "-0";

        if (!isVersionStringValid(entry)) {
            logger.postAlwaysInfo("Version string ${entry} is invalid.")
        } else if (previousPlugin?.getProdVersion() == entry &&
                checkMatchingAPIVersions(previousPlugin)) {
            isCompatible = true;
        } else
            logger.info("Could not find entry for compatibility ${pluginName}:${entry} or ");
        return isCompatible;
    }

    boolean checkMatchingAPIVersions(PluginInfo pluginInfo) {
        return pluginInfo.getJdkVersion() == getJDKVersion() &&
                pluginInfo.getGroovyVersion() == getGroovyVersion() &&
                pluginInfo.getRoddyAPIVersion() == getRoddyAPIVersion();
    }

    public String getJDKVersion() {
        return entries.get(BUILDINFO_RUNTIME_JDKVERSION, [DEFAULT_JDK_VERSION])[0].split(StringConstants.SPLIT_STOP)[0..1].join(".");
    }

    public String getGroovyVersion() {
        return entries.get(BUILDINFO_RUNTIME_GROOVYVERSION, [DEFAULT_GROOVY_VERSION])[0].split(StringConstants.SPLIT_STOP)[0..1].join(".");
    }

    public String getRoddyAPIVersion() {
        return entries.get(BUILDINFO_RUNTIME_APIVERSION, [DEFAULT_RODDY_VERSION])[0].split(StringConstants.SPLIT_STOP)[0..1].join(".");
    }

    public boolean isBetaPlugin() {
        return entries.get(BUILDINFO_STATUS_BETA, ["false"])[0].toBoolean()
    }
}