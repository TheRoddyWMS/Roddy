package de.dkfz.roddy.config;

/**
 * A combined combination for a process / context.
 * In this combo, the input analysis configuration is set as the parent in such a way, that the project configuration
 * values always override the analysis configuration objects.
 */
public class ContextConfiguration extends AnalysisConfiguration {
    private final AnalysisConfiguration analysisConfiguration;
    private final ProjectConfiguration projectConfiguration;

    public ContextConfiguration(AnalysisConfiguration configuration, ProjectConfiguration projectConfiguration) {
        super(configuration.getInformationalConfigurationContent(), configuration.getWorkflowClass(), null, configuration, configuration.getListOfUsedTools(), configuration.getUsedToolFolders(), configuration.getCleanupScript());
        this.analysisConfiguration = configuration;
        this.projectConfiguration = projectConfiguration;
        addParent(analysisConfiguration);
        addParent(projectConfiguration);
//        for(Configuration c : configuration.getContainerParents())
//            addParent(c);
    }

    public AnalysisConfiguration getAnalysisConfiguration() {
        return analysisConfiguration;
    }

    public ProjectConfiguration getProjectConfiguration() {
        return projectConfiguration;
    }
}
