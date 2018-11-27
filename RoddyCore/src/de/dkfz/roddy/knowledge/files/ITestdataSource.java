/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.files;

/**
 * Implement this interface to allow the system to create test data with the implementing class.
 * @author michael
 */
public interface ITestdataSource {
    public boolean createTestData();
}
