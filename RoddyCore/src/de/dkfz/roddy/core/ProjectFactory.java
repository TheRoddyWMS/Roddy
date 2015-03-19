package de.dkfz.roddy.core;

import de.dkfz.roddy.config.*;
import de.dkfz.roddy.plugins.LibrariesFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
//        for(Configuration cp : configuration.getListOfSubConfigurations()) {
//            ProjectConfiguration pc = (ProjectConfiguration)cp;
//            subProjects.add(loadConfiguration());
//        }
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

            for (String s : configuration.getListOfAnalysisIDs()) {
                AnalysisConfiguration ac = configuration.getAnalysis(s);
                if (ac == null) continue;

                analyses.add(loadAnalysisConfiguration(s, project, ac));
//                configuration.addParent(ac);
//                ac.addParent(configuration);
            }

            return project;
        } catch (ClassNotFoundException e) {
            logger.severe("Cannot find project or runtime class! " + e.toString());
            return null;
        } catch (NoSuchMethodException e) {
            logger.severe("Cannot find constructor <init>(Configuration)! " + e.toString());
            return null;
        } catch (Exception e) {
            logger.severe("Cannot call constructor or error during call! (" + projectClass + ", " + runtimeServiceClass + ") ex=" + e.toString() );
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
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InstantiationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InvocationTargetException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NoSuchMethodException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return analysis;
    }

//    /**
//     * Returns a list of all project objects.
//     * @return
//     */
//    public List<Project> getAllProjects() {
//        return null;  //To change body of created methods use File | Settings | File Templates.
//    }
//
//    /**
//     * Returns a list of the names of all projects.
//     * @return
//     */
//    public List<String> getListOfProjects() {
//        List<InformationalConfigurationContent> projects = new LinkedList<InformationalConfigurationContent>(ConfigurationFactory.getInstance().getAvailableProjectConfigurations());
//        List<String> allProjects = new LinkedList<String>();
//        for(InformationalConfigurationContent icc : projects)  {
//            allProjects.add(icc.id);
//        }
//    }
//
//    public List<String> getListOfAnalysisIDs() {
//        List<InformationalConfigurationContent> analyses = return new LinkedList<InformationalConfigurationContent>(ConfigurationFactory.getInstance().getAvailableAnalysisConfigurations());
//    }
//
//    /**
//     * Returns a specific project.
//     * @param name
//     * @return
//     */
//    public Project getProject(String name) {
////        return project;
//        return null;
//    }
}
