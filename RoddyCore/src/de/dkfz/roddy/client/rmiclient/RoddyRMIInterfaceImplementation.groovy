/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.rmiclient

import de.dkfz.roddy.Roddy
import de.dkfz.roddy.client.cliclient.RoddyCLIClient
import de.dkfz.roddy.core.Analysis
import de.dkfz.roddy.core.AnalysisProcessingInformation
import de.dkfz.roddy.core.DataSet
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextError
import de.dkfz.roddy.core.ExecutionContextLevel
import de.dkfz.roddy.core.ExecutionContextSubLevel
import de.dkfz.roddy.core.InfoObject
import de.dkfz.roddy.tools.EscapableString
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.execution.jobs.BEJob
import de.dkfz.roddy.execution.jobs.Job
import de.dkfz.roddy.execution.jobs.JobState;
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import groovy.transform.CompileStatic

import java.rmi.RemoteException

/**
 * The RMI Server implementation for the Roddy RMI run mode
 * Created by heinold on 07.09.16.
 */
@CompileStatic
class RoddyRMIInterfaceImplementation implements RoddyRMIInterface {

    static class DataSetInfoObject implements Serializable {

        final long classversion = 1

        String id
        String project
        File path

        DataSetInfoObject(DataSet ds) {
            this.id = ds.id
            this.project = ds.project.configurationName
            this.path = ds.outputBaseFolder
        }
    }

    static class ExtendedDataSetInfoObject implements Serializable {

        final long classversion = 1

        DataSetInfoObject dataSetInfoObject;

        ExtendedDataSetInfoObject(DataSet ds) {
            this.dataSetInfoObject = new DataSetInfoObject(ds)
        }
    }

    static class JobInfoObject implements Serializable {

        final long classversion = 1

        String jobId
        String jobName
        String toolId
        File logFile

        JobState jobState
        boolean isFakeJob

        Map<String, EscapableString> parameters
        Map<File, String> parentFiles

        JobInfoObject(Job job) {
            jobId = job.jobID
            jobName = job.jobName
            toolId = job.toolID
            logFile = job.logFile

            jobState = job.jobState
            isFakeJob = job.fakeJob

            parameters = job.parameters
            parentFiles = job.parentFiles.collectEntries { return [it.absolutePath, it.class.name] }
        }

        boolean isFakeJob() {
            return isFakeJob
        }

        Map<String, EscapableString> getParameters() {
            return parameters
        }

        boolean hasLogFile() {
            return logFile
        }
    }


    static class ExtendedDataSetInfoObjectCollection implements Serializable {

        final long classversion = 1

        DataSetInfoObject dataset;

        ExecutionContextInfoObject dummy
        ExecutionContextInfoObject running
        List<ExecutionContextInfoObject> list = []

    }

    static class ExecutionContextInfoObject implements Serializable {

        final long classversion = 1

        String datasetId
        String projectId
        String analysisId

        File executionDirectory
        File inputDirectory
        File outputDirectory
        Date executionDate
        String executionDateHumanReadable
        String executingUser

        ExecutionContextLevel executionContextLevel
        ExecutionContextSubLevel executionContextSubLevel

        List<JobInfoObject> executedJobs
        List<ExecutionContextError> errors

        ExecutionContextInfoObject(ExecutionContext context) {
            datasetId = context.dataSet.id
            projectId = context.analysis.project.configurationName
            analysisId = context.analysis.name
            executionDirectory = context.executionDirectory
            inputDirectory = context.inputDirectory
            outputDirectory = context.outputDirectory
            executingUser = context.executingUser

            executionDate = context.timestamp
            executionDateHumanReadable = InfoObject.formatTimestampReadable(context.getTimestamp())
            executionContextLevel = context.executionContextLevel
            executionContextSubLevel = context.detailedExecutionContextLevel

            executedJobs = context.startedJobs.collect { new JobInfoObject(it) }
            errors = context.errors
        }

        ExecutionContextInfoObject(AnalysisProcessingInformation api) {
            this(api.detailedProcessingInfo)

            executionDirectory = api.execPath
            executionDate = api.executionDate
            executionDateHumanReadable = api.executionDateHumanReadable
        }

    }

    private static LoggerWrapper logger = LoggerWrapper.getLogger(RoddyRMIInterfaceImplementation.class.name)

    public final long interfaceClassVersion = 1

    private Map<String, Analysis> analysesById = [:]

    private Map<String, Map<String, DataSet>> dataSetsByAnalysisAndId = [:]

    @Override
    boolean ping(boolean keepalive) throws RemoteException {
        println("ping ($keepalive)")
        if (keepalive)
            RoddyRMIServer.touchServer()
        return RoddyRMIServer.active
    }

    @Override
    void close() {
        RoddyRMIServer.stopServer()
    }

    @Override
    long getInterfaceClassVersion() throws RemoteException {
        return interfaceClassVersion
    }

    static String reformatAnalysisId(String analysisId) {
        return analysisId.split("::")[0]
    }

    /**
     * Load and cache an analysis object for the given analysis id.
     * @param analysisId
     * @return
     */
    public synchronized Analysis loadAnalysis(String analysisId) {
        analysisId = reformatAnalysisId(analysisId); // ensure, that long id's also work.
        if (!analysesById.containsKey(analysisId)) {
            // We correct the analysisId and load it a bit differently than usual
            String project = Roddy.commandLineCall.arguments[1].split("[@]")[0]
            analysesById[analysisId] = RoddyCLIClient.loadAnalysisOrFail("${project}@${analysisId}")
        }
        return analysesById[analysisId]
    }

