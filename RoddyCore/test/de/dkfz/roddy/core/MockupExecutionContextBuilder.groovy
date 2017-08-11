/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core

import de.dkfz.roddy.config.ResourceSet
import de.dkfz.roddy.execution.jobs.BEJob
import de.dkfz.roddy.execution.jobs.BEJobResult
import de.dkfz.roddy.execution.jobs.Command
import de.dkfz.roddy.execution.jobs.GenericJobInfo
import de.dkfz.roddy.execution.jobs.BEJobID
import de.dkfz.roddy.execution.jobs.BatchEuphoriaJobManager
import de.dkfz.roddy.execution.jobs.JobManagerCreationParametersBuilder
import de.dkfz.roddy.execution.jobs.JobState
import de.dkfz.roddy.execution.jobs.ProcessingCommands
import de.dkfz.roddy.config.AnalysisConfiguration;
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ProjectConfiguration
import de.dkfz.roddy.execution.io.NoNoExecutionService
import de.dkfz.roddy.execution.jobs.JobResult
import de.dkfz.roddy.knowledge.files.BaseFile

/**
 * Created by heinold on 01.07.16.
 */
@groovy.transform.CompileStatic
public class MockupExecutionContextBuilder {

    public static final String DIR_PREFIX = "RoddyTests_";

    public static File getTestBaseDirectory(String testID) {
        final File testBaseDirectory = File.createTempDir(DIR_PREFIX, "_" + testID)
        testBaseDirectory.deleteOnExit();
        testBaseDirectory
    }

    public static File getDirectory(String testID, String id) {
        def file = new File(getTestBaseDirectory(testID), id)
        if(!file.exists()) file.mkdirs();
        file.deleteOnExit();
        return file;
    }

    public static File getTestLoggingDirectory(String testID) {
        return getDirectory(testID, "logdir");
    }

    public static File getTestExecutionDirectory(String testID) {
        return getDirectory(testID, "exec_dir");
    }

    public static File getTestOutputDirectory(String testID) {
        return getDirectory(testID, "output");
    }

    public static File getTestInputDirectory(String testID) {
        return getDirectory(testID, "input");
    }

    public static RuntimeService createSimpleRuntimeService(final String testClassName) {
        return new RuntimeService() {
            @Override
            public Map<String, Object> getDefaultJobParameters(ExecutionContext context, String TOOLID) {
                return null;
            }

            @Override
            public String createJobName(ExecutionContext executionContext, BaseFile file, String TOOLID, boolean reduceLevel) {
                return null;
            }

            @Override
            public boolean isFileValid(BaseFile baseFile) {
                return false;
            }

            @Override
            File getLoggingDirectory(ExecutionContext context) {
                return getTestLoggingDirectory(testClassName)
            }

        }
    }

    public static ExecutionContext createSimpleContext(final Class testClass, final Configuration testConfig = new Configuration(null), final RuntimeService testRuntimeService = createSimpleRuntimeService(testClass.name)) {
        return createSimpleContext(testClass.name, testConfig, testRuntimeService);
    }

    public static ExecutionContext createSimpleContext(final String testID, final Configuration testConfig = new Configuration(null), final RuntimeService testRuntimeService = createSimpleRuntimeService(testID)) {

        final File testInputDirectory = getTestInputDirectory(testID)
        final File testOutputDirectory = getTestOutputDirectory(testID)
        final File testExecutionDirectory = getTestExecutionDirectory(testID)
        final File testLoggingDirectory = getTestLoggingDirectory(testID)

        for (File f : [testInputDirectory, testOutputDirectory, testExecutionDirectory, testLoggingDirectory]) {
            if (!f.exists()) f.mkdirs();
        }
        final Project project = new Project(new ProjectConfiguration(null, null, null, testConfig), testRuntimeService, null, null) {
            @Override
            String getName() {
                return "TestProject";
            }
        }
        final Analysis analysis = new Analysis("Test", project, null, null, new AnalysisConfiguration(null, "", "", null, null, null, null))

        return new ExecutionContext(System.getProperty("user.name"), analysis, null, ExecutionContextLevel.UNSET, testOutputDirectory, testInputDirectory, testExecutionDirectory, System.nanoTime(), true) {
            @Override
            public Configuration getConfiguration() {
                return testConfig;
            }

            @Override
            public RuntimeService getRuntimeService() {
                return testRuntimeService;
            }

            @Override
            DataSet getDataSet() {
                DataSet ds = new DataSet(getAnalysis(), "TEST_PID", new File(getOutputDirectory(), "TEST_PID"));
                return ds;
            }

            @Override
            Map<String, Object> getDefaultJobParameters(String TOOLID) {
                return [:];
            }

            @Override
            File getLoggingDirectory() {
                return testLoggingDirectory
            }

            @Override
            public String toString() {
                return "TestContext";
            }
        };
    }

