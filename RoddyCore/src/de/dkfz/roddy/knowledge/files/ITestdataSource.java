package de.dkfz.roddy.knowledge.files;

/**
 * Implement this interface to allow the system to create test data with the implementing class.
 * @author michael
 */
public interface ITestdataSource {
    public boolean createTestData();
}
