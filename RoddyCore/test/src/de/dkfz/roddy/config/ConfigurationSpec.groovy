/*
 * Copyright (c) 2021 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config


import spock.lang.Specification

import static de.dkfz.roddy.config.ConfigurationConstants.CVALUE_TYPE_INTEGER

/**
 * Created by heinold on 22.07.16.
 */
class ConfigurationSpec extends Specification {

    PreloadedConfiguration mockContent(String name, PreloadedConfiguration parent) {
        return new PreloadedConfiguration(
                parent, Configuration.ConfigurationType.OTHER, name, "", "", null, "", ResourceSetSize.s,
                [], [], null, "")
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
        Configuration cfgB = new Configuration()
        Configuration cfgC = new Configuration()
        cfgB.addParent(cfgA)
        cfgC.addParent(cfgB)

        // Shall be elevated but their warnings and errors shall only exist once!
        cfgA.configurationValues << new ConfigurationValue("vala", 'detached $ sign ')
        cfgA.configurationValues << new ConfigurationValue("valb", 'type mismatch', CVALUE_TYPE_INTEGER)

        then:
        cfgC.warnings.size() == 1
        cfgC.warnings[0].id == ConfigurationIssue.ConfigurationIssueTemplate.detachedDollarCharacter

        cfgC.errors.size() == 1
        cfgC.errors[0].id == ConfigurationIssue.ConfigurationIssueTemplate.valueAndTypeMismatch
    }
}
