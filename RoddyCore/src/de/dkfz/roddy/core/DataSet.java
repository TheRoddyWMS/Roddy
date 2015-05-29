package de.dkfz.roddy.core;

import de.dkfz.roddy.config.TestDataOption;
import de.dkfz.roddy.execution.io.fs.FileSystemInfoProvider;
import de.dkfz.roddy.execution.jobs.Job;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Represents a unique dataset like i.e. a PID or a cohort.
 * A unique dataset belongs to a project. Each DataSet can be associated with one ore more analysesByID. This reference is managed by the analysis.
 * For each dataset a number of runs can exist.
 */
@groovy.transform.CompileStatic
public class DataSet extends InfoObject implements Serializable, ExecutionContextListener {

    private String id;

    /**
     * This is the PIDs base folder
     */
    private File outputBaseFolder;

    private Project project;

    private transient final Deque<DataSetListener> listeners = new ConcurrentLinkedDeque<>();

    /**
     * List of all analysesByID to which this DataSet is related.
     */
    private Map<String, Analysis> analysesByID = new HashMap<>();

    /**
     * It is only allowed to have one dummy / query process at a time for each analysis
     */
    private Map<Analysis, AnalysisProcessingInformation> processingInformationDummies = new HashMap<>();
    /**
     * It is only allowed to have one concurrent running process at a time for each analysis
     */
    private Map<Analysis, AnalysisProcessingInformation> processingInformationPlannedOrRunning = new HashMap<>();
    /**
     * This is a list of finished or read out processing info containers. The api objects are stored sorted by their creation / runtime
     */
    private Map<Analysis, List<AnalysisProcessingInformation>> processingInformation = new HashMap<>();

    private Map<Analysis, Map<TestDataOption, TestDataSet>> testdataContainer = new HashMap<>();
    /**
     * Marks a dataset as being only in the output directory.
     */
    private boolean onlyAvailableInOutputDirectory;

    public DataSet(Analysis analysis, String id, File outputBaseFolder) {

        this.id = id;
        this.outputBaseFolder = outputBaseFolder;
        this.project = analysis.getProject();
        this.analysesByID.put(analysis.getName(), analysis);
        ExecutionContext.registerStaticContextListener(this);
    }

    public String getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    /**
     * Returns a folder like i.e. results_per_pid/ICGC_PCA0xxx
     *
     * @return
     */
    public File getOutputBaseFolder() {
        return outputBaseFolder;
    }

    public File getInputFolderForAnalysis(Analysis analysis) {
        return project.getRuntimeService().getInputFolderForDataSetAndAnalysis(this, analysis);
    }

    public File getOutputFolderForAnalysis(Analysis analysis) {
        return project.getRuntimeService().getOutputFolderForDataSetAndAnalysis(this, analysis);
    }

    /**
     * Adds (or overrides) an analysis to the list.
     *
     * @param analysis
     */
    public void updateAnalysis(Analysis analysis) {
        analysesByID.put(analysis.getName(), analysis);
    }

    public Analysis getAnalysis(String id) {
        return analysesByID.get(id);
    }

    public boolean hasAnalysis(String id) {
        return analysesByID.containsKey(id);
    }

    /**
     * Returns a list of known / loaded analyses for this dataset.
     */
    public List<String> getListOfAnalyses() {
        return new LinkedList<>(analysesByID.keySet());
    }

    private List<AnalysisProcessingInformation> getAnalysisContainer(Analysis analysis) {
        if (!processingInformation.containsKey(analysis)) {
            processingInformation.put(analysis, new LinkedList<AnalysisProcessingInformation>());
        }
        return processingInformation.get(analysis);
    }

    public void addProcessingInformation(AnalysisProcessingInformation pi) {
        Analysis analysis = pi.getAnalysis();
        List<AnalysisProcessingInformation> con = getAnalysisContainer(analysis);
        if (con.contains(pi)) //Don't sort in existing objects.
            return;
        con.add(pi);
        Collections.sort(con, new Comparator<AnalysisProcessingInformation>() {
            @Override
            public int compare(AnalysisProcessingInformation o1, AnalysisProcessingInformation o2) {
                return o2.getExecPath().compareTo(o1.getExecPath());
            }
        });
        fireProcessingInfoAddedEvent(pi);
    }

    public boolean hasDummyAnalysisProcessingInformation(Analysis analysis) {
        synchronized (processingInformationDummies) {
            return this.processingInformationDummies.get(analysis) != null;
        }
    }

    public boolean hasPlannedOrRunningAnalysisProcessingInformation(Analysis analysis) {
        synchronized (processingInformationPlannedOrRunning) {
            return this.processingInformationPlannedOrRunning.get(analysis) != null;
        }
    }

