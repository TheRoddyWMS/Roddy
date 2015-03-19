package de.dkfz.roddy.config;

/**
 * A test data option allows an analysis configuration to create test data sets out of productive data.
 * Therefore it needs at least four things:
 * An id (i.e. small)
 * A size (i.e. 10000 [datasets, like in a lane])
 * A ratio (absolute or relative)
 * A path (with testdataOutputBaseDirectory as the default)
 */
public class TestDataOption {
    public enum Ratio {
        absolute,
        relative
    }

    private String id;
    private int size = 10000;
    private Ratio ratio = Ratio.absolute;
    private ConfigurationValue testDataPath;
    private ConfigurationValue outputPath;

    public TestDataOption(String id, int size, Ratio ratio, ConfigurationValue testDataPath, ConfigurationValue outputPath) {
        this.id = id;
        this.size = size;
        this.outputPath = outputPath;
        this.ratio = ratio == null ? Ratio.absolute : ratio;
        this.testDataPath = testDataPath;
    }

    public String getId() {
        return id;
    }

    public int getSize() {
        return size;
    }

    public Ratio getRatio() {
        return ratio;
    }

    public ConfigurationValue getTestDataPath() {
        return testDataPath;
    }

    public ConfigurationValue getOutputPath() {
        return outputPath;
    }
}
