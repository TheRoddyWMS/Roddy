/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config

import groovy.transform.CompileStatic
import org.junit.Test

/**
 * Created by heinold on 14.07.16.
 */
@CompileStatic
class ConfigurationValueTest {

    @Test
    public void testCValueSubstitutionWithCfgDuo() {
        // Two dependent values in base cfg
        // Overriden in extended cfg

        Configuration cfgBase = new Configuration(null)
        Configuration cfgExt = new Configuration(null, cfgBase)

        def cvBase = cfgBase.getConfigurationValues()
        def cvExt = cfgExt.getConfigurationValues()

        cvBase << new ConfigurationValue(cfgBase, "a", "abc")
        cvBase << new ConfigurationValue(cfgBase, "b", 'abc${a}')

        cvExt << new ConfigurationValue(cfgExt, "a", "def")

        assert cvExt["a"].value == "def"
        assert cvExt["b"].value == 'abc${a}'
        assert cvExt["b"].toString() == "abcdef"
    }
}
