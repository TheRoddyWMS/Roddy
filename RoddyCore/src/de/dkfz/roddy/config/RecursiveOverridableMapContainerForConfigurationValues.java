/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config;

import java.util.LinkedList;

/**
 * Helps configurations to store overridable versions of configuration values and other things
 */
public class RecursiveOverridableMapContainerForConfigurationValues
        extends RecursiveOverridableMapContainer<String, ConfigurationValue, Configuration> {
    RecursiveOverridableMapContainerForConfigurationValues(Configuration parent, String id) {
        super(parent, id);
    }

    public ConfigurationValue get(String id, String defaultValue) {
        try {
            return getValue(id);
        } catch (ConfigurationError ex) {
            return new ConfigurationValue(getContainerParent(), id, defaultValue);
        }
    }

    /**
     * Temporary elevation makes it possible for Roddy to evaluate variables with dependent values properly.
     *
     * Given are two configurations:
     * C extends B, B extends A
     * =>   A -> B
     *
     * Configurations contain variables a and b
     * A = { a = 'abc', b = 'abc${a}' }         <br/> Idea will try to pull A B C on the same line on code format.
     * B = { a = 'def' }                        <br/>
     * expected C.b = 'klm'
     * expected B.b = 'hij'
     *
     * If you do not elevate the value, B.b would resolve to B.b = 'abc', because the configuration value does only know about its parent and predecessors.
     *
     * @param src
     * @return
     */
    @Override
    protected ConfigurationValue temporarilyElevateValue(ConfigurationValue src) {
        return new ConfigurationValue(this.getContainerParent(), src.id, src.value, src.getType(),
                src.getDescription(), new LinkedList<>(src.getListOfTags()));
    }

    public ConfigurationValue get(String id) {
        return get(id, "");
    }

    /** Get value or throw a RuntimeException, if the value does not exist. */
    public ConfigurationValue getOrThrow(String id) throws ConfigurationError {
        return getValue(id);
    }

    public ConfigurationValue getAt(String id) {
        return get(id, "");
    }

    public boolean getBoolean(String id) {
        return getBoolean(id, false);
    }

    public boolean getBoolean(String id, boolean b) {
        try {
            return getValue(id).toBoolean();
        } catch (ConfigurationError e) {
            return b;
        }
    }

    /**
     * Returns the value converted to a string or an empty string ("") if the value could not be found.
     *
     * @param id The id of the value
     * @return "" or the value converted to a string.
     */
    public String getString(String id) {
        return getString(id, "");
    }

    public String getString(String id, String s) {
        try {
            return getValue(id).toString();
        } catch (ConfigurationError e) {
            return s;
        }
    }

    /**
     * Put a configuration value to this containers map.
     *
     * @param id
     * @param value
     * @param type
     */
    public void put(String id, String value, String type) {
        super.add(new ConfigurationValue(this.getContainerParent(), id, value, type));
    }

    public void put(String id, String value) {
        this.put(id, value, "string");
    }

}
