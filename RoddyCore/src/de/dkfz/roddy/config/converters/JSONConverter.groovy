/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.converters

import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.core.ExecutionContext
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import sun.reflect.generics.reflectiveObjects.NotImplementedException

/**
 * In exception to our other Groovy classes, this class needs to be CompileDynamic!
 * We use MarkupBuilder closures, where you cannot attach the CompileDynamic directive.
 *
 * Created by heinold on 30.01.17.
 */
class JSONConverter extends MapConverter {

    @CompileStatic
    @Override
    String convert(ExecutionContext context, Configuration configuration) {
        throw new NotImplementedException()
    }

    @CompileStatic
    @Override
    StringBuilder convertConfigurationValue(ConfigurationValue cv, ExecutionContext context) {
        throw new NotImplementedException()
    }

    @CompileStatic
    @Override
    String convertToXML(File file) {
        convertJsonString(file.text)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    String convertJsonString(String json) {
        /**
         Remove comments first, they are not allowed in JSON and will produce errors.

         See also a comment from Douglas Crockford:

         I removed comments from JSON because I saw people were using them to hold
         parsing directives, a practice which would have destroyed interoperability.
         I know that the lack of comments makes some people sad, but it shouldn't.

         Suppose you are using JSON to keep configuration files, which you would
         like to annotate. Go ahead and insert all the comments you like. Then
         pipe it through JSMin before handing it to your JSON parser.

         to be found here: https://plus.google.com/+DouglasCrockfordEsq/posts/RK8qyGVaGSr
         */

        json = json.readLines().findAll { !(it.startsWith("//") || it.startsWith("#")) }.join("\n")
        def content = new JsonSlurper().parseText(json)
        convertMap(content)
    }
}
