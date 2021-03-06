/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */
package de.dkfz.roddy.config

import spock.lang.Shared
import spock.lang.Specification

import static de.dkfz.roddy.config.ConfigurationIssue.ConfigurationIssueLevel.CVALUE_ERROR
import static de.dkfz.roddy.config.ConfigurationIssue.ConfigurationIssueLevel.CVALUE_WARNING
import static de.dkfz.roddy.config.ConfigurationIssue.ConfigurationIssueTemplate.detachedDollarCharacter
import static de.dkfz.roddy.config.ConfigurationIssue.ConfigurationIssueTemplate.valueAndTypeMismatch

class ConfigurationIssueSpec extends Specification {

    @Shared
    private static String[] smallArray = ["a", "some"].toArray(new String[0])

    @Shared
    private static String[] largeArray = ["b", "other", "field"].toArray(new String[0])

    def "check proper #REPLACE# count"(template, expectedNoOfPlaceholders) {
        expect:
        template.noOfPlaceholders == expectedNoOfPlaceholders

        where:
        template                | expectedNoOfPlaceholders
        detachedDollarCharacter | 2
        valueAndTypeMismatch    | 3
    }

    def "check property passthrough"(template, messageContent, expectedLevel, expectedCollectiveMessage) {
        when:
        def cval = new ConfigurationIssue(template, messageContent)

        then:
        cval.level == expectedLevel
        cval.collectiveMessage == expectedCollectiveMessage

        where:
        template                | messageContent | expectedLevel  | expectedCollectiveMessage
        detachedDollarCharacter | smallArray     | CVALUE_WARNING | "Variables with plain dollar sign(s). Roddy does not interpret them as variables and cannot guarantee correct ordering of assignments for such variables in the job parameter file."
        valueAndTypeMismatch    | largeArray     | CVALUE_ERROR   | "Variables in your configuration mismatch regarding their type and value. See the extended logs for more information."
    }


    def "get message"(template, messageContent, expectedMessage) {
        expect:
        new ConfigurationIssue(template, messageContent).message == expectedMessage

        where:
        template                | messageContent | expectedMessage
        detachedDollarCharacter | smallArray     | "Variable 'a' defined in 'some' with plain dollar sign(s) without braces. Roddy does not interpret them as variables and cannot guarantee correct ordering of assignments for such variables in the job parameter file."
        valueAndTypeMismatch    | largeArray     | "The value of variable named 'b' defined in 'other' is not of its declared type 'field'."
    }

    def "expand template"(ConfigurationIssue.ConfigurationIssueTemplate template, messageContent, expectedIssue) {
        expect:
        template.expand(messageContent) == expectedIssue

        where:
        template                | messageContent | expectedIssue
        detachedDollarCharacter | smallArray     | new ConfigurationIssue(detachedDollarCharacter, "a", "some")
        valueAndTypeMismatch    | largeArray     | new ConfigurationIssue(valueAndTypeMismatch, "b", "other", "field")
    }

    def "expand template with exception"(ConfigurationIssue.ConfigurationIssueTemplate template, messageContent, expectedException, expectedMessage) {
        when:
        template.expand(messageContent)

        then:
        def ex = thrown(expectedException)
        ex.message == expectedMessage

        where:
        template                | messageContent | expectedException | expectedMessage
        detachedDollarCharacter | null           | RuntimeException  | "You need to supply a value for all #REPLACE_[n]# fields for the configuration issue template 'detachedDollarCharacter'."
        detachedDollarCharacter | new String[0]  | RuntimeException  | "You need to supply a value for all #REPLACE_[n]# fields for the configuration issue template 'detachedDollarCharacter'."
        valueAndTypeMismatch    | smallArray     | RuntimeException  | "You need to supply a value for all #REPLACE_[n]# fields for the configuration issue template 'valueAndTypeMismatch'."
        detachedDollarCharacter | largeArray     | RuntimeException  | "You supplied too many values for #REPLACE_[n]# fields for the configuration issue template 'detachedDollarCharacter'."
    }

    def "equalitycheck"() {
        expect:
        detachedDollarCharacter.
                expand('a', 'file') == detachedDollarCharacter.expand('a', 'file')
        detachedDollarCharacter.
                expand('b', 'other') != detachedDollarCharacter.expand('c', 'other')
    }

    def "test toString() {"() {
        expect:
        detachedDollarCharacter.
                expand('a', 'some').toString() == "Variable 'a' defined in 'some' with plain dollar sign(s) without braces. Roddy does not interpret them as variables and cannot guarantee correct ordering of assignments for such variables in the job parameter file."
        valueAndTypeMismatch.
                expand('b', 'other', 'c').toString() == "The value of variable named 'b' defined in 'other' is not of its declared type 'c'."
    }
}
