package de.dkfz.roddy.core

import de.dkfz.roddy.config.ConfigurationIssue
import spock.lang.Shared
import spock.lang.Specification

import static de.dkfz.roddy.config.ConfigurationIssue.ConfigurationIssueTemplate.unattachedDollarCharacter
import static de.dkfz.roddy.config.ConfigurationIssue.ConfigurationIssueTemplate.valueAndTypeMismatch

class AnalysisSpec extends Specification {

    @Shared
    static def valA = unattachedDollarCharacter.expand("a")

    @Shared
    static def valB = unattachedDollarCharacter.expand("b")

    @Shared
    static def valC = valueAndTypeMismatch.expand("a", "b")

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
