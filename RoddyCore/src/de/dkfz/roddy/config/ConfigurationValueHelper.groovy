/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config

import java.util.regex.Matcher

/**
 * Groovy helper class for conviguration value objects.
 * Not all things in configuration value can be converted to groovy
 * without a load of work, so it is easier to write this helper class
 * which just provides some basic groovy functionality for the java
 * part.
 */
@groovy.transform.CompileStatic
class ConfigurationValueHelper {

    public static List<String> callFindAllForPatternMatcher(Matcher m) {
        return new LinkedList<String>(m.findAll());
    }

}
