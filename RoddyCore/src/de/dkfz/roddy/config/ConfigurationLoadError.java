package de.dkfz.roddy.config;

/**
 * Errors which occur during configuration load are represented by this class.
 */
public class ConfigurationLoadError extends ConfigurationError {

    public ConfigurationLoadError(Configuration configuration, String id, String description, Exception exception) {
        super(description, configuration, id, exception);
    }
}
