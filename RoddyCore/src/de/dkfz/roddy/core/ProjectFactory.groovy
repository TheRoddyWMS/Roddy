package de.dkfz.roddy.core

import de.dkfz.roddy.AvailableFeatureToggles
import de.dkfz.roddy.Constants
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.client.cliclient.RoddyCLIClient;
import de.dkfz.roddy.config.*
import de.dkfz.roddy.config.validation.XSDValidator;
import de.dkfz.roddy.plugins.LibrariesFactory;
import de.dkfz.roddy.tools.RoddyConversionHelperMethods;
import de.dkfz.roddy.tools.Tuple2;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map

import static de.dkfz.roddy.config.ConfigurationConstants.CFG_INPUT_BASE_DIRECTORY
import static de.dkfz.roddy.config.ConfigurationConstants.CFG_OUTPUT_BASE_DIRECTORY;

/**
 * The project factory converts a configuration to a project/analysis. It stores a reference to already loaded projects and reuses them if possible.
 * A project can have multiple analyses
 */
public class ProjectFactory {

    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(ProjectFactory.class.getName());

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
    public Project loadConfiguration(InformationalConfigurationContent icc) {
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
    public Project loadConfiguration(ProjectConfiguration configuration) {
        List<Project> subProjects = new LinkedList<Project>();
        List<Analysis> analyses = new LinkedList<Analysis>();

        String _projectClass = "";
        String _runtimeServiceClass = "";
        Class projectClass = null;
        Class runtimeServiceClass = null;

        try {
            _projectClass = configuration.getConfiguredClass();
            _runtimeServiceClass = configuration.getRuntimeServiceClass();
            logger.postSometimesInfo("Found project class " + _projectClass);
            logger.postSometimesInfo("Found runtime service class " + _runtimeServiceClass);
            projectClass = LibrariesFactory.getInstance().loadClass(_projectClass);
            runtimeServiceClass = LibrariesFactory.getInstance().loadClass(_runtimeServiceClass);
            RuntimeService runtimeService = (RuntimeService) runtimeServiceClass.getConstructor().newInstance();
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

    public Analysis loadAnalysisConfiguration(String analysisName, Project project, AnalysisConfiguration configuration) {
        if (configuration == null)
            return null;

        Analysis analysis = null;

        try {
            Class analysisClass = LibrariesFactory.getInstance().loadClass(configuration.getConfiguredClass());
            Class workflowClass = LibrariesFactory.getInstance().loadClass(configuration.getWorkflowClass());
            Workflow workflow = (Workflow) workflowClass.getConstructor().newInstance();
            analysis = (Analysis) analysisClass.getConstructor(String.class, Project.class, Workflow.class, AnalysisConfiguration.class).newInstance(analysisName, project, workflow, configuration);
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
     * Load an analysis with a set project/analysis identifier.
     *
     * @param configurationIdentifier Something like [project.subproject.subproject]@[analysisID] where analysisID will be used to find the correct analysis.
     * @return An analysis object containing linking a project and an analysis configuration.
     */
    public Analysis loadAnalysis(String configurationIdentifier) {

        LibrariesFactory.initializeFactory();

        String[] splitProjectAnalysis = configurationIdentifier.split(StringConstants.SPLIT_AT);
        String projectID = splitProjectAnalysis[0];
        if (splitProjectAnalysis.length == 1) {
            RoddyCLIClient.logger.postAlwaysInfo("There was no analysis specified for configuration " + splitProjectAnalysis[0] + "\n\t Please specify the configuration string as [configuration_id]@[analysis_id].");
            return null;
        }
        String analysisID = splitProjectAnalysis[1];
        ConfigurationFactory fac = ConfigurationFactory.getInstance();
        InformationalConfigurationContent iccProject = fac.getAllAvailableConfigurations()[projectID];

        if (iccProject == null) {
            logger.postAlwaysInfo("The project configuration \"${projectID}\" could not be found.")
            return null;
        }

        //Validate the project icc
        if (Roddy.getFeatureToggleValue(AvailableFeatureToggles.XMLValidation))
            XSDValidator.validate(iccProject);

        String fullAnalysisID = iccProject.getListOfAnalyses().find { String aID -> aID.split("[:][:]")[0] == analysisID; }

        if (fullAnalysisID == null) {
            logger.postAlwaysInfo("The analysis \"${analysisID}\" could not be found.")
            return null;
        }

        String[] splitEntries = fullAnalysisID?.split("[:][:]");

        // If the plugin is set, find "parent" plugins with the proper version.
        String pluginPart = splitEntries?.find { String part -> part.startsWith("useplugin") }

        // Get kill switches from the fullAnalysisID.
        String[] killSwitchKeyVal = splitEntries?.find { String part -> part.startsWith("killswitches") }.split(StringConstants.EQUALS)
        List<AnalysisImportKillSwitch> killSwitches;
        if (killSwitchKeyVal.size() == 2) {
            killSwitches = killSwitchKeyVal[1]?.split(StringConstants.SPLIT_COMMA)?.collect { it as AnalysisImportKillSwitch } as List;
        } else {
            killSwitches = new LinkedList<AnalysisImportKillSwitch>()
        }

        boolean pluginsAreLoaded = false;

        def librariesFactory = LibrariesFactory.getInstance()
        if (pluginPart && pluginPart.size() > "useplugin=".size()) {
            // Extract the plugin and its version.
            String pluginStr = pluginPart.split("[=]")[1]
            pluginsAreLoaded = librariesFactory.resolveAndLoadPlugins(pluginStr);
        }

        // If no plugin is set, load all libraries with the settings from the ini file
        String[] iniPluginVersion = Roddy.getPluginVersionEntries();
        if (!pluginsAreLoaded && iniPluginVersion) {
            pluginsAreLoaded = librariesFactory.resolveAndLoadPlugins(iniPluginVersion)
        }

        // If this is also not set, load all libraries with the current version
        if (!pluginsAreLoaded)
            librariesFactory.loadLibraries(librariesFactory.getAvailablePluginVersion());

        fac.loadAvailableAnalysisConfigurationFiles();
        // Validate the analysis icc
//        InformationalConfigurationContent iccAnalysis = fac.allAvailableConfigurations.get(iccProject.anaanalysisID);
//        if(Roddy.getFeatureToggleValue(AvailableFeatureToggles.XMLValidation))
//            XSDValidator.validate(iccAnalysis);

        ProjectConfiguration projectConfiguration = fac.getProjectConfiguration(projectID);
        InformationalConfigurationContent iccAnalysis = ((AnalysisConfigurationProxy) projectConfiguration.getAnalysis(analysisID)).informationalConfigurationContent;
        if (Roddy.getFeatureToggleValue(AvailableFeatureToggles.XMLValidation))
            XSDValidator.validate(iccAnalysis);
        Project project = loadConfiguration(projectConfiguration);

        AnalysisConfiguration ac = projectConfiguration.getAnalysis(analysisID);
        if (killSwitches.contains(AnalysisImportKillSwitch.FilenameSection))
            ac.removeFilenamePatternsRecursively();

        Analysis analysis = null;
        if (ac != null)
            analysis = loadAnalysisConfiguration(analysisID, project, ac);
        project.getAnalyses().add(analysis);

        if (projectConfiguration == null)
            throw new RuntimeException("Could not load project ${projectID}!");

        // Add custom command line values to the project configuration.
        List<String> externalConfigurationValues = Roddy.getCommandLineCall().getSetConfigurationValues();

        def configurationValues = project.getConfiguration().getConfigurationValues()
        for (eVal in externalConfigurationValues) {
            String[] splitIDValue = eVal.split(StringConstants.SPLIT_COLON);
            //TODO Put in a better error checking when converting the split string to a configuration value.
            String value = "'" + (splitIDValue.size() >= 2 ? splitIDValue[1] : "") + "'"; // Surround value with single quotes '' to prevent string interpretation for e.g. execution in bash
            String cvalueId = splitIDValue[0];
            String type = splitIDValue.size() >= 3 ? splitIDValue[2] : "string";
            configurationValues.add(new ConfigurationValue(cvalueId, value, type));
        }

        if (Roddy.useCustomIODirectories()) {
            configurationValues.add(new ConfigurationValue(CFG_INPUT_BASE_DIRECTORY, Roddy.getCustomBaseInputDirectory(), "path"));
            configurationValues.add(new ConfigurationValue(CFG_OUTPUT_BASE_DIRECTORY, Roddy.getCustomBaseOutputDirectory(), "path"));
        }

        if (analysis != null) {
            return analysis;
        } else {
            StringBuilder sb = new StringBuilder();
            sb << "Could not load analysis ${configurationIdentifier}, try one of those: " << Constants.ENV_LINESEPARATOR;
            for (String aID : projectConfiguration.listOfAnalysisIDs) {
                sb << "  " << projectID << "@" << aID << Constants.ENV_LINESEPARATOR;
            }
            throw new RuntimeException(sb.toString());
        }
    }
}
