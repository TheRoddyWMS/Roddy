package de.dkfz.roddy.config.validation;

import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.ConfigurationValue;
import de.dkfz.roddy.config.EnumerationValue;

import java.util.logging.Logger;

/**
 * This validator check values of type
 * string (not really checked)
 * integer
 * boolean
 */
public class DefaultValidator extends ConfigurationValueValidator {
    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(DefaultValidator.class.getSimpleName());

    public DefaultValidator(Configuration cfg) {
        super(cfg);
    }

    @Override
    public boolean validate(ConfigurationValue configurationValue) {
//        logger.info(configurationValue.id + " - " + configurationValue.value);
        EnumerationValue ev = configurationValue.getEnumerationValueType();
        Configuration cfg = configurationValue.getConfiguration();

        String evID = ev != null ? ev.getId() : "string";
        boolean result = true;
        if (evID.equals("integer")) {
            try {
                configurationValue.toInt();
            } catch (Exception e) {
                result = false;
                cfg.addValidationError(new ConfigurationValidationError.DataTypeValidationError(configurationValue, e));
            }
        } else if (evID.equals("boolean")) {
            configurationValue.toBoolean();
//            result = true;
        } else if (evID.equals("float")) {
            try {
                configurationValue.toFloat();
            } catch (Exception e) {
                result = false;
                cfg.addValidationError(new ConfigurationValidationError.DataTypeValidationError(configurationValue, e));
            }
        } else if (evID.equals("double")) {
            try {
                configurationValue.toDouble();
            } catch (Exception e) {
                result = false;
                cfg.addValidationError(new ConfigurationValidationError.DataTypeValidationError(configurationValue, e));
            }
        } else if (evID.equals("string")) {
//            return true;
        }
//        configurationValue.setInvalid(!result);
//        if(!result) {
//            logger.severe("Configuration value " + configurationValue.id + " is not valid.");
//        }
        return result;
    }
}
