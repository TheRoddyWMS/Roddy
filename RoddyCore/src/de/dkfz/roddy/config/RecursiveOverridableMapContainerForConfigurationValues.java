/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static de.dkfz.roddy.config.ConfigurationConstants.CVALUE_TYPE_STRING;

/**
 * Helps configurations to store overridable versions of configuration values and other things.
 */
public class RecursiveOverridableMapContainerForConfigurationValues
        extends RecursiveOverridableMapContainer<String, ConfigurationValue, Configuration> {

    RecursiveOverridableMapContainerForConfigurationValues(@NotNull Configuration parent,
                                                           @NotNull String id) {
        super(parent, id);
    }

    public @NotNull ConfigurationValue get(@NotNull String id,
                                           @NotNull String defaultValue) {
        try {
            return getValue(id);
        } catch (ConfigurationError ex) {
            return new ConfigurationValue(getContainerParent(), id, defaultValue);
        }
    }

    public @NotNull ConfigurationValue get(@NotNull String id) {
        return get(id, "");
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
    protected @NotNull ConfigurationValue temporarilyElevateValue(@NotNull ConfigurationValue src) {
        return src.elevate(this.getContainerParent());
    }

    /** Get value or throw a ConfigurationError, if the value does not exist. */
    public @NotNull ConfigurationValue getOrThrow(@NotNull String id)
            throws ConfigurationError {
        return getValue(id);
    }

    public @NotNull ConfigurationValue getAt(@NotNull String id) {
        return get(id, "");
    }

    public boolean getBoolean(@NotNull String id)
            throws ConfigurationError {
        ConfigurationValue value = getValue(id);
        if (value == null)
            throw new ConfigurationError("Boolean value for '" + id + "' could not be found.", id);
        try {
            return value.toBoolean();
        } catch (NumberFormatException nfe) {
            throw new ConfigurationError("Value for '" + id + "' is not a boolean: " + value.toString(), id);
        }
    }

    public boolean getBoolean(@NotNull String id,
                              boolean defaultValue)
            throws ConfigurationError {
        return getBoolean(id, Boolean.toString(defaultValue));
    }

    public boolean getBoolean(@NotNull String id,
                              @NotNull String defaultValue)
            throws ConfigurationError {
//        if (!defaultValue.isEmpty())
//            throw new ConfigurationError("Default boolean value must not be empty", id);
        return getValue(id, new ConfigurationValue(id, defaultValue)).toBoolean();
    }

    public int getInteger(@NotNull String id,
                          int defaultValue)
            throws ConfigurationError {
        return getInteger(id, Integer.toString(defaultValue));
    }

    public int getInteger(@NotNull String id)
            throws ConfigurationError {
        ConfigurationValue value = getValue(id);
        if (value == null)
            throw new ConfigurationError("Integer value for '" + id + "' could not be found.", id);
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException nfe) {
            throw new ConfigurationError("Value for '" + id + "' is not an integer: " + value.toString(), id);
        }
    }

    public int getInteger(@NotNull String id,
                          @NotNull String defaultValue)
            throws ConfigurationError {
        if (defaultValue.isEmpty())
            throw new ConfigurationError( "Default integer value must not be empty", id);
        return getValue(id, new ConfigurationValue(id, defaultValue)).toInt();
    }

    public @NotNull List<String> getList(@NotNull String id) {
        return getList(id, ",");
    }

    public @NotNull List<String> getList(@NotNull String id,
                                         @NotNull String separator) {
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
    public @NotNull String getString(@NotNull String id) {
        return getString(id, "");
    }

    public @NotNull String getString(@NotNull String id,
                                     @NotNull String defaultValue) {
        return getValue(id, new ConfigurationValue(id, defaultValue)).toString();
    }

    /**
     * Put a configuration value to this containers map.
     *
     * @param id
     * @param value
     * @param type
     */
    public void put(@NotNull String id,
                    @Nullable String value,
                    @Nullable String type) {
        super.add(new ConfigurationValue(this.getContainerParent(), id, value, type));
    }

    public void put(@NotNull String id,
                    @Nullable String value) {
        this.put(id, value, CVALUE_TYPE_STRING);
    }

}
