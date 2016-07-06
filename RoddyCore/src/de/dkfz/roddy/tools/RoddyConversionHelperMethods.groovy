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

    public static float toFloat(String value, int number = 0) {
        try {
            return Float.parseFloat(value)
        } catch (any) {
            return number;
        }
    }

    public static double toDouble(String value, int number = 0) {
        try {
            return Double.parseDouble(value)
        } catch (any) {
            return number;
        }
    }

    public static boolean toBoolean(String value, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(value)
        } catch (any) {
            return defaultValue;
        }
    }

    public static boolean isInteger(String str) {
        return str.isInteger();
    }

    public static boolean isNullOrEmpty(String string) {
        return !string;
    }

    public static boolean isNullOrEmpty(Collection collection) {
        return !collection;
    }
}
