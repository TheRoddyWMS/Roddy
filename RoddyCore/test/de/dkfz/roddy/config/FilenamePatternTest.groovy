/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config

import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.MockupExecutionContextBuilder
import de.dkfz.roddy.execution.jobs.Command
import de.dkfz.roddy.execution.jobs.Job
import de.dkfz.roddy.execution.jobs.JobDependencyID
import de.dkfz.roddy.execution.jobs.JobManager
import de.dkfz.roddy.execution.jobs.JobResult
import de.dkfz.roddy.execution.jobs.JobState
import de.dkfz.roddy.execution.jobs.ProcessingCommands
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.knowledge.files.FileObject
import de.dkfz.roddy.knowledge.files.GenericFile
import de.dkfz.roddy.knowledge.methods.GenericMethod
import de.dkfz.roddy.knowledge.nativeworkflows.GenericJobInfo
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.plugins.LibrariesFactoryTest
import org.junit.BeforeClass;

import java.io.*

import org.junit.After
import org.junit.Test

/**
 * Created by heinold on 07.01.2016.
 */
@groovy.transform.CompileStatic
public class FilenamePatternTest {
    public static ExecutionContext mockedContext;

    private static Class testClass

    @BeforeClass
    public static void setUpMocks() throws Exception {
        //Setup plugins and default configuration folders
        LibrariesFactory.initializeFactory(true);
        LibrariesFactory.getInstance().loadLibraries(LibrariesFactory.buildupPluginQueue(LibrariesFactoryTest.callLoadMapOfAvailablePlugins(), "DefaultPlugin").values() as List);
        ConfigurationFactory.initialize(LibrariesFactory.getInstance().getLoadedPlugins().collect { it -> it.getConfigurationDirectory() })

        final Configuration mockupConfig = new Configuration(new InformationalConfigurationContent(null, Configuration.ConfigurationType.OTHER, "default", "", "", null, "", ResourceSetSize.l, null, null, null, null), ConfigurationFactory.getInstance().getConfiguration("default")) {
            @Override
            File getSourceToolPath(String tool) {
                if (tool == "wrapinScript")
                    return super.getSourceToolPath(tool);
                return new File("/tmp/RoddyTests/RoddyTestScript_ExecutionServiceTest.sh")
            }
        };

        mockedContext = MockupExecutionContextBuilder.createSimpleContext(FilenamePatternTest, mockupConfig);

        testClass = LibrariesFactory.getInstance().loadRealOrSyntheticClass("FilenamePatternTest_testFilenamePatternWithSelectionByToolID", BaseFile.class as Class<FileObject>);

        ToolEntry toolEntry = new ToolEntry("RoddyTests", "RoddyTests", "RoddyTestScript_ExecutionServiceTest.sh");
        toolEntry.getOutputParameters(mockupConfig).add(new ToolFileParameter(testClass, null, "TEST", new ToolFileParameterCheckCondition(true)))

        mockupConfig.getTools().add(toolEntry);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testFilenamePatternWithSelectionByToolID() {
        FilenamePattern fp = new OnToolFilenamePattern(testClass, "RoddyTests", "/tmp/RoddyTests/testFileResult.sh", "default");
        BaseFile.ConstructionHelperForGenericCreation helper = new BaseFile.ConstructionHelperForGenericCreation(mockedContext, mockedContext.getConfiguration().getTools().getValue("RoddyTests"), "RoddyTests", null, null, new TestFileStageSettings(), null);
        String filename = fp.apply((BaseFile) testClass.newInstance(helper));
        assert filename == "/tmp/RoddyTests/testFileResult.sh";
    }

    @Test
    public void testFilenamePatternWithDerivedParentClass() {
        assert false
    }

    @Test
    public void testFilenamePatternWithSelectionByMethod() {
        assert false
    }

    @Test
    public void testFilenamePatternWithFileStage() {
        assert false
    }

    @Test
    void testFilenamePatternsForFileGroupWithNumericIndices() {

    }

    @Test
    void testFilenamePatternsForFileGroupWithStringIndices() {

    }

    @Test
    void testComplexFilenamePattern() {
        assert false
    }

    @Test
    public void testJobCreationWithFileUsingToolIDForNamePattern() {
        JobManager.initializeFactory(new JobManager() {
            @Override
            void createUpdateDaemonThread(int interval) {

            }

            @Override
            Command createCommand(GenericJobInfo jobInfo) {
                return null
            }

            @Override
            Command createCommand(Job job, ExecutionContext run, String jobName, List processingCommands, File tool, Map parameters, List dependencies, List arraySettings) {
                return null
            }

            @Override
            JobDependencyID createJobDependencyID(Job job, String jobResult) {
                return null
            }

            @Override
            ProcessingCommands convertResourceSet(Configuration configuration, ResourceSet resourceSet) {
                return null
            }

            @Override
            ProcessingCommands parseProcessingCommands(String alignmentProcessingOptions) {
                return null
            }

            @Override
            ProcessingCommands getProcessingCommandsFromConfiguration(Configuration configuration, String toolID) {
                return new ProcessingCommands() {}
            }

            @Override
            ProcessingCommands extractProcessingCommandsFromToolScript(File file) {
                return null
            }

            @Override
            Job parseToJob(ExecutionContext executionContext, String commandString) {
                return null
            }

            @Override
            GenericJobInfo parseGenericJobInfo(ExecutionContext context, String command) {
                return null
            }

            @Override
            JobResult convertToArrayResult(Job arrayChildJob, JobResult parentJobsResult, int arrayIndex) {
                return null
            }

            @Override
            Map<String, JobState> queryJobStatus(List jobIDs) {
                return null
            }

            @Override
            void queryJobAbortion(List executedJobs) {

            }

            @Override
            void addJobStatusChangeListener(Job job) {

            }

            @Override
            String getLogFileWildcard(Job job) {
                return null
            }

            @Override
            boolean compareJobIDs(String jobID, String id) {
                return false
            }

            @Override
            String[] peekLogFile(Job job) {
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
        })

        FilenamePattern fp = new OnToolFilenamePattern(testClass, "RoddyTests", "/tmp/RoddyTests/testFileResult.sh", "default");
        mockedContext.getConfiguration().getFilenamePatterns().add(fp);
        BaseFile sourceFile = BaseFile.constructSourceFile(GenericFile, new File("/tmp/RoddyTests/output/abcfile"), mockedContext);
        BaseFile result = (BaseFile) GenericMethod.callGenericTool("RoddyTests", sourceFile);
        assert result != null
        assert result.class == testClass;
    }

    @Test
    public testExtractCommand() {

    }

    @Test
    public testExtractCommands() {

    }
}