    public Map<String, DataSet> getDataSetsForAnalysis(String analysisId) {
        analysisId = reformatAnalysisId(analysisId)
        synchronized (dataSetsByAnalysisAndId) {
            Analysis analysis = loadAnalysis(analysisId)
            if (analysis) {
                if (!dataSetsByAnalysisAndId[analysisId]) {
                    dataSetsByAnalysisAndId[analysisId] =
                            analysis.runtimeService.getListOfPossibleDataSets(analysis).
                                    collectEntries { DataSet it -> [it.id, it] } as Map<String, DataSet>
                }
                return dataSetsByAnalysisAndId[analysisId]
            } else {
                return [:]
            }
        }
    }

    /**
     * A method which can be used to encapsulate a server call.
     * @param defaultResult
     * @param c
     */
    def withServer(String message = "", def defaultResult, Closure c) {
        if (message)
            logger.postAlwaysInfo(message)
        RoddyRMIServer.touchServer()
        def result = defaultResult
        try {
            result = c()
            RoddyRMIServer.touchServer()
        } catch (Exception ex) {
            logger.postAlwaysInfo("Error in RMI Server call:\n" + RoddyIOHelperMethods.getStackTraceAsString(ex))
        }
        return result
    }

    @Override
    List<DataSetInfoObject> listdatasets(String analysisId) {
        return withServer("listdatasets for $analysisId", [],
                { getDataSetsForAnalysis(analysisId).values().collect {
                        new DataSetInfoObject(it)
                    }
                }) as List<DataSetInfoObject>
    }

    @Override
    ExtendedDataSetInfoObjectCollection queryExtendedDataSetInfo(String id, String analysisId) {
        withServer("queryExtendedDataSetInfo for $analysisId and $id", null, {
            Analysis analysis = loadAnalysis(analysisId);
            DataSet ds = getDataSetsForAnalysis(analysisId)[id]

            ExtendedDataSetInfoObjectCollection ioc = new ExtendedDataSetInfoObjectCollection()
            ioc.dataset = new DataSetInfoObject(ds);

            List<AnalysisProcessingInformation> processingInformation = ds.getProcessingInformation(analysis)

            ioc.list = ds.getProcessingInformation(analysis).collect {
                AnalysisProcessingInformation api ->
                    new ExecutionContextInfoObject(api)
            }
            return ioc
        }) as ExtendedDataSetInfoObjectCollection
    }

    @Override
    JobState queryDataSetState(String dataSetId, String analysisId) throws RemoteException {
        return withServer(JobState.UNKNOWN, {
            Map<DataSet, Boolean> status = loadAnalysis(analysisId).checkStatus([dataSetId]);
            if (!status) return JobState.UNSTARTED;
            if (status && status.values()[0]) return JobState.RUNNING;
        }) as JobState
    }

    @Override
    boolean queryDataSetExecutability(String id, String analysisId) {
        return withServer(false, {
            if (queryDataSetState(id, analysisId) == JobState.RUNNING) return false
            def analysis = loadAnalysis(analysisId)
            return ExecutionContext.createAnalysisWorkflowObject(analysis).
                    checkExecutability(analysis.run([id], ExecutionContextLevel.QUERY_STATUS)[0])
        })
    }

    @Override
    List<ExecutionContextInfoObject> run(List<String> datasetIds, String analysisId) throws RemoteException {
        return withServer([], {
            loadAnalysis(analysisId).run(datasetIds, ExecutionContextLevel.RUN).collect {
                new ExecutionContextInfoObject(it)
            }
        }) as List<ExecutionContextInfoObject>
    }

    @Override
    List<ExecutionContextInfoObject> testrun(List<String> datasetIds, String analysisId) {
        return withServer([], {
            loadAnalysis(analysisId).run(datasetIds, ExecutionContextLevel.QUERY_STATUS).collect {
                new ExecutionContextInfoObject(it)
            }
        }) as List<ExecutionContextInfoObject>
    }

    @Override
    List<ExecutionContextInfoObject> rerun(List<String> datasetIds, String analysisId) throws RemoteException {
        return withServer([], {
            Analysis analysis = loadAnalysis(analysisId);
            return analysis.rerun(analysis.run(datasetIds, ExecutionContextLevel.QUERY_STATUS), false).collect {
                new ExecutionContextInfoObject(it)
            }
        }) as List<ExecutionContextInfoObject>
    }

    @Override
    List<ExecutionContextInfoObject> testrerun(List<String> datasetIds, String analysisId) throws RemoteException {
        return withServer([], {
            Analysis analysis = loadAnalysis(analysisId);
            return analysis.rerun(analysis.run(datasetIds, ExecutionContextLevel.QUERY_STATUS), true).collect {
                new ExecutionContextInfoObject(it)
            }
        }) as List<ExecutionContextInfoObject>
    }

    @Override
    Map<String, JobState> queryJobState(List<BEJob> jobs) throws RemoteException {
        return withServer([:], { Roddy.getJobManager().queryJobStatus(jobs) }) as Map<String, JobState>
    }

    @Override
    List<String> readLocalFile(String path) throws RemoteException {
        return withServer([], { new File(path).readLines() }) as List<String>
    }

    @Override
    List<String> readRemoteFile(String path) throws RemoteException {
        return withServer([], {
            Arrays.asList(FileSystemAccessProvider.instance.loadTextFile(new File(path))) ?: new String[0]
        }) as List<String>
    }
}
