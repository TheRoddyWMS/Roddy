/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config

import de.dkfz.roddy.config.loader.ConfigurationLoadError
import groovy.transform.CompileStatic

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Helper class (or service) for configuration value objects.
 */
@CompileStatic
class ConfigurationValueHelper {

    /**
     * Pattern for matching configuration value references in configuration values.
     *
     * TODO Change this to '[$][{][a-zA-Z_][a-zA-Z_0-9]*[}]'. This changing is compatibility-breaking.
     * */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile('[$][{][a-zA-Z0-9_]*[}]')

    /**
     * Note this is public.
     *
     * @param value   String to search in.
     * @return        List of variable names in variable reference patterns
     */
    static List<String> getContainedKeys(String value) {
        List<String> containedKeys = [] as LinkedList

        Matcher m = VARIABLE_PATTERN.matcher(value)
        // Findall is not available in standard java, so I use groovy here.
        for (String s : m.findAll()) {
            String vName = s.replaceAll('[\${}]', '')
            containedKeys.add(vName)
        }

        return containedKeys
    }

    /** Evaluate a String with an initial value and associated with a key (valueID) that contains value references of
     *  the form '${someKey}'. The actual values to be used as replacements for '${someKey}' are taken from a
     *  Configuration object.
     *
     *  Essentially this implements a templating mechanism that takes its values from a Configuration. The
     *  configuration represents a tree and `evaluateValue` is called recursively on the configuration values in the
     *  tree to fill in all non-blacklisted variable references. The evaluation follows the normal rules for value
     *  evaluation in configuration trees, with configurations of different priorities (e.g. CLI configuration has
     *  highest priority).
     *
     *  Note that the loading errors of the Configuration may get updated AND a ConfigurationError may get thrown, if a
     *  valueID that is directly queried or that occurs during the recursive evaluation is in the blacklist or if there
     *  is a cyclic dependency.
     *
     * @param key             key of the value itself
     * @param value           the current value itself in which the replacements should take place
     * @param configuration   the background environment from which to take the values to be templated in.
     * @return
     */
    static String evaluateValue(String key, String value, Configuration configuration) {
        evaluateValueImpl(key, value, configuration, [])
    }

    /**
     *  Do the actual evaluation of keys. The first parameter is a blacklist of keys that have already been
     *  evaluated before. If a key re-occurs this means there is a cyclic dependency.
     *
     * @param blackList       values not to be replaced
     */
    private static String evaluateValueImpl(String valueID,
                                            String initialValue,
                                            Configuration configuration,
                                            List<String> blackList) {
        String result = initialValue
        if (configuration != null) {
            List<String> containedKeys = getContainedKeys(result)
            if (blackList.intersect(containedKeys as Iterable<String>)) {
                def badConf =
                        configuration.preloadedConfiguration != null ?
                                configuration.preloadedConfiguration.file :
                                configuration.ID
                ConfigurationError exc =
                        new ConfigurationError("Cyclic dependency found for cvalue '${valueID}' in file '${badConf}'",
                                               configuration, 'Cyclic dependency', null)
                configuration.addLoadError(new ConfigurationLoadError(configuration, 'cValues', exc.message, exc))
                throw exc
            }
            def configurationValues = configuration.configurationValues
            for (String vName : containedKeys) {
                if (configurationValues.hasValue(vName)) {
                    ConfigurationValue referencedValue = configurationValues[vName] as ConfigurationValue
                    result = result.replace("\${$vName}",
                                            evaluateValueImpl(referencedValue.id,
                                                              referencedValue.value,
                                                              referencedValue.configuration,
                                                              blackList + [vName]))
                }
            }
        }
        return result
    }

}
