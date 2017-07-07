/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core

import de.dkfz.roddy.core.Analysis
import de.dkfz.roddy.core.DataSet
import groovy.transform.CompileStatic;

/**
 * Created by heinold on 19.12.16.
 */
@CompileStatic
public class CohortDataSet extends DataSet {
    final List<DataSet> allCohortDatasets

    final DataSet primarySet;

    CohortDataSet(Analysis analysis, String id, DataSet primarySet, List<DataSet> allCohortDatasets) {
        super(analysis, id, primarySet.getOutputFolderForAnalysis(analysis))
        this.allCohortDatasets = allCohortDatasets
        this.primarySet = primarySet
    }
}
