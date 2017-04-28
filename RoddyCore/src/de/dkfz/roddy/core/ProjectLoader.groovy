/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core

import de.dkfz.roddy.Constants
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.config.*
import de.dkfz.roddy.config.validation.XSDValidator
import de.dkfz.roddy.execution.io.MetadataTableFactory
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.plugins.PluginInfo
import de.dkfz.roddy.plugins.PluginInfoMap
import de.dkfz.roddy.plugins.PluginLoaderException
import jdk.internal.org.xml.sax.SAXParseException

import java.lang.reflect.InvocationTargetException

/**
 * The project factory converts a configuration to a project/analysis. It stores a reference to already loaded projects and reuses them if possible.
 * A project can have multiple analyses
 */
@groovy.transform.CompileStatic
class ProjectLoader {

    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(ProjectLoader.class.getSimpleName());

    /**
     * Cache necessary for RMI... TODO Better into libraries factory?
     */
    private static Map<String, Analysis> _analysisCache = [:]

    private ProjectConfiguration projectConfiguration

    List<AnalysisImportKillSwitch> killSwitches

    String projectID

    String analysisID

    Project project

    Analysis analysis

    ProjectLoader() {

    }

    ProjectLoader(ProjectConfiguration projectConfiguration) {
        this.projectConfiguration = projectConfiguration
    }


