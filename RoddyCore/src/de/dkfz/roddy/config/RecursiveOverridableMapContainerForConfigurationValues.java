/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static de.dkfz.roddy.config.ConfigurationConstants.CVALUE_TYPE_STRING;

/**
 * Helps configurations to store overridable versions of configuration values and other things.
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
     * Temporary elevation makes it possible for Roddy to properly evaluate variables with dependent values.
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
     * If you do not elevate the value, B.b would resolve to B.b = 'abc',
     * because the configuration value does only know about its parent and predecessors.
     *
     * @param src
     * @return
     */
    @Override
    protected ConfigurationValue temporarilyElevateValue(ConfigurationValue src) {
        return src.elevate(this.getContainerParent());
    }

    public ConfigurationValue get(String id) {
        return get(id, "");
    }

    /** Get value or throw a ConfigurationError, if the value does not exist. */
    public ConfigurationValue getOrThrow(String id) throws ConfigurationError {
        return getValue(id);
    }

    public ConfigurationValue getAt(String id) {
        return get(id, "");
    }

    public boolean getBoolean(String id) throws  ConfigurationError {
        return getBoolean(id, false);
    }

    public boolean getBoolean(String id, boolean defaultValue) throws ConfigurationError {
        return getValue(id, new ConfigurationValue(id, defaultValue)).toBoolean();
    }

    public Integer getInteger(String id) throws ConfigurationError {
        return getValue(id).toInt();
    }

    public Integer getInteger(String id, Integer defaultValue) throws ConfigurationError {
        return getValue(id, new ConfigurationValue(id, defaultValue)).toInt();
    }

    public List<String> getList(String id) {
        return getList(id, ",");
    }

    public List<String> getList(String id, String separator) {
        try {
            return Arrays.asList(getValue(id).toString().split("[" + separator + "]"));
        } catch(ConfigurationError e) {
            return new LinkedList<>();
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

    public String getString(String id, String defaultValue) {
        return getValue(id, new ConfigurationValue(id, defaultValue)).toString();
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
        this.put(id, value, CVALUE_TYPE_STRING);
    }

}
