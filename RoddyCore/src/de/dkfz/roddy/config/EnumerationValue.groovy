/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config

/**
 * Enumeration values for Roddy enumerations
 */
@groovy.transform.CompileStatic
public class EnumerationValue {
    private final String id;
    private final String description;
    private final String tag;

    public EnumerationValue(String id, String description, String tag = null) {
        this.tag = tag
        this.id = id;
        this.description = description;
    }

    String getId() {
        return id
    }

    String getDescription() {
        return description
    }

    String getTag() {
        return tag
    }
}