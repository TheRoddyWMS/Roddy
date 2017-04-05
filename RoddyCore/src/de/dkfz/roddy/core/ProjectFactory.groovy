/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core

import de.dkfz.roddy.AvailableFeatureToggles
import de.dkfz.roddy.Constants
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.config.*
import de.dkfz.roddy.config.validation.XSDValidator
import de.dkfz.roddy.execution.io.MetadataTableFactory;
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.plugins.PluginInfo
import de.dkfz.roddy.plugins.PluginInfoMap

import java.lang.reflect.InvocationTargetException

import static de.dkfz.roddy.config.ConfigurationConstants.CFG_INPUT_BASE_DIRECTORY
import static de.dkfz.roddy.config.ConfigurationConstants.CFG_OUTPUT_BASE_DIRECTORY
import static de.dkfz.roddy.config.ConfigurationConstants.CFG_USED_RESOURCES_SIZE
/**
 * The project factory converts a configuration to a project/analysis. It stores a reference to already loaded projects and reuses them if possible.
 * A project can have multiple analyses
 */
@groovy.transform.CompileStatic
public class ProjectFactory {

    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(ProjectFactory.class.getSimpleName());

    private static final ProjectFactory singleton = new ProjectFactory();

    public static ProjectFactory getInstance() {
        return singleton;
    }

    ProjectFactory() {
    }

    /**
     * Stores a list of all available projects by their name.
     */
    private final Map<String, Project> projectMap = new LinkedHashMap<String, Project>();

    /**
     * Tries to load a project configuration with the given icc and calls loadConfiguration(ProjectConfiguration) if the step succeeded
     *
     * @param icc
     * @return
     */
    public static Project loadConfiguration(InformationalConfigurationContent icc) {
        if (icc.type == Configuration.ConfigurationType.PROJECT) {
            ProjectConfiguration pc = (ProjectConfiguration) ConfigurationFactory.getInstance().loadConfiguration(icc);
            return loadConfiguration(pc);
        }
        return null;
    }

    /**
     * Creates a project out of a configuration object. The project is stored in the project map afterwards.
     * If the project is already known it is checked whether the available project knows about the new analysis.
     * If the analysis is already available then just return the known object.
     * May return null if the configuration is at least on the project level.
     *
     * @param configuration
     * @return
     */
    public static Project loadConfiguration(ProjectConfiguration configuration) {
        List<Project> subProjects = new LinkedList<Project>();
        List<Analysis> analyses = new LinkedList<Analysis>();

        String _projectClass = "";
        String _runtimeServiceClass = "";
        Class projectClass = null;
        Class runtimeServiceClass = null;

        try {
            _projectClass = configuration.getConfiguredClass();
            _runtimeServiceClass = configuration.getRuntimeServiceClass();
            logger.postSometimesInfo("Found project class: " + _projectClass);
            if(_runtimeServiceClass)
                logger.postSometimesInfo("Found runtime service class set in project configuration: " + _runtimeServiceClass);
            projectClass = LibrariesFactory.getInstance().loadClass(_projectClass);
            RuntimeService runtimeService
            if (_runtimeServiceClass) {
                runtimeServiceClass = LibrariesFactory.getInstance().loadClass(_runtimeServiceClass);
                runtimeService = (RuntimeService) runtimeServiceClass.getConstructor().newInstance();
            }
            Project project = (Project) projectClass.getConstructor(ProjectConfiguration.class, RuntimeService.class, List.class, List.class).newInstance(configuration, runtimeService, subProjects, analyses);

            return project;
        } catch (ClassNotFoundException e) {
            logger.severe("Cannot find project or runtime class! " + e.toString());
            return null;
        } catch (NoSuchMethodException e) {
            logger.severe("Cannot find constructor <init>(Configuration)! " + e.toString());
            return null;
        } catch (Exception e) {
            logger.severe("Cannot call constructor or error during call! (" + projectClass + ", " + runtimeServiceClass + ") ex=" + e.toString());
            return null;
        }
    }

