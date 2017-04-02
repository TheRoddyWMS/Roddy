/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config

import de.dkfz.eilslabs.batcheuphoria.config.ResourceSet
import de.dkfz.eilslabs.batcheuphoria.config.ResourceSetSize
import de.dkfz.eilslabs.batcheuphoria.jobs.GenericJobInfo
import de.dkfz.eilslabs.batcheuphoria.jobs.Job
import de.dkfz.eilslabs.batcheuphoria.jobs.JobManagerCreationParametersBuilder
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.MockupExecutionContextBuilder
import de.dkfz.eilslabs.batcheuphoria.jobs.Command
import de.dkfz.roddy.execution.io.NoNoExecutionService
import de.dkfz.eilslabs.batcheuphoria.jobs.JobDependencyID
import de.dkfz.eilslabs.batcheuphoria.jobs.JobManager
import de.dkfz.eilslabs.batcheuphoria.jobs.JobState;
import de.dkfz.eilslabs.batcheuphoria.jobs.ProcessingCommands
import de.dkfz.roddy.execution.jobs.JobResult
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.knowledge.files.FileObject
import de.dkfz.roddy.knowledge.files.GenericFile
import de.dkfz.roddy.knowledge.methods.GenericMethod
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.plugins.LibrariesFactoryTest
import org.junit.BeforeClass

import java.io.*

import org.junit.After
import org.junit.Test

/**
 * Created by heinold on 07.01.2016.
 */
@groovy.transform.CompileStatic
class FilenamePatternTest {
    public static ExecutionContext mockedContext

    private static Class testClass

    @BeforeClass
    static void setUpMocks() throws Exception {
        //Setup plugins and default configuration folders
        LibrariesFactory.initializeFactory(true)
        LibrariesFactory.getInstance().loadLibraries(LibrariesFactory.buildupPluginQueue(LibrariesFactoryTest.callLoadMapOfAvailablePlugins(), "DefaultPlugin").values() as List)
        ConfigurationFactory.initialize(LibrariesFactory.getInstance().getLoadedPlugins().collect { it -> it.getConfigurationDirectory() })

        final Configuration mockupConfig = new Configuration(new InformationalConfigurationContent(null, Configuration.ConfigurationType.OTHER, "default", "", "", null, "", ResourceSetSize.l, null, null, null, null), ConfigurationFactory.getInstance().getConfiguration("default")) {
            @Override
            File getSourceToolPath(String tool) {
                if (tool == "wrapinScript")
                    return super.getSourceToolPath(tool)
                return new File("/tmp/RoddyTests/RoddyTestScript_ExecutionServiceTest.sh")
            }
        }

        mockedContext = MockupExecutionContextBuilder.createSimpleContext(FilenamePatternTest, mockupConfig)

        testClass = LibrariesFactory.getInstance().loadRealOrSyntheticClass("FilenamePatternTest_testFilenamePatternWithSelectionByToolID", BaseFile.class as Class<FileObject>)

        ToolEntry toolEntry = new ToolEntry("RoddyTests", "RoddyTests", "RoddyTestScript_ExecutionServiceTest.sh")
        toolEntry.getOutputParameters(mockupConfig).add(new ToolFileParameter(testClass, null, "TEST", new ToolFileParameterCheckCondition(true)))

        mockupConfig.getTools().add(toolEntry)
    }

    @After
    void tearDown() throws Exception {

    }

    @Test
    void testFilenamePatternWithSelectionByToolID() {
        FilenamePattern fp = new OnToolFilenamePattern(testClass, "RoddyTests", "/tmp/RoddyTests/testFileResult.sh", "default")
        BaseFile.ConstructionHelperForGenericCreation helper = new BaseFile.ConstructionHelperForGenericCreation(mockedContext, mockedContext.getConfiguration().getTools().getValue("RoddyTests"), "RoddyTests", null, null, new TestFileStageSettings(), null)
        String filename = fp.apply((BaseFile) testClass.newInstance(helper))
        assert filename == "/tmp/RoddyTests/testFileResult.sh"
    }

    @Test
    void testFilenamePatternWithDerivedParentClass() {
        assert false
    }

    @Test
    void testFilenamePatternWithSelectionByMethod() {
        assert false
    }

    @Test
    void testFilenamePatternWithFileStage() {
        assert false
    }

    @Test
    void testFilenamePatternsForFileGroupWithNumericIndices() {
        assert false
    }

    @Test
    void testFilenamePatternsForFileGroupWithStringIndices() {
        assert false
    }

    @Test
    void testComplexFilenamePattern() {
        assert false
    }

