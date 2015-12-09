package de.dkfz.roddy.tools;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by heinold on 10.11.15.
 */
public class BufferValueTest {

    private static Map<String, String> validAndExpectedValues = [
            "2.4t": "2516582M",
            "2T"  : "2097152M",
            ""    : "1024M",
            "3k"  : "3K",
            "3K"  : "3K",
            "50g" : "51200M",
            "4t"  : "4194304M",
            "4T"  : "4194304M",
            "1"   : "1024M",
            "1000": "1024000M",
            "300M": "300M",
            "3.5G": "3584M",
    ]

    private static Map<String, String> validAndExpectedNumberValues = [
            "3k"  : 3,
            "2.4t": 2516582,
            ""    : 1024,
            "3K"  : 3,
            "50g" : 51200,
            "4t"  : 4194304,
            "4T"  : 4194304,
            "1"   : 1024,
            "1000": 1024000,
            "3.5G": 3584,
    ]


    @Test(expected = NumberFormatException)
    public void testInvalidStringParseWithInvalidUnit() {
        new BufferValue("3F")
    }

    @Test(expected = NumberFormatException)
    public void testInvalidStringParseWithMultiUnit() {
        new BufferValue("3kk")
    }

    @Test
    public void testToStringWithIntInput() {
        assert new BufferValue(1, BufferUnit.G).toString(BufferUnit.M) == "1024M"
        assert new BufferValue(1, BufferUnit.M).toString(BufferUnit.M) == "1M"
        assert new BufferValue(1, BufferUnit.T).toString(BufferUnit.M) == "1048576M"
        assert new BufferValue(1, BufferUnit.G).toString(BufferUnit.G) == "1G"
    }

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
        validAndExpectedNumberValues.each {
            String value, long expectedValue ->
                BufferValue testValue = new BufferValue(value);
                assert testValue.toNumber() == expectedValue;
        }
    }
}