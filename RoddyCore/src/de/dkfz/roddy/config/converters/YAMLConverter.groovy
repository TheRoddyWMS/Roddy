/*
 * Copyright (c) 2017 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.converters

import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.core.ExecutionContext
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.representer.Representer
import org.yaml.snakeyaml.resolver.Resolver
import sun.reflect.generics.reflectiveObjects.NotImplementedException

/**
 * Created by heinold on 30.01.17.
 */
class YAMLConverter extends MapConverter {
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
        convertYamlToXML(file.text)
    }

    String convertYamlToXML(String yaml) {
        Yaml converter = new Yaml(new Constructor(), new Representer(), new DumperOptions(), new Resolver() {
            @Override
            protected void addImplicitResolvers() {
                // Turn off auto resolving of types. Will be done by Roddy later.
            }
        })
        def content = converter.load(yaml)
        def result = convertMap(content)
        result
    }
}
