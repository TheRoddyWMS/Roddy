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
class ConfigurationValueTest extends GroovyTestCase {

    // Test for determineTypeOfValue is in RoddyConversionHelperMethodsTest.groovy
    // The method is directly bound to several methods of the conversion class and has nearly no
    // logic of its own. So we put it there.

    @Test
    public void testNothing() {
        // Asimple test so the class really fails.
        assert false
    }
}
