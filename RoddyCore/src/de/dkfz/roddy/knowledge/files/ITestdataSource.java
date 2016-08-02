/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.files;

/**
 * Implement this interface to allow the system to create test data with the implementing class.
 * @author michael
 */
public interface ITestdataSource {
    public boolean createTestData();
}
