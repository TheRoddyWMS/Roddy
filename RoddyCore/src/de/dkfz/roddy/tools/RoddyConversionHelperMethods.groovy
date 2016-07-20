package de.dkfz.roddy.tools

import de.dkfz.roddy.execution.io.ExecutionHelper
import org.apache.commons.codec.digest.DigestUtils

import java.util.logging.Logger

/**
 * Contains methods to convert numbers
 *
 * User: michael
 * Date: 27.11.12
 * Time: 09:09
 */
@groovy.transform.CompileStatic
class RoddyConversionHelperMethods {

    public static int toInt(String value, int number = 0) {
        try {
            return Integer.parseInt(value)
        } catch (any) {
            return number;
        }
    }

    public static float toFloat(String value, float number = 0) {
        try {
            return Float.parseFloat(value)
        } catch (any) {
            return number;
        }
    }

    public static double toDouble(String value, double number = 0) {
        try {
            return Double.parseDouble(value)
        } catch (any) {
            return number;
        }
    }

    public static boolean toBoolean(String value, boolean defaultValue) {
        value = value?.toLowerCase()
        if (value == "true" || value == "1") return true;
        if (value == "false" || value == "0") return false;
        return defaultValue ?: false
    }

    public static boolean isInteger(String str) {
        return !isNullOrEmpty(str) && str.isInteger();
    }

    static boolean isFloat(String str) {
        return !isNullOrEmpty(str) && str.isFloat() &&
                (!str.contains(".") || str.endsWith("f")); // Expand test to "real" floats ending with "f", if there is a format like 1.0f 1.0e10f
    }

    static boolean isDouble(String str) {
        return !isNullOrEmpty(str) && !str.endsWith("f") && str.isDouble(); // In case of double it is easy, they are not allowed to end with f
    }

    public static boolean isNullOrEmpty(String string) {
        return !string;
    }

    public static boolean isNullOrEmpty(Collection collection) {
        return !collection;
    }

    public static boolean isDefinedArray(String value) {
        return !isNullOrEmpty(value) && ([value[0], value[-1]] == ["(", ")"]); // What else to test??
    }
}
