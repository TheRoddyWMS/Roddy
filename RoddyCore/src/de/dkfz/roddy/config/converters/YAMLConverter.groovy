/*
 * Copyright (c) 2017 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.converters

import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.core.ExecutionContext
import sun.reflect.generics.reflectiveObjects.NotImplementedException

/**
 * Created by heinold on 30.01.17.
 */
class YAMLConverter extends ConfigurationConverter {
    @Override
    String convert(ExecutionContext context, Configuration configuration) {
        throw new NotImplementedException()
    }

    @Override
    StringBuilder convertConfigurationValue(ConfigurationValue cv, ExecutionContext context) {
        throw new NotImplementedException()
    }

    @Override
    String convertToXML(File file) {
        throw new NotImplementedException()
    }
}
