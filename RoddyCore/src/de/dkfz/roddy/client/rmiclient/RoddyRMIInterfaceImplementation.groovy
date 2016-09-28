/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.rmiclient

import de.dkfz.roddy.Roddy
import de.dkfz.roddy.core.Analysis
import de.dkfz.roddy.core.AnalysisProcessingInformation
import de.dkfz.roddy.core.DataSet
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextError
import de.dkfz.roddy.core.ExecutionContextLevel
import de.dkfz.roddy.core.ExecutionContextSubLevel
import de.dkfz.roddy.core.InfoObject
import de.dkfz.roddy.core.ProjectFactory
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.execution.jobs.Job
import de.dkfz.roddy.execution.jobs.JobManager
import de.dkfz.roddy.execution.jobs.JobState
import groovy.transform.CompileStatic

import java.rmi.RemoteException

/**
 * The RMI Server implementation for the Roddy RMI run mode
 * Created by heinold on 07.09.16.
 */
@CompileStatic
public class RoddyRMIInterfaceImplementation implements RoddyRMIInterface {

    public static class DataSetInfoObject implements Serializable {
        String id;
        String project;
        File path;

        DataSetInfoObject(DataSet ds) {
            this.id = ds.id
            this.project = ds.getProject().getName()
            this.path = ds.getOutputBaseFolder()
        }
    }


    public static class ExtendedDataSetInfoObject implements Serializable {
        DataSetInfoObject dataSetInfoObject;

        ExtendedDataSetInfoObject(DataSet ds) {
            this.dataSetInfoObject = new DataSetInfoObject(ds);
        }
    }

    public static class JobInfoObject implements Serializable {
        String jobId;
        String jobName;
        String toolId;
        File logFile

        JobState jobState;
        boolean isFakeJob;

        Map<String, String> parameters;
        Map<File, String> parentFiles

        JobInfoObject(Job job) {
            jobId = job.getJobID()
            jobName = job.getJobName()
            toolId = job.getToolID()
            logFile = job.getLogFile()

            jobState = job.getJobState()
            isFakeJob = job.isFakeJob()

            parameters = job.getParameters()
            parentFiles = job.getParentFiles().collectEntries { return [it.absolutePath, it.class.name] }
        }

        public boolean isFakeJob() {
            return isFakeJob
        }

        Map<String, String> getParameters() {
            return parameters;
        }

        boolean hasLogFile() {
            return logFile;
        }
    }


    public static class ExtendedDataSetInfoObjectCollection implements Serializable {
        DataSetInfoObject dataset;

        ExecutionContextInfoObject dummy
        ExecutionContextInfoObject running
        List<ExecutionContextInfoObject> list = []

    }

    public static class ExecutionContextInfoObject implements Serializable {
        String datasetId;
        String projectId;
        String analysisId;

        File executionDirectory
        File inputDirectory
        File outputDirectory
        Date executionDate
        String executionDateHumanReadable
        String executingUser

        ExecutionContextLevel executionContextLevel
        ExecutionContextSubLevel executionContextSubLevel

        List<JobInfoObject> executedJobs;
        List<ExecutionContextError> errors;

        ExecutionContextInfoObject(ExecutionContext context) {
            datasetId = context.getDataSet().getId()
            projectId = context.getAnalysis().getProject().getName()
            analysisId = context.getAnalysis().getName()
            executionDirectory = context.getExecutionDirectory()
            inputDirectory = context.getInputDirectory()
            outputDirectory = context.getOutputDirectory()
            executingUser = context.getExecutingUser()

            executionDate = context.getTimestamp();
            executionDateHumanReadable = InfoObject.formatTimestampReadable(context.getTimestamp());
            executionContextLevel = context.getExecutionContextLevel()
            executionContextSubLevel = context.getDetailedExecutionContextLevel();

            executedJobs = context.getStartedJobs().collect { new JobInfoObject(it) }
            errors = context.getErrors()
        }

        ExecutionContextInfoObject(AnalysisProcessingInformation api) {
            this(api.getDetailedProcessingInfo())

            executionDirectory = api.execPath
            executionDate = api.getExecutionDate()
            executionDateHumanReadable = api.getExecutionDateHumanReadable()
        }

    }

    private Map<String, Analysis> analysesById = [:]

    private Map<String, Map<String, DataSet>> dataSetsByAnalysisAndId = [:]

    @Override
    boolean ping(boolean keepalive) throws RemoteException {
        println("ping ($keepalive)")
        if (keepalive)
            RoddyRMIServer.touchServer();
        return RoddyRMIServer.isActive();
    }

    @Override
    void close() {
        RoddyRMIServer.stopServer();
    }

    public static String reformatAnalysisId(String analysisId) {
        return analysisId.split("::")[0]
    }

    public synchronized Analysis loadAnalysis(String analysisId) {
        analysisId = reformatAnalysisId(analysisId); // ensure, that long id's also work.
        if (!analysesById.containsKey(analysisId)) {
            String project = Roddy.getCommandLineCall().getArguments()[1].split("[@]")[0]
            analysesById[analysisId] = ProjectFactory.getInstance().loadAnalysis("${project}@${analysisId}");
        }
        return analysesById[analysisId];
    }

    public Map<String, DataSet> getDataSetsForAnalysis(String analysisId) {
        analysisId = reformatAnalysisId(analysisId)
        synchronized (dataSetsByAnalysisAndId) {
            Analysis analysis = loadAnalysis(analysisId)
            if (!analysis)
                return [:];
            if (!dataSetsByAnalysisAndId[analysisId])
                dataSetsByAnalysisAndId[analysisId] = analysis.getListOfDataSets().collectEntries { DataSet it -> [it.id, it] } as Map<String, DataSet>
            return dataSetsByAnalysisAndId[analysisId];
        }
    }

