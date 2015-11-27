package de.dkfz.roddy.execution.io

import de.dkfz.roddy.RunMode
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationFactory
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.config.InformationalConfigurationContent
import de.dkfz.roddy.config.ToolEntry
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextLevel
import de.dkfz.roddy.core.RuntimeService
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.execution.jobs.Command
import de.dkfz.roddy.execution.jobs.CommandFactory
import de.dkfz.roddy.execution.jobs.Job
import de.dkfz.roddy.execution.jobs.JobDependencyID
import de.dkfz.roddy.execution.jobs.JobResult
import de.dkfz.roddy.execution.jobs.JobState
import de.dkfz.roddy.execution.jobs.ProcessingCommands
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.knowledge.files.GenericFileGroup
import de.dkfz.roddy.knowledge.nativeworkflows.GenericJobInfo
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.plugins.LibrariesFactoryTest
import de.dkfz.roddy.plugins.PluginInfo
import org.junit.AfterClass
import org.junit.BeforeClass;
import org.junit.Test

import java.nio.file.CopyOption
import java.nio.file.Files;

import static org.junit.Assert.*;

/**
 * Created by heinold on 25.11.15.
 */
public class ExecutionServiceTest {
    public static ExecutionContext mockedContext;

    @BeforeClass
    public static void setupContext() {
        //Setup plugins and default configuration folders
        LibrariesFactory.initializeFactory(true)
        LibrariesFactory.getInstance().loadLibraries(LibrariesFactory.buildupPluginQueue(LibrariesFactoryTest.callLoadMapOfAvailablePlugins(), "DefaultPlugin").values() as List);
        ConfigurationFactory.initialize(LibrariesFactory.getInstance().getLoadedPlugins().collect { it -> it.getConfigurationDirectory() })

        final Configuration mockupConfig = new Configuration(new InformationalConfigurationContent(null, Configuration.ConfigurationType.OTHER, "test", "", "", null, "", ToolEntry.ResourceSetSize.l, null, null, null, null), ConfigurationFactory.getInstance().getConfiguration("default")) {
            @Override
            File getSourceToolPath(String tool) {
                if(tool == "wrapinScript")
                    return super.getSourceToolPath(tool);
                return new File("/tmp/RoddyTests/RoddyTestScript_ExecutionServiceTest.sh")
            }
        };

        final RuntimeService mockupRuntimeService = new RuntimeService() {
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
            public void releaseCache() {

            }

            @Override
            File getLoggingDirectory(ExecutionContext context) {
                return new File("/tmp/RoddyTests/logdir")
            }

            @Override
            public boolean initialize() {
                return false;
            }

            @Override
            public void destroy() {

            }

        };

        mockupConfig.getConfigurationValues().add(new ConfigurationValue(RuntimeService.RODDY_CENTRAL_EXECUTION_DIRECTORY, "/tmp/roddyCentralDirectory"));

        mockedContext = new ExecutionContext("testuser", null, null, ExecutionContextLevel.UNSET, null, null, null) {
            @Override
            public Configuration getConfiguration() {
                return mockupConfig;
            }

            @Override
            public RuntimeService getRuntimeService() {
                return mockupRuntimeService;
            }

            @Override
            Map<String, Object> getDefaultJobParameters(String TOOLID) {
                return [:];
            }

            @Override
            File getExecutionDirectory() {
                return new File("/tmp/RoddyTests/exec_dir")
            }

            @Override
            File getLoggingDirectory() {
                return new File("tmp/RoddyTests/logdir")
            }
        };
    }

    static class TestFile extends BaseFile {
        TestFile(BaseFile parentFile) {
            super(parentFile)
        }
    }

    @Test
    public void testExecuteTool() throws Exception {

        // Create a test script which outputs several file paths
        def testFolder = "/tmp/RoddyTests"
        def testScriptPrefix = testFolder + "/RoddyTestScript_ExecutionServiceTest"
        def testScriptPath = testScriptPrefix + ".sh"
        def testScriptsFolder = new File(testFolder, "exec_dir/analysisTools/RoddyTests")
        testScriptsFolder.mkdirs()
        (new File(testFolder)).mkdir()
        File testScript = new File(testScriptsFolder, "RoddyTestScript_ExecutionServiceTest.sh") // TODO This is Linux specific...!
        testScript.delete()
        testScript << """
            touch ${testScriptPrefix}_a ${testScriptPrefix}_b ${testScriptPrefix}_c ${testScriptPrefix}_d
            ls ${testScriptPrefix}_*
        """

        // Create the definition for it
        ExecutionContext context = mockedContext
        Configuration config = context.getConfiguration();

        def scriptsFolder = new File(testFolder, "exec_dir/analysisTools/roddyTools/")
        scriptsFolder.mkdirs()
        def sourceToolpath = config.getSourceToolPath( "wrapinScript");
        def targetToolpath = new File(scriptsFolder, sourceToolpath.name)
        targetToolpath.delete();
        targetToolpath << sourceToolpath.text;
        targetToolpath.setExecutable(true, true);

        ToolEntry toolEntry = new ToolEntry("RoddyTests", "RoddyTests", "RoddyTestScript_ExecutionServiceTest.sh");
        toolEntry.getOutputParameters(config).add(new ToolEntry.ToolFileGroupParameter(GenericFileGroup, [], "TEST", ToolEntry.ToolFileGroupParameter.PassOptions.parameters))
        config.getTools().add(toolEntry);

        // Initialize with fallback provider! Don't worry about errors at this point.
        FileSystemAccessProvider.initializeProvider(true);
        ExecutionService.initializeService(LocalExecutionService.class, RunMode.CLI);

        // Call it
        def executeTool = ExecutionService.getInstance().executeTool(context, "RoddyTests");

        // Check the output contents
        assert executeTool.size() == 4;
        assert executeTool == [ "${testScriptPrefix}_a", "${testScriptPrefix}_b", "${testScriptPrefix}_c", "${testScriptPrefix}_d" ]
    }
}