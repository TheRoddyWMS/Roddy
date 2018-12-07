/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core;

/**
 * A data set listener can register to datasets.
 * He will be informed if a data set is changed, i.e. when a new execution context is added.
 */
public interface DataSetListener {

    void processingInfoAddedEvent(DataSet dataSet, AnalysisProcessingInformation pi);

    void processingInfoRemovedEvent(DataSet dataSet, AnalysisProcessingInformation pi);
}
