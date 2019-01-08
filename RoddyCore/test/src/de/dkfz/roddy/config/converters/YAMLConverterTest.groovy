/*
 * Copyright (c) 2017 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.converters

import de.dkfz.roddy.RoddyTestSpec

/**
 * Created by heinold on 30.01.17.
 */
class YAMLConverterTest extends RoddyTestSpec {

    def "convert YAML configuration files to Roddy xml"(yaml, xml) {
        when:
        def converted = new YAMLConverter().convertToXML(getResourceFile("roddy/config/converters/$yaml").absoluteFile)
        def expected = getResourceFile("roddy/config/converters/$xml").text
        /**
         * The JSON and the YAML convert use the same conversion methods as both libraries return a map of nested maps.
         * Constraints of the JSON converter apply here as well.
         */
        def resultLines = converted.trim().readLines()
        def expectedLines = expected.trim().readLines()[2..-1]

        boolean sameSize = resultLines.size() == expectedLines.size()
        def differentLines = []
        if (sameSize) { // Check all lines
            for (int i = 0; i < resultLines.size(); i++) {
                if (resultLines[i].trim() != expectedLines[i].trim()) {
                    differentLines << resultLines[i].trim()
                    differentLines << expectedLines[i].trim()
                }
            }
        }

        then:
        sameSize  // Tests fails if sameSize == false or if there are different lines.
        differentLines.size() == 0

        where:
        yaml                             | xml
        "projectYAMLConfiguration.yaml"  | "projectConfigurationConverted.xml"
        "analysisYAMLConfiguration.yaml" | "analysisConfigurationConverted.xml"
    }

}