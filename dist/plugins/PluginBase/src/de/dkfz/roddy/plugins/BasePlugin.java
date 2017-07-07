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

    public static final String CURRENT_VERSION_STRING = "1.0.29";
    public static final String CURRENT_VERSION_BUILD_DATE = "Thu Jul 06 13:10:24 CEST 2017";

    public abstract String getVersionInfo();
}