    public static BatchEuphoriaJobManager createMockupJobManager() {
        new BatchEuphoriaJobManager(new NoNoExecutionService(), new JobManagerCreationParametersBuilder().setCreateDaemon(false).build()) {
            @Override
            Command createCommand(GenericJobInfo genericJobInfo) {
                return null
            }

            @Override
            JobResult runJob(BEJob job) {
                return null
            }

            @Override
            BEJobID createJobID(BEJob job, String s) {
                return null
            }

            @Override
            ProcessingCommands convertResourceSet(ResourceSet resourceSet) {
                return null
            }

            @Override
            ProcessingCommands parseProcessingCommands(String s) {
                return null
            }

            @Override
            ProcessingCommands extractProcessingCommandsFromToolScript(File file) {
                return null
            }

            @Override
            BEJob parseToJob(String s) {
                return null
            }

            @Override
            GenericJobInfo parseGenericJobInfo(String s) {
                return null
            }

            @Override
            BEJobResult convertToArrayResult(BEJob arrayChildJob, BEJobResult parentJobsResult, int arrayIndex) {
                return null
            }

            @Override
            void updateJobStatus() {

            }

            @Override
            Map<BEJob, GenericJobInfo> queryExtendedJobState(List list, boolean forceUpdate) {
                return null
            }

            @Override
            void addJobStatusChangeListener(BEJob job) {

            }

            @Override
            String getLogFileWildcard(BEJob job) {
                return null
            }

            @Override
            boolean compareJobIDs(String s, String s1) {
                return false
            }

            @Override
            String getStringForQueuedJob() {
                return null
            }

            @Override
            String getStringForJobOnHold() {
                return null
            }

            @Override
            String getStringForRunningJob() {
                return null
            }

            @Override
            String getSpecificJobIDIdentifier() {
                return null
            }

            @Override
            String getSpecificJobArrayIndexIdentifier() {
                return null
            }

            @Override
            String getSpecificJobScratchIdentifier() {
                return null
            }

            @Override
            void queryJobAbortion(List list) {

            }

            @Override
            Map<String, JobState> queryJobStatus(List list) {
                return null
            }

            @Override
            Command createCommand(BEJob job, String s, List list, File file, Map map, List list1) {
                return null
            }

            @Override
            String[] peekLogFile(BEJob job) {
                return new String[0]
            }

            @Override
            String parseJobID(String commandOutput) {
                return null
            }

            @Override
            String getSubmissionCommand() {
                return null
            }

            @Override
            File getLoggingDirectoryForJob(BEJob job) {
                return null
            }

            @Override
            Map<BEJob, JobState> queryJobStatus(List list, boolean forceUpdate) {
                return null
            }

            @Override
            JobState parseJobState(String stateString) {
                return null
            }

            @Override
            Map<String, GenericJobInfo> queryExtendedJobStateById(List<String> jobIds, boolean forceUpdate) {
                return null
            }

            @Override
            Map<String, JobState> queryJobStatusAll(boolean forceUpdate = false) {
                return null
            }

            @Override
            Map<String, JobState> queryJobStatusById(List<String> jobIds, boolean forceUpdate = false) {
                return null
            }
        }
    }
}
