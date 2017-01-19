/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.plugins;

/**
 * The base class for all Roddy Plugins.
 */
public abstract class BasePlugin {

    public static final String CURRENT_VERSION_STRING = "1.0.28";
    public static final String CURRENT_VERSION_BUILD_DATE = "Mon Mar 07 10:58:54 CET 2016";

    public abstract String getVersionInfo();
}
