package de.dkfz.roddy.config;

import java.util.LinkedList;
import java.util.List;

/**
 * Helps configurations to store overridable versions of configuration values and other things
 */
public class RecursiveOverridableMapContainerForConfigurationValues extends RecursiveOverridableMapContainer<String, ConfigurationValue, Configuration> {
    RecursiveOverridableMapContainerForConfigurationValues(Configuration parent, String id) {
        super(parent, id);
    }

    public ConfigurationValue get(String id, String defaultValue) {
        if (!hasValue(id)) {
            return new ConfigurationValue(getContainerParent(), id, defaultValue);
        } else {
            return getValue(id);
        }
    }

    public ConfigurationValue get(String id) {
        return get(id, "");
    }

    public boolean getBoolean(String id) {
        return getBoolean(id, false);
    }

    public boolean getBoolean(String id, boolean b) {
        return hasValue(id) ? getValue(id).toBoolean() : b;
    }

    /**
     * Returns the value converted to a string or an empty string ("") if the value could not be found.
     * @param id The id of the value
     * @return "" or the value converted to a string.
     */
    public String getString(String id) {
        return getString(id, "");
    }

    public String getString(String id, String s) {
        return hasValue(id) ? getValue(id).toString() : s;
    }

    /**
     * Put a configuration value to this containers map.
     *
     * @param id
     * @param value
     * @param type
     */
    public void put(String id, String value, String type) { super.add(new ConfigurationValue(id, value, type)); }

}
