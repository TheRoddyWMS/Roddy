/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core

import groovy.transform.CompileStatic

/**
 * A rareGAP dataset, which has a little extra logic built in. E.g.
 * Created by heinold on 06.12.16.
 */
@CompileStatic
class SuperCohortDataSet extends DataSet {
    final List<CohortDataSet> allCohorts = []

    SuperCohortDataSet(Analysis analysis, String id, List<CohortDataSet> allCohortDatasets) {
        super(analysis, id, allCohortDatasets?.first()?.primarySet?.getOutputFolderForAnalysis(analysis))
        this.allCohorts += allCohortDatasets
    }
}