    Analysis loadAnalysisConfiguration(String analysisName, Project project, AnalysisConfiguration configuration) {
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
            logger.sometimes("Created an analysis object of class ${analysis.class.name} with workflow class ${workflow.class.name}.")
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
    String getPluginRoddyAPILevel(String configurationIdentifier) {
        extractProjectIDAndAnalysisIDOrFail(configurationIdentifier)

        if (analysisID == null) return null; //Return null (which needs to be checked then) because an auto id is not possible.

        String fullAnalysisID = getFullAnalysisID(projectID, analysisID)

        LibrariesFactory librariesFactory = LibrariesFactory.initializeFactory();

        String pluginString; List<AnalysisImportKillSwitch> killSwitches;
        def res = dissectFullAnalysisID(fullAnalysisID)
        pluginString = res[0];

        PluginInfoMap mapOfPlugins = librariesFactory.loadMapOfAvailablePluginsForInstance();
        PluginInfo pinfo = mapOfPlugins.getPluginInfoWithPluginString(pluginString);
        return pinfo.getRoddyAPIVersion();
    }

    /**
     * Load an analysis with a set project/analysis identifier.
     *
     * @param configurationIdentifier Something like [project.subproject.subproject]@[analysisID] where analysisID will be used to find the correct analysis.
     * @return An analysis object containing linking a project and an analysis configuration.
     */
    Analysis loadAnalysisAndProject(String configurationIdentifier) {
        try {
            if (_analysisCache.containsKey(configurationIdentifier))
                return _analysisCache[configurationIdentifier];

            extractProjectIDAndAnalysisIDOrFail(configurationIdentifier)

            loadPluginsOrFail(projectID, analysisID)

            loadProjectConfigurationOrFail(projectID, analysisID)

            createProjectFromConfigurationOrFail()

            loadAnalysisOrFail()

            // Also a failed analysis object should go to cache.
            // Put into cache
            _analysisCache[configurationIdentifier] = analysis;

            performFinalChecksOrFail(analysis, configurationIdentifier, projectConfiguration, projectID)

            // Try to build up the metadata table from here on. Project and analysis are ready.
            MetadataTableFactory.getTable(analysis);
            return analysis;
        } catch (ProjectLoaderException ex) {
            logger.severe(ex.message)
            return null;
        } catch (PluginLoaderException ex) {
            logger.severe(ex.message)
            return null
        }
    }

    void extractProjectIDAndAnalysisIDOrFail(String configurationIdentifier) {
        String[] splitProjectAnalysis = configurationIdentifier.split(StringConstants.SPLIT_AT);
        projectID = splitProjectAnalysis[0];
        if (splitProjectAnalysis.length == 1) {
            throw new ProjectLoaderException("There was no analysis specified for configuration ${projectID}\n\t Please specify the configuration string as [configuration_id]@[analysis_id].");
        }

        analysisID = splitProjectAnalysis[1];

        if (analysisID == null)
            throw new ProjectLoaderException("Could not extract analysis for ${configurationIdentifier}.");
    }

    void loadPluginsOrFail(String projectID, String analysisID) {
        String fullAnalysisID = getFullAnalysisID(projectID, analysisID)

        def res
        LibrariesFactory librariesFactory = LibrariesFactory.initializeFactory();
        boolean pluginsAreLoaded = false;

        // Get the plugin string and enabled kill switches
        res = dissectFullAnalysisID(fullAnalysisID)
        String pluginString = res[0];
        killSwitches = res[1] as List<AnalysisImportKillSwitch>;

        if (pluginString) {
            pluginsAreLoaded = librariesFactory.resolveAndLoadPlugins(pluginString);
        }

        // If no plugin is set, load all libraries with the settings from the ini file
        String[] iniPluginVersion = Roddy.getPluginVersionEntries();
        if (!pluginsAreLoaded && iniPluginVersion) {
            pluginsAreLoaded = librariesFactory.resolveAndLoadPlugins(iniPluginVersion)
        }

        if (!pluginsAreLoaded) {
            throw new ProjectLoaderException("Unrecoverable errors, could not load plugins for analysis ${analysisID}")
        }
    }

    void loadProjectConfigurationOrFail(String projectID, String analysisID) {
        ConfigurationFactory fac = ConfigurationFactory.getInstance();
        // Just make sure all files are loaded.
        fac.loadAvailableAnalysisConfigurationFiles();

        projectConfiguration = fac.getProjectConfiguration(projectID);
        InformationalConfigurationContent iccAnalysis = ((AnalysisConfigurationProxy) projectConfiguration.getAnalysis(analysisID)).informationalConfigurationContent;
        if (!XSDValidator.validateTree(iccAnalysis) && Roddy.isStrictModeEnabled()) {
            throw new ProjectLoaderException("Validation of project configuration failed.")
        }
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
    void createProjectFromConfigurationOrFail() {
        List<Project> subProjects = new LinkedList<Project>();
        List<Analysis> analyses = new LinkedList<Analysis>();

        String _projectClass
        String _runtimeServiceClass
        Class projectClass = null
        Class runtimeServiceClass = null

        try {
            _projectClass = projectConfiguration.getConfiguredClass();
            _runtimeServiceClass = projectConfiguration.getRuntimeServiceClass();
            logger.postSometimesInfo("Found project class: " + _projectClass);
            if (_runtimeServiceClass)
                logger.postSometimesInfo("Found runtime service class set in project configuration: " + _runtimeServiceClass);
            projectClass = LibrariesFactory.getInstance().loadClass(_projectClass);
            RuntimeService runtimeService
            if (_runtimeServiceClass) {
                runtimeServiceClass = LibrariesFactory.getInstance().loadClass(_runtimeServiceClass);
                runtimeService = (RuntimeService) runtimeServiceClass.getConstructor().newInstance();
            }
            project = (Project) projectClass.getConstructor(ProjectConfiguration.class, RuntimeService.class, List.class, List.class).newInstance(projectConfiguration, runtimeService, subProjects, analyses);
        } catch (ClassNotFoundException e) {
            logger.severe("Cannot find project or runtime class! " + e.toString());
        } catch (NoSuchMethodException e) {
            logger.severe("Cannot find constructor <init>(Configuration)! " + e.toString());
        } catch (Exception e) {
            logger.severe("Cannot call constructor or error during call! (" + projectClass + ", " + runtimeServiceClass + ") ex=" + e.toString());
        }

        if (!projectConfiguration) {
            throw new ProjectLoaderException("Could not load project ${projectID}!")
        }
    }

    void loadAnalysisOrFail() {
        AnalysisConfiguration analysisConfiguration = loadAnalysisConfigurationFromProjectConfigurationOrFail(projectConfiguration, analysisID)

        extractKillSwitchesFromAnalysisConfiguration(analysisConfiguration)

        if (analysisConfiguration != null)
            analysis = loadAnalysisConfiguration(analysisID, project, analysisConfiguration);
        project.getAnalyses().add(analysis);

        if (analysis == null)
            throw new ProjectLoaderException("Could not load analysis ${analysisID}")
    }

    AnalysisConfiguration loadAnalysisConfigurationFromProjectConfigurationOrFail(ProjectConfiguration projectConfiguration, String analysisID) {
        AnalysisConfiguration analysisConfiguration = projectConfiguration.getAnalysis(analysisID);
        if (!analysisConfiguration) {
            throw new ProjectLoaderException("The analysis configuration for ${analysisID} could not be retrieved.")
        }
        return analysisConfiguration
    }

    void extractKillSwitchesFromAnalysisConfiguration(AnalysisConfiguration analysisConfiguration) {
        if (killSwitches.contains(AnalysisImportKillSwitch.FilenameSection)) {
            logger.postSometimesInfo("Killswitch for filename patterns is ACTIVE! Please see, if filenames are properly set.")
            analysisConfiguration.removeFilenamePatternsRecursively();
        }
    }

    /**
     * Dissects an analysis id string. Returns:
     * [0] = plugin
     * [1] = killswitches
     * @param fullAnalysisID
     * @return
     */
    static List dissectFullAnalysisID(String fullAnalysisID) {
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

    String getFullAnalysisID(String projectID, String analysisID) {
        InformationalConfigurationContent iccProject = loadAndValidateProjectICCOrFail(projectID);

        String fullAnalysisID = loadFullAnalysisIDOrFail(iccProject, analysisID);
        fullAnalysisID
    }

    InformationalConfigurationContent loadAndValidateProjectICCOrFail(String projectID) {
        ConfigurationFactory fac = ConfigurationFactory.getInstance();
        InformationalConfigurationContent iccProject = fac.getAllAvailableConfigurations()[projectID];
        logger.postRareInfo("Loading information configuration context for ${projectID} from ${iccProject.file}")

        if (!iccProject) {
            throw new ProjectLoaderException("The project configuration \"${projectID}\" could not be found (call Roddy with listworkflows)")
        }

        //Validate the project icc
        XSDValidator.validateTree(iccProject);
        return iccProject;
    }

    String loadFullAnalysisIDOrFail(InformationalConfigurationContent iccProject, String analysisID) {
        String fullAnalysisID = iccProject.getListOfAnalyses().find { String aID -> aID.split("[:][:]")[0] == analysisID; }

        if (!fullAnalysisID) {
            throw new ProjectLoaderException("The analysis \"${analysisID}\" could not be found in (call Roddy with listworkflows)")
        }
        return fullAnalysisID
    }

    /**
     * Check if an analysis is available and if the runtime service is setup properly.
     */
    void performFinalChecksOrFail(Analysis analysis, String configurationIdentifier, ProjectConfiguration projectConfiguration, String projectID) {
        if (analysis == null) {
            StringBuilder sb = new StringBuilder();
            sb << "Could not load analysis ${configurationIdentifier}, try one of those: " << Constants.ENV_LINESEPARATOR;
            for (String aID : projectConfiguration.listOfAnalysisIDs) {
                sb << "  " << projectID << "@" << aID << Constants.ENV_LINESEPARATOR;
            }
            throw new ProjectLoaderException(sb.toString());
        } else if (analysis.getRuntimeService() == null) {
            throw new ProjectLoaderException("There is no runtime service class set for the selected analysis. This has to be set in either the project configuration or the analysis configuration.");
        }
    }
}
