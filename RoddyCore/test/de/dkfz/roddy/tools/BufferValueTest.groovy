package de.dkfz.roddy.tools;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by heinold on 10.11.15.
 */
public class BufferValueTest {

    private static Map<String, String> validAndExpectedValues = [
            "3k"  : "3K",
            "3K"  : "3K",
            "50g" : "51200M",
            "4t"  : "4194304M",
            "4T"  : "4194304M",
            "1"   : "1M",
            "1000": "1000M",
            "3.5G": "35840M",
            "2.4t": "2516582M"
    ]


    private static List<String> invalidValues = [
            "3kb", "g", "3z"
    ]

    @Test
    public void testToString() throws Exception {
        validAndExpectedValues.each {
            String value, String expectedValue ->
                BufferValue testValue = new BufferValue(value);
                assert testValue.toString().equals(expectedValue);
        }
    }

    @Test
    public void testToNumber() throws Exception {
//        invalidValues.each {
//            String value ->
//
//        }
    }
}