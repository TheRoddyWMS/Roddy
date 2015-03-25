package de.dkfz.roddy.plugins;

/**
 * The base class for all Roddy Plugins.
 */
public abstract class BasePlugin {

    public static final String CURRENT_VERSION_STRING = "1.0.25";
    public static final String CURRENT_VERSION_BUILD_DATE = "Wed Mar 25 14:49:39 CET 2015";

    public abstract String getVersionInfo();
}
