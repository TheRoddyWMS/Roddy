/*
 * Copyright (c) 2017 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core

import de.dkfz.roddy.Constants
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.config.ToolEntry
import de.dkfz.roddy.tools.EscapableString
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.execution.jobs.*
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.knowledge.files.LoadedFile
import de.dkfz.roddy.tools.LoggerWrapper
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.slurpersupport.NodeChild
import groovy.xml.MarkupBuilder

import java.nio.file.Paths

import static de.dkfz.roddy.StringConstants.*

/**
 * The class provides methods to:
 *  - read back an execution context from storage
 *  - read back the job state logfile
 *  - read and write job info files (xml)
 * Created by heinold on 04.04.17.
 */
@CompileStatic
class ExecutionContextReaderAndWriter {

    static final LoggerWrapper logger = LoggerWrapper.getLogger(ExecutionContextReaderAndWriter.class)

    private final RuntimeService runtimeService

    ExecutionContextReaderAndWriter(RuntimeService runtimeService) {
        this.runtimeService = runtimeService
    }

    /**
     * The method tries to read back an execution context from a directory structure.
     * In some cases, the method tries to recover information from other sources, if files are missing.
     *
     * @param api
     * @return
     */
    ExecutionContext readInExecutionContext(AnalysisProcessingInformation api) {
        logger.sometimes("Read back execution context from: ${api.execPath}")

        ExecutionContext context = getInitialContext(api)

        try {
            //First, try to read in the executedJobInfo file
            //All necessary information about jobs is stored there.
            List<Job> jobsStartedInContext = readJobsStartedInContext(context)

            Map<String, JobState> statusList = readInJobStateLogFile(context)

            /**
             * Use statusList to set all the jobs states.
             * If the state for a job is unknown
             */
            for (BEJob job : jobsStartedInContext) {
                if (job == null) continue

                if (job.jobID == "Unknown")
                    job.setJobState(JobState.FAILED)
                else
                    job.setJobState(JobState.UNSTARTED)

                for (String id : statusList.keySet()) {
                    JobState status = statusList[id]

                    if (job.getJobID() ==  new BEJobID(id))
                        continue
                    job.setJobState(status)
                }
            }

            Map<String, BEJob> unknownJobs = new LinkedHashMap<>()
            Map<String, BEJob> possiblyRunningJobs = new LinkedHashMap<>()
            //For every job which is still unknown or possibly running get the actual jobState from the cluster
            for (BEJob job : jobsStartedInContext) {
                if (job.getJobState().isUnknown() || job.getJobState() == JobState.UNSTARTED) {
                    unknownJobs.put(job.getJobID().toString(), job)
                } else if (job.getJobState() == JobState.STARTED) {
                    possiblyRunningJobs.put(job.getJobID().toString(), job)
                }
            }

            Map<BEJob, JobState> map = Roddy.getJobManager().queryJobStatus(jobsStartedInContext as List<BEJob>)
            for (String jobID : unknownJobs.keySet()) {
                BEJob job = unknownJobs[jobID]
                job.setJobState(map[job])
                Roddy.getJobManager().addToListOfStartedJobs(job)
            }
            for (String jobID : possiblyRunningJobs.keySet()) {
                BEJob job = possiblyRunningJobs[jobID]
                if (map[job] == null) {
                    job.setJobState(JobState.FAILED)
                } else {
                    job.setJobState(map[job])
                    Roddy.getJobManager().addToListOfStartedJobs(job)
                }
            }

        } catch (Exception ex) {
            System.out.println(ex)
            for (Object o : ex.getStackTrace())
                System.out.println(o.toString())
        }
        return context
    }

    private List<Job> readJobsStartedInContext(ExecutionContext context) {
        return readJobInfoFile(context) ?: readJobsFromRealJobCallsFile(context)
    }

    /**
     * Read out the initial (empty) context from storage.
     * @param api The base processing info object for the context
     * @return
     */
    ExecutionContext getInitialContext(AnalysisProcessingInformation api) {

        FileSystemAccessProvider fip = FileSystemAccessProvider.instance

        File executionDirectory = api.getExecPath()

        //Set the read time stamp before anything else. Otherwise a new Run Directory will be created!
        String[] executionContextDirName = executionDirectory.getName().split(StringConstants.SPLIT_UNDERSCORE)
        String timeStamp = executionContextDirName[1] + StringConstants.UNDERSCORE + executionContextDirName[2]
        String userID = null
        String analysisID = null
        if (executionContextDirName.length > 3) {
            //New style with additional information
            userID = executionContextDirName[3]
            analysisID = executionContextDirName[4]
        }

        ExecutionContext context = new ExecutionContext(api, InfoObject.parseTimestampString(timeStamp))

        if (userID == null)
            context.setExecutingUser(fip.getOwnerOfPath(executionDirectory))
        else
            context.setExecutingUser(userID)
        context
    }