    @Test
    public void testJobCreationWithFileUsingToolIDForNamePattern() {
        new JobManager(new NoNoExecutionService(), new JobManagerCreationParametersBuilder().setCreateDaemon(false).build()) {
            @Override
            Command createCommand(de.dkfz.eilslabs.batcheuphoria.jobs.GenericJobInfo genericJobInfo) {
                return null
            }

            @Override
            JobResult runJob(de.dkfz.eilslabs.batcheuphoria.jobs.Job job, boolean b) {
                return null
            }

            @Override
            JobDependencyID createJobDependencyID(de.dkfz.eilslabs.batcheuphoria.jobs.Job job, String s) {
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
            de.dkfz.eilslabs.batcheuphoria.jobs.Job parseToJob(String s) {
                return null
            }

            @Override
            de.dkfz.eilslabs.batcheuphoria.jobs.GenericJobInfo parseGenericJobInfo(String s) {
                return null
            }

            @Override
            JobResult convertToArrayResult(de.dkfz.eilslabs.batcheuphoria.jobs.Job job, JobResult jobResult, int i) {
                return null
            }

            @Override
            void updateJobStatus() {

            }

            @Override
            Map<Job, GenericJobInfo> queryExtendedJobState(List list, boolean forceUpdate) {
                return null
            }

            @Override
            void addJobStatusChangeListener(de.dkfz.eilslabs.batcheuphoria.jobs.Job job) {

            }

            @Override
            String getLogFileWildcard(de.dkfz.eilslabs.batcheuphoria.jobs.Job job) {
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
            Command createCommand(de.dkfz.eilslabs.batcheuphoria.jobs.Job job, String s, List list, File file, Map map, List list1, List list2) {
                return null
            }

            @Override
            String[] peekLogFile(de.dkfz.eilslabs.batcheuphoria.jobs.Job job) {
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
            Map<Job, JobState> queryJobStatus(List list, boolean forceUpdate) {
                return null
            }
        }

        FilenamePattern fp = new OnToolFilenamePattern(testClass, "RoddyTests", "/tmp/RoddyTests/testFileResult.sh", "default")
        mockedContext.getConfiguration().getFilenamePatterns().add(fp)
        BaseFile sourceFile = BaseFile.constructSourceFile(GenericFile, new File("/tmp/RoddyTests/output/abcfile"), mockedContext)
        BaseFile result = (BaseFile) GenericMethod.callGenericTool("RoddyTests", sourceFile)
        assert result != null
        assert result.class == testClass
    }

    @Test
    void testExtractCommand() {
        assert false
    }

    @Test
    void testExtractCommands() {
        assert false
    }

    @Test
    void testFillFileGroupIndex() {
        String src = 'abc_${fgindex}_def'
        def parentFile = new GenericFile(new BaseFile.ConstructionHelperForManualCreation(mockedContext, null, null, null, null, null, null));
        def file = new GenericFile(new BaseFile.ConstructionHelperForManualCreation(parentFile, null, null, null, null, null, "1", null, null));
        file.hasIndexInFileGroup()
        assert createFilenamePattern().fillFileGroupIndex(src, file) == 'abc_1_def'
    }

    @Test
    void testFillDirectories() {
        assert false
    }

    @Test
    void testFillConfigurationVariable() {
        String srcFull = 'something_${avalue}_${cvalue,name="anothervalue"}_${cvalue,name="unknown",default="bebe"}'
        ExecutionContext context = createMockupContext()
        FilenamePattern fpattern = createFilenamePattern()
        assert fpattern.fillConfigurationVariables(srcFull, context) == 'something_abc_abc_bebe'
    }

    @Test
    void testFillWithUnsetVariables() {
        String srcFull = 'something_${avalue}_${cvalue,name="anothervalue"}_${cvalue,name="unknown"}'
        ExecutionContext context = createMockupContext()
        FilenamePattern fpattern = createFilenamePattern()
        assert fpattern.fillConfigurationVariables(srcFull, context) == 'something_abc_abc_${cvalue}'
    }

    @Test
    void testWithDataSetID() {
        String srcFull = 'something_${avalue}_${cvalue,name="unknown"}_${pid}_${fileStageID[0]}'
        ExecutionContext context = createMockupContext()
        FilenamePattern fpattern = createFilenamePattern()
        assert fpattern.fillConfigurationVariables(srcFull, context) == 'something_abc_${cvalue}_${pid}_${fileStageID[0]}'
    }

    private FilenamePattern createFilenamePattern() {
        String srcFull
        def fpattern = new FilenamePattern(LibrariesFactory.getInstance().loadRealOrSyntheticClass("FPTTestClass", "BaseFile"), srcFull, "default") {

            @Override
            String getID() { return null }

            @Override
            FilenamePatternDependency getFilenamePatternDependency() { return null }
        }
        fpattern
    }

    private ExecutionContext createMockupContext() {
        Configuration cfg = new Configuration(null);
        cfg.configurationValues << new ConfigurationValue(cfg, "avalue", "abc")
        cfg.configurationValues << new ConfigurationValue(cfg, "anothervalue", "abc")
        def context = MockupExecutionContextBuilder.createSimpleContext(getClass(), cfg)
        context
    }
}
