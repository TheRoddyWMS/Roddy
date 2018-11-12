/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config

import groovy.transform.CompileStatic
import spock.lang.Specification

/**
 * Created by heinold on 22.07.16.
 */
class ConfigurationSpec extends Specification {

    PreloadedConfiguration mockContent(String name, PreloadedConfiguration parent) {
        return new PreloadedConfiguration(parent, Configuration.ConfigurationType.OTHER, name, "", "", null, "", ResourceSetSize.s, [], [], null, "")
    }

    void testGetResourcesSize() {
        when:
        def iccA = mockContent("A", null)
        def iccB = mockContent("B", iccA)
        def iccC = mockContent("C", iccB)

        Configuration cfgB = new Configuration(iccB)
        Configuration cfgC = new Configuration(iccC)

        cfgC.getConfigurationValues() << new ConfigurationValue(cfgC, ConfigurationConstants.CFG_USED_RESOURCES_SIZE, "xl")

        then:
        cfgB.getResourcesSize() == ResourceSetSize.s
        cfgC.getResourcesSize() == ResourceSetSize.xl
    }

    void "test bad configuration values in configuration"() {
        when:
        Configuration cfgA = new Configuration()
        cfgA.configurationValues << new ConfigurationValue("vala", 'unattached $ sign ')
        cfgA.configurationValues << new ConfigurationValue("valb", 'type mismatch', "integer")

        then:
        cfgA.warnings.size() == 1
        cfgA.warnings[0].id == ConfigurationIssue.ConfigurationIssueTemplate.unattachedDollarCharacter

        cfgA.errors.size() == 1
        cfgA.errors[0].id == ConfigurationIssue.ConfigurationIssueTemplate.valueAndTypeMismatch
    }
}