    public static Analysis loadAnalysisConfiguration(String analysisName, Project project, AnalysisConfiguration configuration) {
        if (configuration == null)
            return null;

        Analysis analysis = null;

        try {
            Class analysisClass = LibrariesFactory.getInstance().loadClass(configuration.getConfiguredClass());
            Class workflowClass = LibrariesFactory.getInstance().loadClass(configuration.getWorkflowClass());
            String _runtimeServiceClass = configuration.getRuntimeServiceClass();
            Workflow workflow
            if (workflowClass.name.endsWith('$py')) {
                // Jython creates a class called Workflow$py with a constructor with a single (unused) String parameter.
                workflow = (Workflow) workflowClass.getConstructor(String).newInstance("dummy")
            } else {
                workflow = (Workflow) workflowClass.getConstructor().newInstance();
            }
            RuntimeService runtimeService

            if (_runtimeServiceClass) {
                Class runtimeServiceClass = LibrariesFactory.getInstance().loadClass(_runtimeServiceClass);
                runtimeService = (RuntimeService) runtimeServiceClass.getConstructor().newInstance();
            }
            def constructor = analysisClass.getConstructor(String.class, Project.class, Workflow.class, RuntimeService.class, AnalysisConfiguration.class)
            analysis = (Analysis) constructor.newInstance(analysisName, project, workflow, runtimeService, configuration);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return analysis;
    }

    /**
     * There is some code duplication in this method and I do not actually like it. It has to be refactored for future times.
     * @return
     */
    public static String getPluginRoddyAPILevel(String configurationIdentifier) {
        String projectID; String analysisID;
        def res = extractProjectIDAndAnalysisID(configurationIdentifier)
        projectID = res[0]; analysisID = res[1]

        if (analysisID == null) return null; //Return null (which needs to be checked then) because an auto id is not possible.

        String fullAnalysisID = getFullAnalysisID(projectID, analysisID)

        LibrariesFactory librariesFactory = LibrariesFactory.initializeFactory();

        String pluginString; List<AnalysisImportKillSwitch> killSwitches;
        res = dissectFullAnalysisID(fullAnalysisID)
        pluginString = res[0];

        PluginInfoMap mapOfPlugins = librariesFactory.loadMapOfAvailablePluginsForInstance();
        PluginInfo pinfo = mapOfPlugins.getPluginInfoWithPluginString(pluginString);
        return pinfo.getRoddyAPIVersion();
    }

    /**
     * Cache necessary for RMI... TODO Better into libraries factory?
     */
    private static Map<String, Analysis> _analysisCache = [:]

    /**
     * Load an analysis with a set project/analysis identifier.
     *
     * @param configurationIdentifier Something like [project.subproject.subproject]@[analysisID] where analysisID will be used to find the correct analysis.
     * @return An analysis object containing linking a project and an analysis configuration.
     */
    public static synchronized Analysis loadAnalysis(String configurationIdentifier) {
        if(_analysisCache.containsKey(configurationIdentifier)) return _analysisCache[configurationIdentifier];

        String projectID;
        String analysisID;
        def res = extractProjectIDAndAnalysisID(configurationIdentifier)
        projectID = res[0]; analysisID = res[1]

        if (analysisID == null) return null;

        String fullAnalysisID = getFullAnalysisID(projectID, analysisID)

        LibrariesFactory librariesFactory = LibrariesFactory.initializeFactory();
        ConfigurationFactory fac = ConfigurationFactory.getInstance();

        boolean pluginsAreLoaded = false;

        String pluginString; List<AnalysisImportKillSwitch> killSwitches;
        res = dissectFullAnalysisID(fullAnalysisID)
        pluginString = res[0];
        killSwitches = res[1] as List<AnalysisImportKillSwitch>;

        if (pluginString) {
            pluginsAreLoaded = librariesFactory.resolveAndLoadPlugins(pluginString);
        }

        // If no plugin is set, load all libraries with the settings from the ini file
        String[] iniPluginVersion = Roddy.getPluginVersionEntries();
        if (!pluginsAreLoaded && iniPluginVersion) {
            pluginsAreLoaded = librariesFactory.resolveAndLoadPlugins(iniPluginVersion)
        }

        // If this is also not set, exit with an error code!
        if (!pluginsAreLoaded) {
            logger.severe("Unrecoverable errors, could not load plugins for analysis ${analysisID}");
            Roddy.exit(2)
        }

        fac.loadAvailableAnalysisConfigurationFiles();

        ProjectConfiguration projectConfiguration = fac.getProjectConfiguration(projectID);
        InformationalConfigurationContent iccAnalysis = ((AnalysisConfigurationProxy) projectConfiguration.getAnalysis(analysisID)).informationalConfigurationContent;
        XSDValidator.validateTree(iccAnalysis);
        Project project = loadConfiguration(projectConfiguration);

        AnalysisConfiguration ac = projectConfiguration.getAnalysis(analysisID);
        if (killSwitches.contains(AnalysisImportKillSwitch.FilenameSection)) {
            logger.postSometimesInfo("Killswitch for filename patterns is ACTIVE! Please see, if filenames are properly set.")
            ac.removeFilenamePatternsRecursively();
        }

        Analysis analysis = null;
        if (ac != null)
            analysis = loadAnalysisConfiguration(analysisID, project, ac);
        project.getAnalyses().add(analysis);

        if (projectConfiguration == null)
            throw new RuntimeException("Could not load project ${projectID}!");

        // Add custom command line values to the project configuration.
        List<ConfigurationValue> externalConfigurationValues = Roddy.getCommandLineCall().getSetConfigurationValues();

        RecursiveOverridableMapContainerForConfigurationValues configurationValues = project.getConfiguration().getConfigurationValues()
        configurationValues.addAll(externalConfigurationValues);

        if (Roddy.useCustomIODirectories()) {
            configurationValues.add(new ConfigurationValue(CFG_INPUT_BASE_DIRECTORY, Roddy.getCustomBaseInputDirectory(), "path"));
            configurationValues.add(new ConfigurationValue(CFG_OUTPUT_BASE_DIRECTORY, Roddy.getCustomBaseOutputDirectory(), "path"));
        }

        if (Roddy.getUsedResourcesSize()) {
            configurationValues.add(new ConfigurationValue(CFG_USED_RESOURCES_SIZE, Roddy.getUsedResourcesSize().toString(), "string"));
        }

        // Put into cache
        _analysisCache[configurationIdentifier] = analysis;

        // Check if an analysis is available and if the runtime service is setup properly.
        if (analysis == null) {
            StringBuilder sb = new StringBuilder();
            sb << "Could not load analysis ${configurationIdentifier}, try one of those: " << Constants.ENV_LINESEPARATOR;
            for (String aID : projectConfiguration.listOfAnalysisIDs) {
                sb << "  " << projectID << "@" << aID << Constants.ENV_LINESEPARATOR;
            }
            throw new RuntimeException(sb.toString());
        } else if (analysis.getRuntimeService() == null) {
            throw new RuntimeException("There is no runtime service class set for the selected analysis. This has to be set in either the project configuration or the analysis configuration.");
        } else {

            // Try to build up the metadata table from here on. Project and analysis are ready.
            MetadataTableFactory.getTable(analysis);
            return analysis;
        }
    }

    /**
     * Dissects an analysis id string. Returns:
     * [0] = plugin
     * [1] = killswitches
     * @param fullAnalysisID
     * @return
     */
    public static List dissectFullAnalysisID(String fullAnalysisID) {
        String[] splitEntries = fullAnalysisID.split("[:][:]");

        // If the plugin is set, find "parent" plugins with the proper version.
        String pluginPart = splitEntries?.find { String part -> part.startsWith("useplugin") }
        String pluginStr = null;

        if (pluginPart && pluginPart.size() > "useplugin=".size()) {
            // Extract the plugin and its version.
            pluginStr = pluginPart.split("[=]")[1]
        }

        // Get kill switches from the fullAnalysisID.
        String[] killSwitchKeyVal = splitEntries?.find { String part -> part.startsWith("killswitches") }.split(StringConstants.EQUALS)
        List<AnalysisImportKillSwitch> killSwitches;
        if (killSwitchKeyVal.size() == 2) {
            killSwitches = killSwitchKeyVal[1]?.split(StringConstants.SPLIT_COMMA)?.collect { it as AnalysisImportKillSwitch } as List;
        } else {
            killSwitches = new LinkedList<AnalysisImportKillSwitch>()
        }
        [pluginStr, killSwitches]
    }

    private static String getFullAnalysisID(String projectID, String analysisID) {
        InformationalConfigurationContent iccProject = loadAndValidateProjectICC(projectID);

        String fullAnalysisID = loadFullAnalysisID(iccProject, analysisID);
        fullAnalysisID
    }

    public static List extractProjectIDAndAnalysisID(String configurationIdentifier) {
        String[] splitProjectAnalysis = configurationIdentifier.split(StringConstants.SPLIT_AT);
        String projectID = splitProjectAnalysis[0];
        if (splitProjectAnalysis.length == 1) {
            logger.postAlwaysInfo("There was no analysis specified for configuration ${projectID}\n\t Please specify the configuration string as [configuration_id]@[analysis_id].");
            return [null, null];
        }

        String analysisID = splitProjectAnalysis[1];
        [projectID, analysisID]
    }

    public static InformationalConfigurationContent loadAndValidateProjectICC(String projectID) {
        ConfigurationFactory fac = ConfigurationFactory.getInstance();
        InformationalConfigurationContent iccProject = fac.getAllAvailableConfigurations()[projectID];
        logger.postRareInfo("Loading information configuration context for ${projectID} from ${iccProject.file}")

        if (iccProject == null) {
            logger.postAlwaysInfo("The project configuration \"${projectID}\" could not be found (call Roddy with listworkflows)")
            Roddy.exit(1)
        }

        //Validate the project icc
        XSDValidator.validateTree(iccProject);
        return iccProject;
    }

    public static String loadFullAnalysisID(InformationalConfigurationContent iccProject, String analysisID) {
        String fullAnalysisID = iccProject.getListOfAnalyses().find { String aID -> aID.split("[:][:]")[0] == analysisID; }

        if (fullAnalysisID == null) {
            logger.postAlwaysInfo("The analysis \"${analysisID}\" could not be found in (call Roddy with listworkflows)")
            Roddy.exit(1)
        }
        return fullAnalysisID
    }
}
