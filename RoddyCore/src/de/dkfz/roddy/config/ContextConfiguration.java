/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config;

import de.dkfz.roddy.Roddy;

/**
 * A combined combination for a process / context.
 * In this combo, the input analysis configuration is set as the parent in such a way, that the project configuration
 * values always override the analysis configuration objects.
 */
public class ContextConfiguration extends AnalysisConfiguration {
    private final AnalysisConfiguration analysisConfiguration;
    private final ProjectConfiguration projectConfiguration;
    private final Configuration applicationSpecificConfiguration;

    public ContextConfiguration(AnalysisConfiguration configuration, ProjectConfiguration projectConfiguration) {
        super(configuration.getInformationalConfigurationContent(), configuration.getWorkflowClass(), configuration.getRuntimeServiceClass(), configuration, configuration.getListOfUsedTools(), configuration.getUsedToolFolders(), configuration.getCleanupScript());
        this.applicationSpecificConfiguration = Roddy.getApplicationSpecificConfiguration();
        this.analysisConfiguration = configuration;
        this.projectConfiguration = projectConfiguration;
        addParent(analysisConfiguration);
        addParent(projectConfiguration);
        addParent(applicationSpecificConfiguration);
    }

    public AnalysisConfiguration getAnalysisConfiguration() {
        return analysisConfiguration;
    }

    public ProjectConfiguration getProjectConfiguration() {
        return projectConfiguration;
    }

}
