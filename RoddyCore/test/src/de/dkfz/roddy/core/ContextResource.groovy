/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core

import de.dkfz.roddy.Constants
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.SystemProperties
import de.dkfz.roddy.config.*
import de.dkfz.roddy.execution.io.ExecutionResult
import de.dkfz.roddy.execution.io.NoNoExecutionService
import de.dkfz.roddy.execution.jobs.*
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.knowledge.files.GenericFile
import groovy.transform.CompileStatic
import org.junit.rules.ExternalResource
import org.junit.rules.TemporaryFolder

import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.TimeoutException

@CompileStatic
class TestWorkflow extends Workflow {

    @Override
    boolean execute(ExecutionContext context) throws ConfigurationError {
        return true
    }

}

@CompileStatic
class ContextResource extends ExternalResource {

    static final String DIR_PREFIX = 'RoddyTests'

    public TemporaryFolder tempFolder = new TemporaryFolder(new File ('/tmp'))

    public File tempDir

    @Override
    protected void before() throws Throwable {
        tempFolder.create()
        tempDir = tempFolder.newFolder(DIR_PREFIX)
        Roddy.applicationConfiguration.getOrSetApplicationProperty(Constants.APP_PROPERTY_SCRATCH_BASE_DIRECTORY, tempDir.toString())
    }

    @Override
    protected void after() {
        tempFolder.delete()
    }

    File createTempdir(String name) {
        assert tempDir != null
        File dir = new File (tempDir, name)
        dir.mkdirs()
        return dir
    }

    File getTestBaseDirectory(String testID) {
        return createTempdir(testID)
    }

    File getDirectory(String testID, String id) {
        return createTempdir(Paths.get(testID, id).toString())
    }

    File getTestLoggingDirectory(String testID) {
        return getDirectory(testID, 'logdir')
    }

    File getTestExecutionDirectory(String testID) {
        return getDirectory(testID, 'exec_dir')
    }

    File getTestOutputDirectory(String testID) {
        return getDirectory(testID, 'output')
    }

    File getTestInputDirectory(String testID) {
        return getDirectory(testID, 'input')
    }

    RuntimeService createSimpleRuntimeService(final String testClassName) {
        return new RuntimeService() {
            @Override
            Map<String, Object> getDefaultJobParameters(ExecutionContext context, String TOOLID) {
                return null
            }

            @Override
            String createJobName(ExecutionContext executionContext, BaseFile file, String TOOLID, boolean reduceLevel) {
                return null
            }

            @Override
            boolean isFileValid(BaseFile baseFile) {
                return false
            }

            @Override
            File getLoggingDirectory(ExecutionContext context) {
                return getTestLoggingDirectory(testClassName)
            }

            @Override
            File getAnalysisToolsDirectory(ExecutionContext executionContext) {
                return new File("/some/analysisToolsDirectory")
            }

        }
    }

    ExecutionContext createSimpleContext(
            final Class testClass,
            final Configuration testConfig = new Configuration(null),
            final RuntimeService testRuntimeService = createSimpleRuntimeService(testClass.name)) {
        return createSimpleContext(testClass.name, testConfig, testRuntimeService)
    }

    ExecutionContext createSimpleContext(
            final String testID,
            final Configuration testConfig = new Configuration(null), final RuntimeService testRuntimeService = createSimpleRuntimeService(testID)) {

        final File testInputDirectory = getTestInputDirectory(testID)
        final File testOutputDirectory = getTestOutputDirectory(testID)
        final File testExecutionDirectory = getTestExecutionDirectory(testID)
        final File testLoggingDirectory = getTestLoggingDirectory(testID)

        for (File f : [testInputDirectory, testOutputDirectory, testExecutionDirectory, testLoggingDirectory]) {
            if (!f.exists()) f.mkdirs()
        }

        ExecutionContext result
        if (testConfig) {

            final PreloadedConfiguration projectPreloadConfig =
                    new PreloadedConfiguration(null, Configuration.ConfigurationType.PROJECT, 'TestProject', '',
                            '', null, '', ResourceSetSize.l, null, [], null, '')

            final ProjectConfiguration projectConfig =
                    new ProjectConfiguration(projectPreloadConfig, testRuntimeService.getClass().toString(), [:], testConfig)

            final Project project = new Project(projectConfig, testRuntimeService, null, null)

            final PreloadedConfiguration analysisPreloadConfig =
                    new PreloadedConfiguration(null, Configuration.ConfigurationType.ANALYSIS, 'TestAnalysis', '',
                            '', null, '', ResourceSetSize.l, null, [], null, '')

            final AnalysisConfiguration analysisConfig = new AnalysisConfiguration(analysisPreloadConfig,
                    'de.dkfz.roddy.core.TestWorkflow',
                    testRuntimeService.getClass().toString(), null, [], [], '')

            final Analysis analysis = new Analysis('Test', project, testRuntimeService, analysisConfig)

            final DataSet dataSet = new DataSet(analysis, 'TEST_PID', getTestOutputDirectory('TEST_PID'))

            result = new ExecutionContext(SystemProperties.userName, analysis, dataSet, ExecutionContextLevel.UNSET,
                    testOutputDirectory, testInputDirectory, testExecutionDirectory, System.nanoTime())

        } else {
            result = new ExecutionContext(SystemProperties.userName, null, null, ExecutionContextLevel.UNSET,
                    testOutputDirectory, testInputDirectory, testExecutionDirectory, System.nanoTime())
        }
        return result
    }

    static BatchEuphoriaJobManager createMockupJobManager() {
        new BatchEuphoriaJobManager(new NoNoExecutionService(), JobManagerOptions.create().build()) {

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

            Command createCommand(BEJob beJob) {
                return null
            }

            @Override
            protected Map<BEJobID, JobState> queryJobStates(List list,
                                                            Duration timeout = Duration.ZERO) {
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
            Map<BEJobID, GenericJobInfo> queryExtendedJobStateById(List list,
                                                                   Duration timeout = Duration.ZERO) {
                return null
            }

            @Override
            protected JobState parseJobState(String s) {
                return JobState.DUMMY
            }

            @Override
            String getQueryJobStatesCommand() {
                return null
            }

            @Override
            String getExtendedQueryJobStatesCommand() {
                return null
            }
        }
    }

    static BaseFile makeTestBaseFileInstance(ExecutionContext context, String baseName) {

        FilenamePattern fp =
                new OnScriptParameterFilenamePattern(GenericFile as Class<BaseFile>, baseName, baseName,
                        "/tmp/$baseName", FilenamePattern.DEFAULT_SELECTIONTAG)
        context.configuration.filenamePatterns.add(fp)
        return new GenericFile(new BaseFile.ConstructionHelperForManualCreation(
                context,
                new ToolEntry(baseName, '/tmp', baseName),
                baseName, baseName, null, null, null))
    }

    static BaseFile makeTestBaseFileInstance(String baseName, List<BaseFile> parentFiles) {
        assert parentFiles.size() > 0
        FilenamePattern fp =
                new OnScriptParameterFilenamePattern(GenericFile as Class<BaseFile>, baseName, baseName,
                        "/tmp/$baseName", FilenamePattern.DEFAULT_SELECTIONTAG)
        parentFiles[0].executionContext.configuration.filenamePatterns.add(fp)
        return new GenericFile(new BaseFile.ConstructionHelperForManualCreation(
                parentFiles[0],
                parentFiles,
                new ToolEntry(baseName, '/tmp', baseName),
                baseName, baseName, null, '1', null, null))
    }
}
