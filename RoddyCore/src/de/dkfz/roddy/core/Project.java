/*
 * Copyright (c) 2021 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core;

import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.ProjectConfiguration;
import de.dkfz.roddy.tools.LoggerWrapper;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * A project is the central object to enable the processing of PIDs with a specific configuration.
 * Therefore it creates various services
 *
 * @author michael
 */
public class Project implements Serializable {

    private static final LoggerWrapper logger = LoggerWrapper.getLogger(Project.class.getSimpleName());

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

    /**
     * The runtime service instance for this project.
     */
    private RuntimeService runtimeService;

    public Project(ProjectConfiguration configuration, RuntimeService runtimeService, List<Project> subProjects, List<Analysis> analyses) {
        this.configuration = configuration;
        this.analyses = analyses != null ? analyses : new LinkedList<Analysis>();
        this.subProjects = subProjects != null ? subProjects : new LinkedList<Project>();
        this.runtimeService = runtimeService;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Get the project configuration name. The configuration name is used to load the correct configurations for the
     * analysis.
     */
    public String getConfigurationName() {
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

    public RuntimeService getRuntimeService() {
        return runtimeService;
    }

    /**
     * Sets or overrides
     * @param analysisToUpdate
     */
    public void updateDataSet(DataSet ds, Analysis analysisToUpdate) {
        //
    }
}