    /**
     * Read in the real job calls file which contains a detailed description of all started jobs for a context.
     * The real job calls file is an xml file, thus the method itself is not checked on compilation.
     * @param context
     * @return
     */
    @CompileStatic(TypeCheckingMode.SKIP)  // Needed for XMLSlurper :(
    List<Job> readJobInfoFile(ExecutionContext context) {
        List<Job> jobList = []
        final File jobInfoFile = context.runtimeService.getJobInfoFile(context)
        String[] _text= FileSystemAccessProvider.instance.loadTextFile(jobInfoFile)
        if (_text == null)
            _text = new String[0]

        String text = _text.join(EMPTY)
        if (!text)
            return jobList

        NodeChild jobinfo = (NodeChild) new XmlSlurper().parseText(text)

        try {
            for (job in jobinfo.jobs.job) {
                String jobID = job.@id.text()
                String jobToolID = job.tool.@toolid.text()
                String jobToolMD5 = job.tool.@md5
                String jobName = job.@name.text()
                Map<String, String> jobsParameters = [:]
                List<LoadedFile> loadedFiles = []
                List<BEJobID> parentJobsIDs = []

                for (parameter in job.parameters.parameter) {
                    String name = parameter.@name.text()
                    String value = parameter.@value.text()
                    jobsParameters[name] = value
                }
                if(jobsParameters.containsKey(JobConstants.PRM_TOOL_ID))
                    jobToolID = jobsParameters[JobConstants.PRM_TOOL_ID]  // Override with safe value!

                for (file in job.filesbyjob.file) {
                    String fileid = file.@id.text()
                    String path = file.@path.text()
                    String clsString = file.@class.text()
                    List<LoadedFile> _parentFiles =
                            Arrays.asList((file.@parentfiles.text() as String).split(SPLIT_COMMA)).collect {
                                filename ->
                                    new LoadedFile(new File(filename), null, context,
                                            null, null)
                            }
                    LoadedFile rb = new LoadedFile(new File(path), jobID, context, _parentFiles, clsString)
                    loadedFiles << rb
                }

                for (dependency in job.dependencies.job) {
                    String id = dependency.@id.text()
                    String fileid = dependency.@fileid.text()
                    String filepath = dependency.@filepath.text()
                    parentJobsIDs << new BEJobID(id)
                }

                jobList << new LoadedJob(
                        context,
                        jobName,
                        jobID,
                        new ToolIdCommand(jobToolID, jobToolID),
                        jobsParameters,
                        loadedFiles,
                        parentJobsIDs)
            }
        } catch (Exception ex) {
            logger.warning("Could not read in xml file " + ex.toString())
            context.addErrorEntry(ExecutionContextError.READBACK_NOEXECUTEDJOBSFILE.expand(ex))
        }

        if (!jobList) {
            context.addErrorEntry(ExecutionContextError.READBACK_NOEXECUTEDJOBSFILE)
        }

        return jobList
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    String writeJobInfoFile(ExecutionContext context) {
        final File jobInfoFile = context.runtimeService.getJobInfoFile(context)
        final List<Job> executedJobs = context.executedJobs

        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)

        xml.jobinfo {
            jobs {
                for (Job ej in executedJobs) {
                    try {
                        if (ej.fakeJob) continue //Skip fake jobs.
                        job(id: ej.jobID, name: ej.jobName) {
                            String commandString =
                                    ej.runResult.beCommand != null ? ej.runResult.beCommand.toBashCommandString() : ""
                            calledcommand(command: commandString)
                            tool(id: ej.toolID, md5: ej.toolMD5)
                            parameters {
                                ej.parameters.each {
                                    String k, EscapableString v ->
                                        parameter(name: k, value: fromBash(v))
                                }
                            }
                            filesbyjob {
                                for (BaseFile bf in ej.filesToVerify) {
                                    String pfiles = bf.parentFiles.collect { BaseFile baseFile ->
                                        baseFile.absolutePath.hashCode()
                                    }.join(",")
                                    file(class: bf.class.name, id: bf.absolutePath.hashCode(),
                                            path: bf.absolutePath, parentfiles: pfiles)
                                }
                            }
                            dependendies {
                                for (BaseFile bf in ej.parentFiles) {
                                    if (bf.sourceFile)
                                        continue
                                    if (bf.creatingJobsResult == null)
                                        continue
                                    String depJobID
                                    try {
                                        depJobID = bf.creatingJobsResult.job.jobID
                                    } catch (Exception ex) {
                                        depJobID = "Error"
                                    }
                                    job(id: depJobID, fileid: bf.absolutePath.hashCode(), filepath: bf.absolutePath)
                                }
                            }
                        }
                    } catch (Exception ex) {
                        logger.severe(
                                "An error occurred, when the job info xml file was written. " + 
                                "These errors are not vital but should be handled properly", 
                                ex)
                    }
                }
            }
        }
        FileSystemAccessProvider.instance.writeTextFile(jobInfoFile, writer.toString(), context)
    }

