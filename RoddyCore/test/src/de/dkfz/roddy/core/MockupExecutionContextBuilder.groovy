/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core

import de.dkfz.roddy.config.AnalysisConfiguration
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationError
import de.dkfz.roddy.config.PreloadedConfiguration
import de.dkfz.roddy.config.ProjectConfiguration
import de.dkfz.roddy.config.ResourceSet
import de.dkfz.roddy.config.ResourceSetSize
import de.dkfz.roddy.execution.io.ExecutionResult
import de.dkfz.roddy.execution.io.NoNoExecutionService
import de.dkfz.roddy.execution.jobs.*
import de.dkfz.roddy.knowledge.files.BaseFile
import groovy.transform.CompileStatic

import java.util.concurrent.TimeoutException

/**
 * Created by heinold on 01.07.16.
 */
@groovy.transform.CompileStatic
class TestWorkflow extends Workflow {

    @Override
    boolean execute(ExecutionContext context) throws ConfigurationError {
        return true
    }

}

@CompileStatic
public class MockupExecutionContextBuilder {

    public static final String DIR_PREFIX = "RoddyTests_"

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
        return createSimpleContext(testClass.name, testConfig, testRuntimeService)
    }

    public static ExecutionContext createSimpleContext(final String testID, final Configuration testConfig = new Configuration(null), final RuntimeService testRuntimeService = createSimpleRuntimeService(testID)) {

        final File testInputDirectory = getTestInputDirectory(testID)
        final File testOutputDirectory = getTestOutputDirectory(testID)
        final File testExecutionDirectory = getTestExecutionDirectory(testID)
        final File testLoggingDirectory = getTestLoggingDirectory(testID)

        for (File f : [testInputDirectory, testOutputDirectory, testExecutionDirectory, testLoggingDirectory]) {
            if (!f.exists()) f.mkdirs()
        }

        if (testConfig) {

            final PreloadedConfiguration projectPreloadConfig =
                    new PreloadedConfiguration(null, Configuration.ConfigurationType.PROJECT, "TestProject", "",
                            "", null, "", ResourceSetSize.l, null, [], null, "")

            final ProjectConfiguration projectConfig =
                    new ProjectConfiguration(projectPreloadConfig, testRuntimeService.getClass().toString(), [:], testConfig)

            final Project project = new Project(projectConfig, testRuntimeService, null, null)

            final PreloadedConfiguration analysisPreloadConfig =
                    new PreloadedConfiguration(null, Configuration.ConfigurationType.ANALYSIS, "TestAnalysis", "",
                            "", null, "", ResourceSetSize.l, null, [], null, "")

            final AnalysisConfiguration analysisConfig = new AnalysisConfiguration(analysisPreloadConfig,
                    "de.dkfz.roddy.core.TestWorkflow",
                    testRuntimeService.getClass().toString(), null, [], [], "")

            final Analysis analysis = new Analysis("Test", project, new RuntimeService(), analysisConfig)

            final DataSet dataSet = new DataSet(analysis, "TEST_PID", getTestOutputDirectory("TEST_PID"))

            return new ExecutionContext(System.getProperty("user.name"), analysis, dataSet, ExecutionContextLevel.UNSET,
                    testOutputDirectory, testInputDirectory, testExecutionDirectory, System.nanoTime())

        } else {
            return new ExecutionContext(System.getProperty("user.name"), null, null, ExecutionContextLevel.UNSET,
                                        testOutputDirectory, testInputDirectory, testExecutionDirectory, System.nanoTime())
        }
    }

    static BatchEuphoriaJobManager createMockupJobManager() {
        new BatchEuphoriaJobManager(new NoNoExecutionService(), JobManagerOptions.create().setStrictMode(false).build()) {

            @Override
            BEJobResult submitJob(BEJob job) throws TimeoutException {
                return null
            }

            @Override
            protected ExecutionResult executeStartHeldJobs(List list) {
                return null
            }

            @Override
            ProcessingParameters convertResourceSet(BEJob job, ResourceSet resourceSet) {
                return null
            }

            @Override
            GenericJobInfo parseGenericJobInfo(String s) {
                return null
            }

            @Override

            protected Command createCommand(BEJob beJob) {
                return null
            }

            @Override
            protected Map<BEJobID, JobState> queryJobStates(List list) {
                return null
            }

            @Override
            void addToListOfStartedJobs(BEJob job) {

            }

            @Override
            String getJobIdVariable() {
                return null
            }

            @Override
            String getJobNameVariable() {
                return null
            }

            @Override

            String getQueueVariable() {
                return null
            }


            @Override
            String getNodeFileVariable() {
                return null
            }

            @Override
            String getSubmitHostVariable() {
                return null
            }

            @Override
            String getSubmitDirectoryVariable() {
                return null
            }

            @Override
            protected ExecutionResult executeKillJobs(List list) {
                return null
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
            Map<BEJobID, GenericJobInfo> queryExtendedJobStateById(List list) {
                return null
            }
        }
    }
}
