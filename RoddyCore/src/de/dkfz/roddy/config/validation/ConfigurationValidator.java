package de.dkfz.roddy.config.validation;

import de.dkfz.roddy.config.Configuration;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Base class for validators on the configuration level
 */
public abstract class ConfigurationValidator {
    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(ConfigurationValidator.class.getName());

    protected final Configuration configuration;

    private final List<ValidationError> listOfValidationErrors = new LinkedList<>();

    public ConfigurationValidator(Configuration cfg) {
        configuration = cfg;
    }

    public abstract boolean validate();

    public boolean performRuntimeChecks() { return true; }

    public void addErrorToList(ValidationError error) {
        this.listOfValidationErrors.add(error);
    }

    public List<ValidationError> getValidationErrors() {
        return new LinkedList<>(listOfValidationErrors);
    }

    public void addAllErrorsToList(List<ValidationError> listOfErrors) {
        listOfValidationErrors.addAll(listOfErrors);
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
