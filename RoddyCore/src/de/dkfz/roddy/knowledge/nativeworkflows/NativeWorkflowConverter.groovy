/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.nativeworkflows

import de.dkfz.eilslabs.batcheuphoria.AvailableClusterSystems
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.client.RoddyStartupOptions
import de.dkfz.roddy.config.AnalysisConfiguration
import de.dkfz.roddy.config.ConfigurationFactory
import de.dkfz.roddy.config.converters.BashConverter
import de.dkfz.roddy.config.converters.XMLConverter
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.plugins.NativePluginInfo
import de.dkfz.roddy.plugins.PluginLoaderException
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.slurpersupport.NodeChild

import java.nio.file.Files

/**
 * Convert native plugins to Roddy conform plugins
 *
 * Created by heinold on 04.05.17.
 */
@CompileStatic
class NativeWorkflowConverter {

    public static final LoggerWrapper logger = LoggerWrapper.getLogger(NativeWorkflowConverter.class)

    public static final String NATIVE_WORKFLOW_CONFIGFILE = "analysisConfiguration.sh"

    public static final String NATIVE_WORKFLOW_SCRIPT_PREFIX = "runWorkflow_"

    public static final String NATIVE_WORKFLOW_SCRIPT_SUFFIX = ".sh"

    public static final String NATIVE_WORFLOW_TOOLS_CHECKSUM_FILE = "toolsChecksum.txt"

    NativePluginInfo nativePluginInfo

    File pluginDir

    NativeWorkflowConverter(NativePluginInfo nativePluginInfo) {
        this.nativePluginInfo = nativePluginInfo
        File folderForConvertedPlugins = Roddy.getFolderForConvertedNativePlugins()
        pluginDir = new File(folderForConvertedPlugins, nativePluginInfo.directory.name)
    }

    static boolean isNativePlugin(File directory) {
        try {
            // Just create a dummy plugin info, try catch and return false on PluginLoaderException. Both methods trow this if something is bad..
            def piTest = new NativePluginInfo("TEST", directory, "current", null)
            piTest.getSourceWorkflow()
            piTest.getSourceConfigurationFile()
            return true
        } catch (PluginLoaderException ex) {
            return false
        }
    }

    /**
     *  It differs and cannot be recreated => throw exception
     *  It differs and can be recreated => delete existing target directory
     *  It does not differ => return
     */
    void convert() {
        // Check if there already is a directory
        if (!ensureWorkflowStatusOrFail()) {

            // Finally convert
            def convertedConfigurationDirectory = nativePluginInfo.getConfigurationDirectory()
            def toolsBaseDirectory = nativePluginInfo.getToolsDirectory()
            def convertedToolsDirectory = nativePluginInfo.getConvertedToolsDirectory()

            convertedConfigurationDirectory.mkdirs()
            toolsBaseDirectory.mkdirs()

            // Copy all scripts from original directory to the tools directory
            RoddyIOHelperMethods.copyDirectory(nativePluginInfo.getNativeSourceDirectory(), convertedToolsDirectory)

            String importedConfigurationText = new BashConverter().convertToXML(nativePluginInfo.getSourceConfigurationFile())
            // Extend the configuration by the tool entries.
            importedConfigurationText = extendAndCleanConfiguration(importedConfigurationText)

            nativePluginInfo.getConvertedConfigurationFile() << importedConfigurationText

            // Let's see what we can read in detailed.
            // Additional configuration values?
            // Extract tool entries?
            // Auto extract parameters and file i/o?
            // Based on checkpoints. Then rerun is possible.

//        nativePluginInfo.getConvertedConfigurationFile() << new XMLConverter().convert(null, importedConfigurationText)
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    String extendAndCleanConfiguration(String importedConfigurationText) {
        NodeChild xml = (NodeChild) new XmlSlurper().parseText(importedConfigurationText);
        xml.attributes().put("listOfUsedTools", nativePluginInfo.getToolIDs().join(","));
        xml.attributes().put("usedToolFolder", nativePluginInfo.getConvertedToolsDirectory().name);

        // Get rid of availAna
        xml.children().findAll { NodeChild it -> it.name() == "availableAnalyses"}.replaceNode {}
//        xml.children().get.configuration.attributes().remove()
        return new groovy.xml.StreamingMarkupBuilder().bindNode(xml) as String
    }
/**
     * Make sure, that the plugin:
     *   is the same and can be reused OR
     *   if it is not, check for --forcenativepluginconversion and delete the converted dir OR
     *   fail and throw an exception
     *
     * @return true , if you can reuse the existing converted workflow otherwise false. Note that the method will throw an exception on error!
     */
    boolean ensureWorkflowStatusOrFail() {
        // Check if it is still the same (md5 sums and so on) and if the --forcenativepluginconversion
        boolean forceoverride = Roddy.getCommandLineCall().isOptionSet(RoddyStartupOptions.forcenativepluginconversion)
        if (pluginDir.exists()) {
            def sourceMD5 = RoddyIOHelperMethods.getSingleMD5OfFilesInDirectoryExcludingDirectoryNames(nativePluginInfo.getNativeSourceDirectory())
            def existingMD5 = RoddyIOHelperMethods.getSingleMD5OfFilesInDirectoryExcludingDirectoryNames(nativePluginInfo.getConvertedToolsDirectory())
            def differs = sourceMD5 != existingMD5
            if (differs && !forceoverride)
                throw new PluginLoaderException("Cannot convert the native plugin ${nativePluginInfo.getName()}, it was already converted and is located in ${pluginDir}. Please remove this directory before going on. Or call Roddy with --forcenativepluginconversion.")
            else if (differs) { // Delete and convert
                pluginDir.deleteDir()
                return false
            } else {
                // Reuse
                return true
            }
        }
        return false
    }

    AvailableClusterSystems determineClusterSystem() {
//        nativePluginInfo
    }


}
