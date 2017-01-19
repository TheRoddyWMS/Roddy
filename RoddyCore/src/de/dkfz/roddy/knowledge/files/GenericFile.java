/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.files;

/**
 * A generic file for various purposes.
 * Use this file class if you do not want to create a custom class and if there is no good class for your needs.
 * A generic file can be created with any parent file.
 * Use the onMethod filename pattern.
 */
public class GenericFile extends BaseFile {
    public GenericFile(ConstructionHelperForBaseFiles helper) {
        super( helper);
    }
}
