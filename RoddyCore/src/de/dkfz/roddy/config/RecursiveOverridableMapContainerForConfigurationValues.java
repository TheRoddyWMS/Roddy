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
    public void put(String id, String value, String type) {
        super.add(new ConfigurationValue(id, value, type));
    }

    public List<ConfigurationValue> getInheritanceList(String id) {
        List<ConfigurationValue> allValues = new LinkedList<>();

        if (getMap().containsKey(id))
            allValues.add(getValue(id));

        Configuration containerParent = getContainerParent();

        if (containerParent == null)
            return allValues;

        for (Configuration configuration : containerParent.getContainerParents()) {
            allValues.addAll(configuration.getConfigurationValues().getInheritanceList(id));
        }

        return allValues;
    }
}
