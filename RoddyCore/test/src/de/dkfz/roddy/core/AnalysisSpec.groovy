/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */
package de.dkfz.roddy.core

import de.dkfz.roddy.Roddy
import spock.lang.Shared
import spock.lang.Specification

import static de.dkfz.roddy.config.ConfigurationIssue.ConfigurationIssueTemplate.detachedDollarCharacter
import static de.dkfz.roddy.config.ConfigurationIssue.ConfigurationIssueTemplate.valueAndTypeMismatch

class AnalysisSpec extends Specification {

    @Shared
    static def valA = detachedDollarCharacter.expand("a", "/some/path")

    @Shared
    static def valB = detachedDollarCharacter.expand("b", "/other/path")

    @Shared
    static def valC = valueAndTypeMismatch.expand("a", "/another/path","b")

    def "Condense"(issues, expected) {
        expect:
        Analysis.condense(issues) == expected

        where:
        issues             | expected
        [valA]             | [valA.message]
        [valA, valB]       | [valA.collectiveMessage]
        [valC, valA, valB] | [valA.collectiveMessage, valC.message]  // condense sorts the messages by the enumeration values.
    }
}
