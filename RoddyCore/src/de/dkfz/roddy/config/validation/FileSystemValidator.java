/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.validation;

import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.ConfigurationValue;

import java.util.logging.Logger;

/**
 * This validator check values of type
 * string (not really checked)
 * integer
 * boolean
 */
public class FileSystemValidator extends ConfigurationValueValidator {
    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(FileSystemValidator.class.getSimpleName());

    public FileSystemValidator(Configuration cfg) {
        super(cfg);
    }

    @Override
    public boolean validate(ConfigurationValue configurationValue) {
////        logger.info(configurationValue.id + " - " + configurationValue.value);
//        String evID = configurationValue.getEnumerationValueType().getId();
//        boolean result = true;
//        if (evID.equals("integer")) {
//            try {
//                configurationValue.toInt();
//            } catch (Exception e) {
//                result = false;
//            }
//        } else if (evID.equals("boolean")) {
//            configurationValue.toBoolean();
////            result = true;
//        } else if (evID.equals("float")) {
//            try {
//                configurationValue.toFloat();
//            } catch (Exception e) {
//                result = false;
//            }
//        } else if (evID.equals("string")) {
////            return true;
//        }
//        configurationValue.setInvalid(!result);
////        if(!result) {
////            logger.severe("Configuration value " + configurationValue.id + " is not valid.");
////        }
//        return result;
        return true;
    }
}
