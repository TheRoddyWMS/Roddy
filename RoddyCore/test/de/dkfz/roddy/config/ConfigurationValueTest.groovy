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
    public void testCValueSubstitutionWithCfgDualChain() {
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

    /**
     * A = { a = "abc", b = "def" }
     * B = { a = "hij", b = "$a" }
     * C = { a = "klm" }
     * C.b = "klm"
     * B.b = "hij"
     */
    @Test
    public void testCValueSubstitutionWithCfgTripleChain() {
        Configuration A = new Configuration(null)
        Configuration B = new Configuration(null, A)
        Configuration C = new Configuration(null, B)

        def cvA = A.getConfigurationValues()
        def cvB = B.getConfigurationValues()
        def cvC = C.getConfigurationValues()

        cvA << new ConfigurationValue(A, "a", "abc")
        cvA << new ConfigurationValue(A, "b", 'def')

        cvB << new ConfigurationValue(B, "a", "hij")
        cvB << new ConfigurationValue(B, "b", '${a}')

        cvC << new ConfigurationValue(C, "a", "klm")

        assert cvB["a"].value == "hij"
        assert cvB["b"].value == '${a}'
        assert cvB["b"].toString() == "hij"
        assert cvC["b"].toString() == "klm"
    }
}
