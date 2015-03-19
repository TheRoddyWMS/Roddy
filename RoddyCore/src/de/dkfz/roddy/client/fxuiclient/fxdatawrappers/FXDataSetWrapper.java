package de.dkfz.roddy.client.fxuiclient.fxdatawrappers;

import de.dkfz.roddy.core.*;
import de.dkfz.roddy.execution.io.fs.FileSystemInfoProvider;

import java.util.List;

/**
 * Encapsulates DataSets (like i.e. PIDs)
 */
public class FXDataSetWrapper implements Comparable<FXDataSetWrapper> {
    private final DataSet dataSet;
    private final Analysis analysis;
    private Boolean isExecutable;

    public FXDataSetWrapper(DataSet dataSet, Analysis analysis) {
        this.dataSet = dataSet;
        this.analysis = analysis;
    }

    public DataSet getDataSet() {
        return dataSet;
    }

    public String getID() {
        return dataSet.toString();
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public AnalysisProcessingInformation getDummyProcessingInfo() {
        return dataSet.getDummyAnalysisProcessingInformation(analysis);
    }

    public AnalysisProcessingInformation getRunningOrPlannedProcessingInfo() {
        return dataSet.getActiveAnalysisProcessingInformation(analysis);
    }

    public List<AnalysisProcessingInformation> getProcessingInfo() {
        return dataSet.getProcessingInformation(analysis);
    }

    public AnalysisProcessingInformation getValidProcessingInfo() {
        AnalysisProcessingInformation api = getDummyProcessingInfo();
        if (api == null)
            api = getRunningOrPlannedProcessingInfo();
        if (api == null && getProcessingInfo().size() > 0)
            api = getProcessingInfo().get(0);
        return api;
    }

    public boolean isExecutable() {
        if(isExecutable == null) {
            DataSet ds = dataSet;
            Analysis analysis = getAnalysis();
            Workflow workflow = analysis.getWorkflow();
            isExecutable = workflow.checkExecutability(new ExecutionContext(FileSystemInfoProvider.getInstance().callWhoAmI(), analysis, ds, ExecutionContextLevel.QUERY_STATUS, ds.getOutputFolderForAnalysis(analysis), ds.getInputFolderForAnalysis(analysis), null, -1, true));
        }
        return isExecutable;
    }

    @Override
    public int compareTo(FXDataSetWrapper o) {
        return dataSet.getId().compareTo(o.dataSet.getId());
    }
}
