/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.validation

import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.config.EnumerationValue
import de.dkfz.roddy.plugins.LibrariesFactory

/**
 * Validates a whole configuration.
 * Does checks like configuration values,
 * scripts
 * binaries
 *
 */
@groovy.transform.CompileStatic
public class WholeConfigurationValidator extends ConfigurationValidator {

    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(WholeConfigurationValidator.class.getSimpleName());

    /**
     * Keep a list of known validators for various class names
     */
    private static final Map<String, ConfigurationValueValidator> cValueValidators = [:];

    public WholeConfigurationValidator(Configuration cfg) {
        super(cfg);
    }

    public boolean validate() {
        if (configuration.isInvalid()) {
            logger.info("Not revalidating configuration ${configuration.name}; Already invalid.")
            return false
        };

        boolean valid = true;
        try {
            List<ConfigurationValue> unValidatedConfigurationValues = [];
            for (ConfigurationValue configurationValue : configuration.getConfigurationValues().getAllValuesAsList()) {
                EnumerationValue val = configurationValue.getEnumerationValueType();
                String className = val != null ? val.tag : null;
                if (!className) {
                    className = DefaultValidator.class.name;
                    if (cValueValidators[className] == null) {
                        logger.warning("Enumeration could not be found, using DefaultValidator for value validation.");
                        cValueValidators[className] = (DefaultValidator) new DefaultValidator(configuration);
                    }
                }
                if (!cValueValidators.containsKey(className)) {
                    try {
                        cValueValidators[className] = (ConfigurationValueValidator) LibrariesFactory.getInstance().loadClass(className).newInstance(configuration);
                    } catch (Exception ex) {
                        cValueValidators[className] = new Invalidator(configuration);
                        logger.severe("Could not load validator " + className);
                    }
                }
                ConfigurationValueValidator dVal = cValueValidators[className];
                if (!dVal || !dVal.validate(configurationValue))
                    unValidatedConfigurationValues << configurationValue;
            }

            for (ConfigurationValue cval in unValidatedConfigurationValues) {
                logger.severe("Could not validate value " + cval.id);
            }
//            if (!validateConfiguration(configuration)) {
//                configuration.setInvalid(true);
//                logger.severe("Configuration ${configuration.name} is invalid.");
//            }
            valid = unValidatedConfigurationValues.size() == 0;
        } catch (Exception ex) {
            logger.severe(ex.toString());
            for (Object o in ex.getStackTrace())
                logger.info(o.toString());
//            (Thread.dumpStack());
            valid = false;
        }


        ConfigurationValueCombinationValidator configurationValueCombinationValidator = new ConfigurationValueCombinationValidator(configuration)
        valid &= configurationValueCombinationValidator.validate();

        ScriptValidator scriptValidator = new ScriptValidator(configuration)
        valid &= scriptValidator.validate();

        addAllErrorsToList(configurationValueCombinationValidator.getValidationErrors())
        addAllErrorsToList(scriptValidator.getValidationErrors());

        if (configuration.isInvalid()) {
            logger.severe("Configuration ${configuration.name} is invalid.");
        }
        return valid;
    }
}


