package de.dkfz.roddy.core;

import de.dkfz.roddy.config.TestDataOption;

/**
* A test data set encapsulates a productive data set and extends it with test data specific options.
*/
public class TestDataSet extends DataSet {
    public TestDataSet(Analysis analysis, DataSet dataSet, TestDataOption testDataOption, boolean existing) {
        super(analysis, dataSet.getId(), null);
//        super(testDataAnalysis, dataSet.getId(), null);
    }
}
