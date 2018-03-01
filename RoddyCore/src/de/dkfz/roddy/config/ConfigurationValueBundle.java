/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 */
public class ConfigurationValueBundle implements RecursiveOverridableMapContainer.Identifiable {

    private final Map<String, ConfigurationValue> values = new HashMap<>();

    private final String id;

    public ConfigurationValueBundle(String id, Map<String, ConfigurationValue> values) {
        this.id = id;
        if (values != null)
            this.values.putAll(values);
    }

    @Override
    public String getID() {
        return id;
    }

    public List<String> getKeys() {
        return new LinkedList<>(values.keySet());
    }

    public List<ConfigurationValue> getValues() {
        return new LinkedList<>(values.values());
    }

    public ConfigurationValue getAt(String key) {
        return values.get(key);
    }
}