    /**
     * Read started Jobs from the real job calls file. This is a fallback method, if readJobInfoFile does not work.
     * @param context
     * @param jobsStartedInContext
     */
    List<Job> readJobsFromRealJobCallsFile(ExecutionContext context) {
        FileSystemAccessProvider fip = FileSystemAccessProvider.instance
        String[] jobCallFileLines = fip.loadTextFile(runtimeService.getRealCallsFile(context))
        if (jobCallFileLines == null || jobCallFileLines.size() == 0) {
            context.addErrorEntry(ExecutionContextError.READBACK_NOREALJOBCALLSFILE)
        }

        List<Job> jobsStartedInContext = []

        // Create a list of all tools accessible by a short version of their folder:
        // /some/path/resources/../toolDir/tool.sh => toolDir/tool.sh
        def allTools = context.configuration.tools.allValuesAsList
        def allToolsByResourcePath = allTools.collectEntries {
            ToolEntry tool ->
                File toolPath = context.configuration.getProcessingToolPath(context, tool.id)
                [tool.id, toolPath.parentFile.name + "/" + toolPath.name]
        }

        //TODO Load a list of the previously created jobs and query those using qstat!
        for (String line : jobCallFileLines) {
            GenericJobInfo jobInfo = Roddy.getJobManager().parseGenericJobInfo(line)
            if(jobInfo == null){
                logger.severe("Skipped read-in of job call: ${line}")
                continue
            }
            // Try to find the tool id in the context. If it is not available, set "UNKNOWN"
            String toolResourcePath = Paths.get(jobInfo.tool.parentFile.name, jobInfo.tool.name)
            String toolID = allToolsByResourcePath[toolResourcePath.toString()] ?: Constants.UNKNOWN

            Job job = new Job(
                    context,
                    jobInfo.jobName,
                    new ToolIdCommand(jobInfo.tool.name),
                    jobInfo.parameters as Map<String, Object>,
                    new LinkedList<BaseFile>(),
                    new LinkedList<BaseFile>())
            jobsStartedInContext.add(job)
        }
        return jobsStartedInContext
    }

    /**
     * Read in all states from the job states logfile and set those to all jobsStartedInContext entries.
     * @param context
     * @param jobsStartedInContext
     */
    Map<String, JobState> readInJobStateLogFile(ExecutionContext context) {
        FileSystemAccessProvider fip = FileSystemAccessProvider.instance
        File jobStatesLogFile = context.getRuntimeService().getJobStateLogFile(context)
        String[] jobStateList = fip.loadTextFile(jobStatesLogFile)

        if (jobStateList == null || jobStateList.size() == 0) {
            context.addErrorEntry(ExecutionContextError.READBACK_NOJOBSTATESFILE)
            return [:]
        } else {
            //All in which were completed!
            Map<String, JobState> statusList = new LinkedHashMap<>()
            Map<String, Long> timestampList = new LinkedHashMap<>()

            for (String stateEntry : jobStateList) {
                if (stateEntry.startsWith("null"))
                    continue //Skip null:N:...
                String[] split = stateEntry.split(SPLIT_COLON)
                if (split.length < 2) continue

                String id = split[0]
                JobState status = null // TODO: Roddy.jobManager.parseJobState(split[1])
                long timestamp = 0
                if (split.length == 3)
                    timestamp = Long.parseLong(split[2])

                //Override if previous timestamp is lower or equal
                if (timestampList.get(id, 0L) <= timestamp) {
                    statusList[id] = status
                    timestampList[id] = timestamp
                }
            }

            return statusList
        }
    }
}
