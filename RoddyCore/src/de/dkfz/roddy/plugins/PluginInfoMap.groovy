/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.plugins

import de.dkfz.roddy.StringConstants

/**
 * A container / map for loaded / available plugins found by e.g. librariesFactory.loadMapOfAvailablePluginsForInstance();
 *
 * Created by heinold on 27.06.16.
 */
@groovy.transform.CompileStatic
class PluginInfoMap {

    /**
     * A map stuffed with (detected) PluginInfo object.
     * Keys are:
     * mapOfPlugins[plugin id][plugin version]
     *
     * Contents are e.g.:
     * [ "BasePlugin" : [ "1.0.1", "1.0.2", "current" ]
     */
    Map<String, Map<String, PluginInfo>> mapOfPlugins

    PluginInfoMap(Map<String, Map<String, PluginInfo>> mapOfPlugins) {
        this.mapOfPlugins = mapOfPlugins
        if (!mapOfPlugins) this.mapOfPlugins = [:] // By default, create an empty map.
    }

    /**
     * Returns the internal object of the map, if it is available. It is not a copy!
     * Access the map with the plugin id.
     * @param element
     * @return
     */
    public Map<String, PluginInfo> getAt(String element) {
        return mapOfPlugins[element];
    }

    /**
     * calls asBoolean of mapOfPlugins / LinkedHashMap / Map
     * @return
     */
    public boolean asBoolean() {
        return mapOfPlugins != null && mapOfPlugins.size() > 0;
    }

    /**
     * Get an object based on a plugin string like:
     * BasePlugin:1.0.12 or so...
     *
     * If no plugin is set "current" will be returned.
     */
    public PluginInfo getPluginInfoWithPluginString(String pluginString) {
        String[] split = pluginString.split(StringConstants.SPLIT_COLON);
        if (split.size() > 2)
            throw new RuntimeException("The plugin string ${pluginString} is malformed.")
        String pluginID = split[0];
        String pluginVersion = split.size() > 1 ? split[1] : "current";
        return getPluginInfo(pluginID, pluginVersion);
    }

    /**
     * Get a plugin object with id and version
     * @param pluginID
     * @param version
     * @return
     */
    public PluginInfo getPluginInfo(String pluginID, String version) {
        if (!version) version = "current";
        if (!mapOfPlugins[pluginID]) //Can this case occur?
            throw new RuntimeException("Plugin ${pluginID} is not available, available are:\n\t" + mapOfPlugins.keySet().join("\t\n"))
        if (!mapOfPlugins[pluginID][version])
            throw new RuntimeException("Version ${version} of plugin ${pluginID} is not available, know versions are:\n\t" + mapOfPlugins[pluginID].keySet().join("\t\n"))
        return mapOfPlugins[pluginID][version]
    }

    public boolean checkExistence(String pluginID, String version) {
        if (!version) version = "current";
        if (!mapOfPlugins[pluginID]) //Can this case occur?
            return false;
        if (!mapOfPlugins[pluginID][version])
            return false;
        return true;
    }

    /**
     * Number of entries in the internal map
     * @return
     */
    public int size() {
        return mapOfPlugins.size()
    }
}
