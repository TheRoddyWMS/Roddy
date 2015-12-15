package de.dkfz.roddy.tools;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by heinold on 08.12.15.
 */
public class TimeUnitTest {

    private static Map<String, String> validAndExpectedValuesWithoutUnit = [
            ""         : "00:01:00:00",
            "4"        : "00:04:00:00",
            "04"       : "00:04:00:00",
            "120:00"   : "00:02:00:00",
            "180:00:00": "07:12:00:00",
    ]

    private static Map<String, String> validAndExpectedValuesWithUnit = [
            "s"   : "00:00:00:01",
            "m"   : "00:00:01:00",
            "h"   : "00:01:00:00",
            "4m"  : "00:00:04:00",
            "120m": "00:02:00:00",
            "180h": "07:12:00:00",
            "180H": "07:12:00:00",
            "5d"  : "05:00:00:00",
            "5D"  : "05:00:00:00",
    ]

    private static Map<String, String> validAndExpectedValuesWithFractions = [
            "5.25m"    : "00:00:05:15",
            "5.25d"    : "05:06:00:00",
            "5.25.5d"  : "05:06:30:00",
            "5.25.5.5d": "05:06:30:30",
            "3.5h"     : "00:03:30:00",
    ]

    @Test
    public void testToString() throws Exception {
        validAndExpectedValuesWithUnit.each {
            String value, String expectedValue ->
                assert (new TimeUnit(value).toString() == expectedValue);
        }
    }

    @Test
    public void testToStringWithoutUnit() throws Exception {
        validAndExpectedValuesWithoutUnit.each {
            String value, String expectedValue ->
                assert (new TimeUnit(value).toString() == expectedValue);
        }
    }

    @Test
    public void testToStringWithFractions() throws Exception {
        validAndExpectedValuesWithFractions.each {
            String value, String expectedValue ->
                assert (new TimeUnit(value).toString() == expectedValue);
        }
    }

    @Test(expected = NumberFormatException)
    public void testInvalidWalltimeString2() {
        new TimeUnit("3:5.25")
    }

    @Test(expected = NumberFormatException)
    public void testInvalidWalltimeString() {
        new TimeUnit("00:x0:");
    }

    @Test(expected = NumberFormatException)
    public void testInvalidToBigWalltimeString() {
        new TimeUnit("00:00:120:00:00:00");
    }

    @Test(expected = NumberFormatException)
    public void testMultiUnitChars() {
        new TimeUnit("4ddd");
    }

    @Test(expected = NumberFormatException)
    public void testInvalidTimeUnit() {
        new TimeUnit("4g");
    }
}