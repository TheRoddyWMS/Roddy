/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core;

import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.ConfigurationConstants;
import de.dkfz.roddy.config.ProjectConfiguration;

import java.io.File;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A project is the central object to enable the processing of PIDs with a specific configuration.
 * Therefore it creates various services
 *
 * @author michael
 */
public class Project implements Serializable {

    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(Project.class.getSimpleName());

    /**
     * A list of analyses related to this project
     */
    private final List<Analysis> analyses;
    /**
     * A list of project variants for this project.
     */
    private final List<Project> subProjects;
    /**
     * The list of PIDs found for this project.
     */
    private List<DataSet> listOfDataSets = new LinkedList<>();
    protected Configuration configuration;

//    /**
//     * The input base directory for this analysis. This is the location where the source data folders are located.
//     */
//    private File inputBaseDirectory;

//    /**
//     * The output base directory for this analysis. This is the base location where the analysed data folders are located.
//     */
//    private File outputBaseDirectory;

    /**
     * The runtime service instance for this project.
     */
    private RuntimeService runtimeService;

    public Project(ProjectConfiguration configuration, RuntimeService runtimeService, List<Project> subProjects, List<Analysis> analyses) {
        this.configuration = configuration;
//        inputBaseDirectory = configuration.getConfigurationValues().get(ConfigurationConstants.CFG_INPUT_BASE_DIRECTORY).toFile(this);
//        outputBaseDirectory = configuration.getConfigurationValues().get(ConfigurationConstants.CFG_OUTPUT_BASE_DIRECTORY).toFile(this);
        this.analyses = analyses != null ? analyses : new LinkedList<Analysis>();
        this.subProjects = subProjects != null ? subProjects : new LinkedList<Project>();
        this.runtimeService = runtimeService;
    }

    public Configuration getConfiguration() {
        return configuration;
    }
//
//    public File getNameOfExecCacheFile() {
//        return runtimeService.getNameOfExecCacheFile(this);
//    }
//
//    public File getInputBaseDirectory() {
////        if(inputBaseDirectory == null)
////            inputBaseDirectory = getConfiguration().getConfigurationValue(ConfigurationConstants.CFG_INPUT_BASE_DIRECTORY).toFile(this);
//        return inputBaseDirectory;
//    }
//
//    public File getOutputBaseDirectory() {
////        if(outputBaseDirectory == null)
////            outputBaseDirectory = getConfiguration().getConfigurationValue(ConfigurationConstants.CFG_OUTPUT_BASE_DIRECTORY).toFile(this);
//        return outputBaseDirectory;
//    }

    public String getName() {
        return configuration.getName();
    }

    public List<Analysis> getAnalyses() {
        return analyses;
    }

    public Analysis getAnalysis(String analysisID) {
        for (Analysis analysis : analyses) {
            if (analysis.getName().equals(analysisID)) {
                return analysis;
            }
        }
        return null;
    }

//    /**
//     * Returns the first level of sub projects / variants for this project.
//     *
//     * @return
//     */
//    public List<Project> getDirectSubProjects() {
//        return new LinkedList<Project>(this.subProjects);
//    }

//    /**
//     * Returns a list of all subprojects /variants (direct and indirect) for this project.
//     *
//     * @return
//     */
//    public List<Project> getAllSubProjects() {
//        List<Project> all = new LinkedList<Project>(subProjects);
//        for (Project sp : subProjects) {
//            all.addAll(sp.getAllSubProjects());
//        }
//        return all;
//    }

    public RuntimeService getRuntimeService() {
        return runtimeService;
    }

//    public List<DataSet> getListOfPossibleDataSets() {
//        return listOfDataSets;
//    }

    /**
     * Sets or overrides
     * @param analysisToUpdate
     */
    public void updateDataSet(DataSet ds, Analysis analysisToUpdate) {
        //
    }
}
