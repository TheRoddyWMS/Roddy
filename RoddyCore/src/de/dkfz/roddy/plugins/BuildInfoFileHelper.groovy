/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

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
    private final String pluginVersion
    private final boolean hasBuildInfoEntries

    public static String[] validEntries = [
            LibrariesFactory.BUILDINFO_RUNTIME_APIVERSION, // Full reference, otherwise Groovy makes a String out of the entry and does not take the constants content
            LibrariesFactory.BUILDINFO_RUNTIME_GROOVYVERSION,
            LibrariesFactory.BUILDINFO_RUNTIME_JDKVERSION,
            LibrariesFactory.BUILDINFO_DEPENDENCY,
            LibrariesFactory.BUILDINFO_STATUS_BETA,
            LibrariesFactory.BUILDINFO_COMPATIBILITY,
    ]

    /** This constructor is mainly for testing **/
    BuildInfoFileHelper(String pluginName, String pluginVersion, List<String> lines) {
        this.pluginName = pluginName
        this.pluginVersion = pluginVersion;
        def invalid = []
        if (!lines) lines = []
        for (String _line in lines) {
            _line = _line.trim();
            if(!_line) continue;
            if(_line.startsWith("#")) continue;

            String[] line = _line.split(StringConstants.SPLIT_EQUALS);
            if (!validEntries.contains(line[0])) {
                invalid << line[0]
            } else {
                entries.get(line[0], []) << line[1];
            }
        }

        hasBuildInfoEntries = true

        if (invalid)
            logger.postAlwaysInfo("There are invalid entries in file buildinfo.txt for plugin ${pluginName}:\n  " + invalid.join("\n "));
    }

    /** This constructor is the "real" constructor **/
    BuildInfoFileHelper(String pluginName, String pluginVersion, File buildinfoFile) {
        this(pluginName, pluginVersion, buildinfoFile.readLines())
    }

    BuildInfoFileHelper(String pluginname, String pluginVersion) {
        this.pluginName = pluginname
        this.pluginVersion = pluginVersion
        hasBuildInfoEntries = false
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

        if(previousPlugin == null) return false; // The value is not set

        def _entries = entries.get(BUILDINFO_COMPATIBILITY, [])
        if(!_entries) return false; // There is no entry in the buildinfo.txt file.

        String entry = _entries[0] as String;
        if (!(entry ==~ ~/.*-\d+$/)) //Ends with something like -123 or so... if not append it.
            entry += "-0";

        if (!isVersionStringValid(entry)) {
            logger.postSometimesInfo("Version string ${entry} is invalid.")
            return false;
        }
        if (previousPlugin?.getProdVersion() != entry) {
            logger.postSometimesInfo("The compatibility entry ${entry} points to a not existing version for ${pluginName}, the last known entry is ${previousPlugin?.getProdVersion()}")
            return false;
        }
        if(!checkMatchingAPIVersions(previousPlugin)) {
            logger.postSometimesInfo("The plugin version ${pluginVersion} for ${pluginName} is incompatible to ${previousPlugin?.getProdVersion()}, because of mismatching API versions.")
            return false;
        }
        return true;
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