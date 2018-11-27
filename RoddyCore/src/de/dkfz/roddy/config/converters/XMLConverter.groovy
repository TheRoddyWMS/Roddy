/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.converters

import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.config.ConfigurationValueBundle
import de.dkfz.roddy.core.ExecutionContext
import groovy.transform.TypeCheckingMode
import groovy.xml.MarkupBuilder
import sun.reflect.generics.reflectiveObjects.NotImplementedException

/**
 * Converts a configuration object to XML.
 * Created by heinold on 18.06.15.
 */
@groovy.transform.CompileStatic
class XMLConverter extends ConfigurationConverter {

    @Override
    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    String convert(ExecutionContext context, Configuration configuration) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        xml.configuration(name: configuration.getName(), description: configuration.getDescription(), workflowClass: configuration.getConfiguredClass()) {
            configurationvalues {
                for (ConfigurationValue configurationValue in configuration.configurationValues.getAllValuesAsList()) {
                    cvalue(name: configurationValue.getID(), value: configurationValue.getValue())
                }
                for (ConfigurationValueBundle bundle in configuration.getConfigurationValueBundles().getAllValuesAsList()) {
                    configurationValueBundle(name: bundle.ID) {
                        for (ConfigurationValue configurationValue in bundle.getValues()) {
                            cvalue(name: configurationValue.id, value: configurationValue.value)
                        }
                    }
                }
            }
            projects {
                configuration.getSubConfigurations().each {
                    prjName, prjCfg ->
                        project(name: prjName, description: prjCfg.getDescription()) {
                            configurationvalues {
                                prjCfg.getConfigurationValues().each {
                                    String cId, ConfigurationValue cVal ->
                                        try {

                                            cvalue(name: cId, value: cVal.value)
                                        } catch (Exception ex) {
                                            logger.severe("Could not write out configuration value: " + configurationValue.id)
                                        }
                                }
                            }

                            variations {
                                prjCfg.getSubConfigurations().each {
                                    varName, varCfg ->
                                        variation(name: varName, description: varCfg.getDescription()) {
                                            configurationvalues {
                                                varCfg.getConfigurationValues().each {
                                                    configurationValue ->
                                                        cvalue(name: configurationValue.id, value: configurationValue.value)
                                                }
                                            }
                                        }
                                }
                            }
                        }
                }
            }
            //TODO Write enumerations
        }
//        xml.configuration.

        return writer.toString()
    }

    @Override
    StringBuilder convertConfigurationValue(ConfigurationValue cv, ExecutionContext context) {
        throw new NotImplementedException();
    }

    @Override
    String convertToXML(File file) {
        return file.text;
    }
}
