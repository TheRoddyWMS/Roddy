package de.dkfz.roddy.plugins;

/**
 * The base class for all Roddy Plugins.
 */
public abstract class BasePlugin {

    public static final String CURRENT_VERSION_STRING = "1.0.27";
    public static final String CURRENT_VERSION_BUILD_DATE = "Tue Jan 19 10:49:51 CET 2016";

    public abstract String getVersionInfo();
}
