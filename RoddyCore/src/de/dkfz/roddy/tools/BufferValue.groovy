package de.dkfz.roddy.tools

/**
 * An Integer value representing sizes for e.g. files or memory units.
 * The values are internally aligned to Byte and use the BufferUnit to align the input value.
 * However, the default BufferUnit is MegaByte, is Megabyte (e.g. when no unit could be parsed)
 *
 * Values lower than M (k) will still be displayed as e.g. 3K as there is no support for floating point.
 * However floating point values can be parsed and will be rounded to their Integers.
 * Created by heinold on 10.11.15.
 */
public class BufferValue {

    /**
     * The stored value aligned to Byte
     */
    private Long alignedValue;

    public BufferValue(String inputValue, BufferUnit defaultInputUnit = BufferUnit.M) {

    }

    public BufferValue(Integer value, BufferUnit unit = BufferUnit.M) {

    }

    public BufferValue(Float value, BufferUnit unit = BufferUnit.M) {

    }

    public String toString(BufferUnit unit = BufferUnit.M) {

    }

    public Long toNumber(BufferUnit unit = BufferUnit.M) {

    }
}
