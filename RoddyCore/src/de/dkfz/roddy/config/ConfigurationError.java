package de.dkfz.roddy.config;

/**
 */
public class ConfigurationError {
    protected final Configuration configuration;
    protected final String id;
    protected final String description;
    protected final Exception exception;

    public ConfigurationError(String description, Configuration configuration, String id, Exception exception) {
        this.description = description;
        this.configuration = configuration;
        this.id = id;
        this.exception = exception;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Exception getException() {
        return exception;
    }

}
