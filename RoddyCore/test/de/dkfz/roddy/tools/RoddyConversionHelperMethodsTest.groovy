/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.tools

import de.dkfz.roddy.config.ConfigurationValue
import groovy.transform.CompileStatic

/**
 * Created by heinold on 14.07.16.
 */
@CompileStatic
class RoddyConversionHelperMethodsTest extends GroovyTestCase {
    void testToInt() {
        assert 1 == RoddyConversionHelperMethods.toInt("1")
        assert 1 == RoddyConversionHelperMethods.toInt("a0", 1)
    }

    void testToFloat() {
        assert 1.0f == RoddyConversionHelperMethods.toFloat("1.0")
        assert 1.0f == RoddyConversionHelperMethods.toFloat("a0", 1.0f)
    }

    void testToDouble() {
        assert 1.0d == RoddyConversionHelperMethods.toDouble("1.0")
        assert 1.0d == RoddyConversionHelperMethods.toDouble("a0", 1.0)
    }

    void testToBoolean() {
        assert true == RoddyConversionHelperMethods.toBoolean("true", false)
        assert true == RoddyConversionHelperMethods.toBoolean("TRUE", false)
        assert true == RoddyConversionHelperMethods.toBoolean("True", false)
        assert true == RoddyConversionHelperMethods.toBoolean("1", false)

        // Would fail! toBoolean is either true or false and throws no exceptions.
        assert true == RoddyConversionHelperMethods.toBoolean("Flack", true)

        assert false == RoddyConversionHelperMethods.toBoolean("0", false)
        assert false == RoddyConversionHelperMethods.toBoolean("false", true)
        assert false == RoddyConversionHelperMethods.toBoolean("False", true)

        assert false == RoddyConversionHelperMethods.toBoolean("Flack", false)
    }

    void testIsInteger() {
        assert RoddyConversionHelperMethods.isInteger("1")
        assert RoddyConversionHelperMethods.isInteger("10")

        assert !RoddyConversionHelperMethods.isInteger("1.0")
        assert !RoddyConversionHelperMethods.isInteger("")
        assert !RoddyConversionHelperMethods.isInteger("1.a")
        assert !RoddyConversionHelperMethods.isInteger("a")

    }

    void testIsFloat() {
        assert RoddyConversionHelperMethods.isFloat("1")
        assert RoddyConversionHelperMethods.isFloat("1.0e10f")

        assert !RoddyConversionHelperMethods.isFloat("1.0")
        assert !RoddyConversionHelperMethods.isFloat("1.0e10")

        assert !RoddyConversionHelperMethods.isFloat("")
        assert !RoddyConversionHelperMethods.isFloat("1.a")
        assert !RoddyConversionHelperMethods.isFloat("a")

    }

    void testIsDouble() {
        assert RoddyConversionHelperMethods.isDouble("1")
        assert RoddyConversionHelperMethods.isDouble("1.0")
        assert RoddyConversionHelperMethods.isDouble("1.0e10")

        assert !RoddyConversionHelperMethods.isDouble("")
        assert !RoddyConversionHelperMethods.isDouble("1.a")
        assert !RoddyConversionHelperMethods.isDouble("a")
    }

    void testIsNullOrEmpty() {
        assert RoddyConversionHelperMethods.isNullOrEmpty([])
        assert RoddyConversionHelperMethods.isNullOrEmpty((String) null)
        assert RoddyConversionHelperMethods.isNullOrEmpty((Collection) null)
        assert RoddyConversionHelperMethods.isNullOrEmpty("")

        assert !RoddyConversionHelperMethods.isNullOrEmpty("a")
        assert !RoddyConversionHelperMethods.isNullOrEmpty(["a"])
        assert !RoddyConversionHelperMethods.isNullOrEmpty([1])
    }

    void testIsDefinedArray() {
        assert RoddyConversionHelperMethods.isDefinedArray("( )")
        assert RoddyConversionHelperMethods.isDefinedArray("( a b )")
        assert RoddyConversionHelperMethods.isDefinedArray("( a )")
        assert !RoddyConversionHelperMethods.isDefinedArray("")
        assert !RoddyConversionHelperMethods.isDefinedArray("a b c")
        assert !RoddyConversionHelperMethods.isDefinedArray("a;b;c")
    }

    /**
     * Test for deteremineTypeOfValue in ConfigurationValue class.
     * Test is here for low level of method logic. Nearly all the logic is in
     * the conversion methods.
     */
    void testDetermineTypeOfValue() {
        assert ConfigurationValue.determineTypeOfValue("( a b c )") == "bashArray"
        assert ConfigurationValue.determineTypeOfValue('"( a b c )"') == "string"
        assert ConfigurationValue.determineTypeOfValue("'( a b c )'") == "string"
        assert ConfigurationValue.determineTypeOfValue("1.0") == "double"
        assert ConfigurationValue.determineTypeOfValue("1.0f") == "float"
        assert ConfigurationValue.determineTypeOfValue("1") == "integer"
        assert ConfigurationValue.determineTypeOfValue("") == "string"
        assert ConfigurationValue.determineTypeOfValue("ba") == "string"
    }

}
