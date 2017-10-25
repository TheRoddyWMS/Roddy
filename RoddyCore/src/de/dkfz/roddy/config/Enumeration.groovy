/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config
/**
 * a custom enumeration class which allows the definition of enumerations in configuration files.
 */
@groovy.transform.CompileStatic
public class Enumeration implements RecursiveOverridableMapContainer.Identifiable {

    private final String name;

    private final String description;

    private final LinkedHashMap<String, EnumerationValue> values = new LinkedHashMap<String, EnumerationValue>();

    public Enumeration(String name, List<EnumerationValue> values, String description = "", Enumeration enumeration = null) {
        if (!name)
            throw new RuntimeException("A enumeration must have a valid name.")
        this.name = name;
        this.description = description;
        if (!values)
            throw new RuntimeException("There must be some enumeration values. An enumeration cannot be empty.")
//        this.values.putAll(enumeration.values);
        final Enumeration THIS = this;
        values.each { EnumerationValue ev -> THIS.values[ev.id] = ev };
    }

    String getName() {
        return name
    }

    String getDescription() {
        return description
    }

    LinkedHashMap<String, EnumerationValue> getValues() {
        return values
    }

    EnumerationValue getValue(String ev) {
        if (values.containsKey(ev))
            return values[ev]
        throw new NoSuchElementException("Type ${ev} is not known in enumeration ${name}")
    }

    @Override
    String getID() {
        return getName();
    }
}
