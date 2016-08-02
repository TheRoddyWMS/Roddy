package de.dkfz.roddy.config

import de.dkfz.roddy.core.ProjectFactory

/**
 * Created by heinold on 22.07.16.
 */
class ConfigurationTest extends GroovyTestCase {

    public InformationalConfigurationContent mockContent(String name, InformationalConfigurationContent parent) {
        return new InformationalConfigurationContent(parent, Configuration.ConfigurationType.OTHER, name, "", "", null, "", ResourceSetSize.s, [], [], null, "");
    }

    void testGetResourcesSize() {
        def iccA = mockContent("A", null)
        def iccB = mockContent("B", iccA)
        def iccC = mockContent("C", iccB)

        Configuration cfgA = new Configuration(iccA)
        Configuration cfgB = new Configuration(iccB)
        Configuration cfgC = new Configuration(iccC)

        cfgC.getConfigurationValues().add(new ConfigurationValue(cfgC, ConfigurationConstants.CFG_USED_RESOURCES_SIZE, "xl"));

        assert cfgB.getResourcesSize() == ResourceSetSize.s;

        assert cfgC.getResourcesSize() == ResourceSetSize.xl;
    }
}
