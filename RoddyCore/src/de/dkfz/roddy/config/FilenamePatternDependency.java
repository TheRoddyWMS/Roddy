/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config;

/**
 * Created by heinold on 15.01.16.
 */
public enum FilenamePatternDependency {
    derivedFrom,
    FileStage,
    onMethod,
    onScriptParameter,
    onTool;
}
