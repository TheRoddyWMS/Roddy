package de.dkfz.roddy.execution.io

import de.dkfz.roddy.RunMode
import de.dkfz.roddy.config.*
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextLevel
import de.dkfz.roddy.core.MockupExecutionContextBuilder
import de.dkfz.roddy.core.RuntimeService
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.LocalExecutionService
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.knowledge.files.GenericFileGroup
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.plugins.LibrariesFactoryTest
import de.dkfz.roddy.plugins.PluginInfo
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import org.junit.BeforeClass
import org.junit.Test

/**
 * Created by kaercher on 05.07.16.
 */
public class ExecutionServiceTestInlineScript {


    public static ExecutionContext mockedContext;

    @BeforeClass
    public static void setupContext() {
        //Setup plugins and default configuration folders
        LibrariesFactory.initializeFactory(true)
        LibrariesFactory.getInstance().loadLibraries(LibrariesFactory.buildupPluginQueue(LibrariesFactoryTest.callLoadMapOfAvailablePlugins(), "DefaultPlugin").values() as List);
        ConfigurationFactory.initialize(LibrariesFactory.getInstance().getLoadedPlugins().collect { it -> it.getConfigurationDirectory() })

        final Configuration mockupConfig = new Configuration(new InformationalConfigurationContent(null, Configuration.ConfigurationType.OTHER, "test", "", "", null, "", ResourceSetSize.l, null, null, null, null), ConfigurationFactory.getInstance().getConfiguration("default"))

        mockedContext = MockupExecutionContextBuilder.createSimpleContext(ExecutionServiceTest, mockupConfig);
    }


    @Test
    public void testWriteInlineScriptsAndCompressToolFolders() {
        // Create the definition for it
        Map<File, PluginInfo> sourcePaths = [:];
        for (PluginInfo pluginInfo : LibrariesFactory.getInstance().getLoadedPlugins()) {
            pluginInfo.getToolsDirectories().values().each { sourcePaths[it] = pluginInfo }
        }

        Map<File, PluginInfo> listOfFolders = sourcePaths.findAll { File it, PluginInfo pInfo -> !it.getName().contains(".svn"); }
        Configuration cfg = mockedContext.getConfiguration();
        ExecutionService.initializeService(LocalExecutionService.class, RunMode.CLI);

        ToolEntry toolEntry = new ToolEntry("roddyTests", "roddyTests", "");
        toolEntry.getOutputParameters(cfg).add(new ToolEntry.ToolFileGroupParameter(GenericFileGroup, null, [], "TEST", ToolEntry.ToolFileGroupParameter.PassOptions.parameters))
        def inlineScriptName = "testInlineScript.sh"
        toolEntry.setInlineScriptName(inlineScriptName)
        toolEntry.setInlineScript("echo test")
        cfg.getTools().add(toolEntry);

        Map<String, List<Map<String,String>>> mapOfInlineScripts = [:]

        for (ToolEntry tool in cfg.getTools().allValuesAsList) {
            if (tool.hasInlineScript()) {
                mapOfInlineScripts.get(tool.basePathId, []) << ["inlineScript":tool.getInlineScript(),"inlineScriptName":tool.getInlineScriptName()]
            }
        }

        ExecutionService.getInstance().writeInlineScriptsAndCompressToolFolders(listOfFolders,mapOfInlineScripts)

        assert  listOfFolders.size() == 2
        boolean hasCompressedFolder = false
        listOfFolders.each {
            File subFolder, PluginInfo pInfo ->
                File localCompressedFolder = ExecutionService.getInstance().mapOfPreviouslyCompressedArchivesByFolder[subFolder].localArchive;
                assert localCompressedFolder != null
                if (localCompressedFolder.name.equals("cTools_DefaultPlugin:current_roddyTests.zip")){
                    hasCompressedFolder = true
                    boolean hasInlineScript = false
                    File tempFolder = File.createTempDir();
                    tempFolder.deleteOnExit()
                    GString str = RoddyIOHelperMethods.getCompressor().getDecompressionString(localCompressedFolder, null, tempFolder);
                    ExecutionService.getInstance().execute(str, true);
                    def files = tempFolder.listFiles()[0].listFiles()
                    files.each {File file ->
                        if (file.name.equals(inlineScriptName)){
                            hasInlineScript = true
                        }
                    }
                    assert hasInlineScript
                }
        }
        assert  hasCompressedFolder
    }

}