    public void removeDummyAnalysisProcessingInformation(Analysis analysis) {
        synchronized (processingInformationDummies) {
            processingInformationDummies.remove(analysis);
        }
    }

    public void setDummyAnalysisProcessingInformation(AnalysisProcessingInformation dummy) {
        Analysis analysis = dummy.getAnalysis();
        synchronized (processingInformationDummies) {
            if (processingInformationDummies.containsKey(analysis)) {

            } else {
                processingInformationDummies.put(analysis, dummy);
            }
        }
    }

    public void setPlannedOrRunningAnalysisProcessingInformation(AnalysisProcessingInformation plannedOrRunning) {
        Analysis analysis = plannedOrRunning.getAnalysis();
        synchronized (processingInformationPlannedOrRunning) {
            if (processingInformationPlannedOrRunning.containsKey(analysis)) {

            } else {
                processingInformationPlannedOrRunning.put(analysis, plannedOrRunning);
            }
        }
    }

    public AnalysisProcessingInformation getDummyAnalysisProcessingInformation(Analysis analysis) {
        return processingInformationDummies.get(analysis);
    }

    public AnalysisProcessingInformation getActiveAnalysisProcessingInformation(Analysis analysis) {
        return processingInformationPlannedOrRunning.get(analysis);
    }

    public List<AnalysisProcessingInformation> getProcessingInformation(Analysis analysis) {
        return new LinkedList<>(getAnalysisContainer(analysis));
    }

    public AnalysisProcessingInformation getLatestValidProcessingInformation(Analysis analysis) {
        List<AnalysisProcessingInformation> information = getProcessingInformation(analysis);
        FileSystemInfoProvider fip = FileSystemInfoProvider.getInstance();
        for (AnalysisProcessingInformation api : information) {
            ExecutionContext detailedProcessingInfo = api.getDetailedProcessingInfo();
            if (api == null || detailedProcessingInfo == null || detailedProcessingInfo.getExecutedJobs().size() == 0 || !fip.checkDirectory(api.getExecPath(), null, false))
                continue;
            return api;
        }
        return null;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public void newExecutionContextEvent(ExecutionContext context) {
        if (context.getDataSet() != this)
            return;
        Analysis cAnalysis = context.getAnalysis();
        if (hasAnalysis(cAnalysis.getName())) {
            AnalysisProcessingInformation api = new AnalysisProcessingInformation(context);
            if (context.getExecutionContextLevel() == ExecutionContextLevel.QUERY_STATUS) {
                setDummyAnalysisProcessingInformation(api);
                //TODO Check, if the dummy process can be set? There should not be one or replace it?
            } else if (context.getExecutionContextLevel() == ExecutionContextLevel.QUERY_STATUS) {
                setPlannedOrRunningAnalysisProcessingInformation(api);
                //TODO Cancel or stop existing running processes? Should be better right? Or just throw an exception?
                removeDummyAnalysisProcessingInformation(cAnalysis);
            } else {
                addProcessingInformation(api);
            }
        } else {

        }
    }

    @Override
    public void jobStateChangedEvent(Job job) {
    }

    @Override
    public void jobAddedEvent(Job job) {
    }

    @Override
    public void fileAddedEvent(File file) {
    }

    @Override
    public void detailedExecutionContextLevelChanged(ExecutionContext context) {
    }

    public void addListener(DataSetListener listener, boolean replaceOfSameClass) {
        if (replaceOfSameClass) {
            Class c = listener.getClass();
            Deque<DataSetListener> listenersToKeep = new LinkedBlockingDeque<>();
            for (DataSetListener jsl : this.listeners) {
                if (jsl.getClass() != c)
                    listenersToKeep.add(jsl);
            }
            listeners.clear();
            listeners.addAll(listenersToKeep);
        }
        addListener(listener);
    }

    public void addListener(DataSetListener dataSetListener) {
        this.listeners.add(dataSetListener);
    }

    private void fireProcessingInfoAddedEvent(AnalysisProcessingInformation pi) {
        for (DataSetListener dsl : listeners) {
            dsl.processingInfoAddedEvent(this, pi);
        }
    }

    private void fireProcessingInfoRemovedEvent(AnalysisProcessingInformation pi) {
        for (DataSetListener dsl : listeners) {
            dsl.processingInfoRemovedEvent(this, pi);
        }
    }

    public void setAsAvailableInOutputOnly() {
        onlyAvailableInOutputDirectory = true;
    }

    public boolean isOnlyAvailableInOutputDirectory() {
        return onlyAvailableInOutputDirectory;
    }
}
