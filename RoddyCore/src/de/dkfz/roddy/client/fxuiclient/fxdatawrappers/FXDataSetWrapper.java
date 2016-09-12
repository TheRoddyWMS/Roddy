/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient.fxdatawrappers;

import de.dkfz.roddy.core.*;
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider;

import java.util.List;

/**
 * Encapsulates DataSets (like i.e. PIDs)
 */
public class FXDataSetWrapper implements Comparable<FXDataSetWrapper> {
    private final String project;
    private final String analysis;
    private final String id;
    private final String folder;

    public FXDataSetWrapper(String project, String analysis, String id, String folder) {
        this.project = project;
        this.analysis = analysis;
        this.id = id;
        this.folder = folder;
    }

    public String getProject() {
        return project;
    }

    public String getAnalysis() {
        return analysis;
    }

    public String getId() {
        return id;
    }

    public String getFolder() {
        return folder;
    }

    //    public AnalysisProcessingInformation getDummyProcessingInfo() {
//        return dataSet.getDummyAnalysisProcessingInformation(analysis);
//    }
//
//    public AnalysisProcessingInformation getRunningOrPlannedProcessingInfo() {
//        return dataSet.getActiveAnalysisProcessingInformation(analysis);
//    }
//
//    public List<AnalysisProcessingInformation> getProcessingInfo() {
//        return dataSet.getProcessingInformation(analysis);
//    }

//    public AnalysisProcessingInformation getValidProcessingInfo() {
//        AnalysisProcessingInformation api = getDummyProcessingInfo();
//        if (api == null)
//            api = getRunningOrPlannedProcessingInfo();
//        if (api == null && getProcessingInfo().size() > 0)
//            api = getProcessingInfo().get(0);
//        return api;
//    }

//    public boolean isExecutable() {
//        if(isExecutable == null) {
//            DataSet ds = dataSet;
//            Analysis analysis = getAnalysis();
//            Workflow workflow = analysis.getWorkflow();
//            isExecutable = workflow.checkExecutability(new ExecutionContext(FileSystemAccessProvider.getInstance().callWhoAmI(), analysis, ds, ExecutionContextLevel.QUERY_STATUS, ds.getOutputFolderForAnalysis(analysis), ds.getInputFolderForAnalysis(analysis), null, -1, true));
//        }
//        return isExecutable;
//    }

    @Override
    public int compareTo(FXDataSetWrapper o) {
        return id.compareTo(o.id);
    }
}