    @Override
    List<DataSetInfoObject> listdatasets(String analysisId) {
        try {
            println("listdatasets for $analysisId")
            RoddyRMIServer.touchServer();
            Map<String, DataSet> mapOfDataSets = getDataSetsForAnalysis(analysisId);
            RoddyRMIServer.touchServer();
            return mapOfDataSets.values().collect { it -> new DataSetInfoObject(it) }
        } catch (Exception ex) {
            println(ex.getStackTrace());
            []
        }
    }

    @Override
    ExtendedDataSetInfoObjectCollection queryExtendedDataSetInfo(String id, String analysisId) {
        try {
            println("queryExtendedDataSetInfo for $analysisId and $id")
            RoddyRMIServer.touchServer();

            Analysis analysis = loadAnalysis(analysisId);
            DataSet ds = getDataSetsForAnalysis(analysisId)[id]

            ExtendedDataSetInfoObjectCollection ioc = new ExtendedDataSetInfoObjectCollection();
            ioc.dataset = new DataSetInfoObject(ds);

            List<AnalysisProcessingInformation> processingInformation = ds.getProcessingInformation(analysis);
//            if (ds.getDummyAnalysisProcessingInformation(analysis) == null && ds.getActiveAnalysisProcessingInformation(analysis) == null)
//                ioc.dummy = new ExecutionContextInfoObject(analysis.run(Arrays.asList(ds.getId()), ExecutionContextLevel.QUERY_STATUS)[0]);

            ioc.list = ds.getProcessingInformation(analysis).collect {
                AnalysisProcessingInformation api ->
                    new ExecutionContextInfoObject(api);
            }
//            processingInformation = ds.getProcessingInformation(analysis);
//            for (AnalysisProcessingInformation api : processingInformation) {
//                api.getDetailedProcessingInfo();
//            }

            RoddyRMIServer.touchServer();
            return ioc;
        } catch (Exception ex) {
            println(ex.getStackTrace());
            return null
        }
    }

    @Override
    JobState queryDataSetState(String dataSetId, String analysisId) throws RemoteException {
        try {
            RoddyRMIServer.touchServer();
            Map<DataSet, Boolean> status = loadAnalysis(analysisId).checkStatus([dataSetId]);
            if (!status) return JobState.UNSTARTED;
            if (status && status.values()[0]) return JobState.RUNNING;
            RoddyRMIServer.touchServer();
        } catch (Exception ex) {
            return JobState.UNKNOWN;
        }
    }

    @Override
    boolean queryDataSetExecutability(String id, String analysisId) {
        try {
            RoddyRMIServer.touchServer();
            if (queryDataSetState(id, analysisId) == JobState.RUNNING) return false
            boolean result = loadAnalysis(analysisId).getWorkflow().checkExecutability(loadAnalysis(analysisId).run([id], ExecutionContextLevel.QUERY_STATUS)[0]);
            RoddyRMIServer.touchServer();
            return result;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    List<ExecutionContextInfoObject> run(List<String> datasetIds, String analysisId) throws RemoteException {
        RoddyRMIServer.touchServer();
        try {
            def list = loadAnalysis(analysisId).run(datasetIds, ExecutionContextLevel.RUN).collect { new ExecutionContextInfoObject(it) };

            RoddyRMIServer.touchServer();
            return list;
        } catch (Exception ex) {
            println(ex.getStackTrace());
            []
        }
    }

    @Override
    List<ExecutionContextInfoObject> testrun(List<String> datasetIds, String analysisId) {
        RoddyRMIServer.touchServer();

        def list = loadAnalysis(analysisId).run(datasetIds, ExecutionContextLevel.QUERY_STATUS).collect { new ExecutionContextInfoObject(it) };

        RoddyRMIServer.touchServer();
        return list;
    }

    @Override
    List<ExecutionContextInfoObject> rerun(List<String> datasetIds, String analysisId) throws RemoteException {
        RoddyRMIServer.touchServer();

        Analysis analysis = loadAnalysis(analysisId);
        def list = analysis.rerun(analysis.run(datasetIds, ExecutionContextLevel.QUERY_STATUS), false).collect { new ExecutionContextInfoObject(it) };

        RoddyRMIServer.touchServer();
        return list;
    }

    @Override
    List<ExecutionContextInfoObject> testrerun(List<String> datasetIds, String analysisId) throws RemoteException {
        RoddyRMIServer.touchServer();

        Analysis analysis = loadAnalysis(analysisId);
        def list = analysis.rerun(analysis.run(datasetIds, ExecutionContextLevel.QUERY_STATUS), true).collect { new ExecutionContextInfoObject(it) };

        RoddyRMIServer.touchServer();
        return list;
    }

    @Override
    Map<String, JobState> queryJobState(List<String> jobIds) throws RemoteException {
        RoddyRMIServer.touchServer();

        def map = JobManager.getInstance().queryJobStatus(jobIds);

        RoddyRMIServer.touchServer();
        return map;
    }

    @Override
    List<String> readLocalFile(String path) throws RemoteException {
        RoddyRMIServer.touchServer();
        List<String> text
        try {
            File file = new File(path);
            text = file.readLines();
        } catch (Exception ex) {
            text = []
        }
        RoddyRMIServer.touchServer();
        return text;
    }

    @Override
    List<String> readRemoteFile(String path) throws RemoteException {
        RoddyRMIServer.touchServer();
        List<String> text
        try {
            text = Arrays.asList(FileSystemAccessProvider.getInstance().loadTextFile(new File(path)));
        } catch (Exception ex) {
            text = []
        }
        RoddyRMIServer.touchServer();
        return text;
    }
}
