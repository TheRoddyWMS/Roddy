/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.converters

import de.dkfz.roddy.RoddyTestSpec

class JSONConverterTest extends RoddyTestSpec {

    def "convert JSON configuration files to Roddy xml"(json, xml) {
        when:
        def converted = new JSONConverter().convertToXML(getResourceFile("roddy/config/converters/$json").absoluteFile)
        def expected = getResourceFile("roddy/config/converters/$xml").text
        /**
         * - A trim never hurts.
         * - The test (and also the YAML loader) looks overly complicated. This has reasons:
         *   - Manual tests with a diff showed equal texts but comparing both using converted == expected
         *     did not result in true! So we need a bit more code here.
         *   - Also it is hard for Groovy to calculate text distances / differences, when the texts are too large, so we
         *     could not directly see what was wrong.
         *   - Moreover resultLines == expectedLines also resulted in false due to missing trim() directives.
         *   - Do not reformat the XML files! Idea will mess up the tag ends! We need "/>", Idea likes " />".
         *   - Another unfunny issue: If then: contains a loop, the conditions in the loop seemed to be ignored.
         * - We assume, that the test xml files ALL start with two lines of comment
         *   we can do this here safely, for standard xmls, this can of course be different
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
        json                             | xml
        "projectJSONConfiguration.json"  | "projectConfigurationConverted.xml"
        "analysisJSONConfiguration.json" | "analysisConfigurationConverted.xml"
    }
}
