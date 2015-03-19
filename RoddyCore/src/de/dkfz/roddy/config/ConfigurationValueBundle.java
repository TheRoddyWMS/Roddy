package de.dkfz.roddy.config;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 */
public class ConfigurationValueBundle implements RecursiveOverridableMapContainer.Identifiable {

    private static int cvbIDs = 0;

    private final Map<String, ConfigurationValue> values = new HashMap<>();

    private final int id = cvbIDs++;

    public ConfigurationValueBundle(Map<String, ConfigurationValue> values) {
        if (values != null)
            this.values.putAll(values);
    }

    @Override
    public String getID() {
        return "" + id;
    }

    public List<String> getKeys() {
        return new LinkedList<>(values.keySet());
    }

    public List<ConfigurationValue> getValues() {
        return new LinkedList<>(values.values());
    }

    public ConfigurationValue get(String key) {
        return values.get(key);
    }
}
