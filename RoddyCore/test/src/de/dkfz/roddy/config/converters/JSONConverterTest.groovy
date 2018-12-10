/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.converters

import de.dkfz.roddy.RoddyTestSpec

class JSONConverterTest extends RoddyTestSpec {

    def "convert full json string to Roddy xml"(json, xml) {
        when:
        def converted = new JSONConverter().convertToXML(getResourceFile("roddy/config/converters/$json").absoluteFile)
        def expected = getResourceFile("roddy/config/converters/$xml").text
        /**
         * - A trim never hurts.
         * - Manual tests with a diff showed equal texts but comparing both using converted == expected
         *   did not result in true! So we need a bit more code here.
         *   Also it is hard for Groovy to calculate text distances / differences, when the texts are too large
         *   Moreover resultLines == expectedLines also resulted in false (whyever)
         * - We assume, that the test xml files ALL start with two lines of comment
         *  we can do this here safely, for standard xmls, this can of course be different
         */
        def resultLines = converted.trim().readLines()
        def expectedLines = expected.trim().readLines()[2..-1]

        then:
        resultLines.size() == expectedLines.size()
        for (int i = 0; i < resultLines.size(); i++) {
            resultLines[i] == expectedLines[i]
        }

        where:
        json                             | xml
        "projectJSONConfiguration.json"  | "projectConfigurationConverted.xml"
        "analysisJSONConfiguration.json" | "analysisConfigurationConverted.xml"
    }
}
