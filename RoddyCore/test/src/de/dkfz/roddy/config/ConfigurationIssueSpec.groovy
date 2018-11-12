package de.dkfz.roddy.config

import spock.lang.Shared
import spock.lang.Specification

import static de.dkfz.roddy.config.ConfigurationIssue.ConfigurationIssueLevel.CVALUE_ERROR
import static de.dkfz.roddy.config.ConfigurationIssue.ConfigurationIssueLevel.CVALUE_WARNING
import static de.dkfz.roddy.config.ConfigurationIssue.ConfigurationIssueTemplate.unattachedDollarCharacter
import static de.dkfz.roddy.config.ConfigurationIssue.ConfigurationIssueTemplate.valueAndTypeMismatch

class ConfigurationIssueSpec extends Specification {

    @Shared
    private static String[] smallArray = ["a"].toArray(new String[0])

    @Shared
    private static String[] largeArray = ["b", "field"].toArray(new String[0])

    def "check proper #REPLACE# count"(template, expectedNoOfPlaceholders) {
        expect:
        template.noOfPlaceholders == expectedNoOfPlaceholders

        where:
        template                  | expectedNoOfPlaceholders
        unattachedDollarCharacter | 1
        valueAndTypeMismatch      | 2
    }

    def "check property passthrough"(template, messageContent, expectedLevel, expectedCollectiveMessage) {
        when:
        def cval = new ConfigurationIssue(template, messageContent)

        then:
        cval.level == expectedLevel
        cval.collectiveMessage == expectedCollectiveMessage

        where:
        template                  | messageContent | expectedLevel  | expectedCollectiveMessage
        unattachedDollarCharacter | smallArray     | CVALUE_WARNING | "Several variables in your configuration contain one or more dollar signs. As this might impose problems in your cluster jobs, check the entries in your job configuration files. See the extended logs for more information."
        valueAndTypeMismatch      | largeArray     | CVALUE_ERROR   | "Several variables in your configuration mismatch regarding their type and value. See the extended logs for more information."
    }


    def "get message"(template, messageContent, expectedMessage) {
        expect:
        new ConfigurationIssue(template, messageContent).message == expectedMessage

        where:
        template                  | messageContent | expectedMessage
        unattachedDollarCharacter | smallArray     | "The variable named 'a' contains one or more dollar signs, which do not belong to a Roddy variable definition (\${variable identifier}). This might impose problems, so make sure, that your results job configuration is created in the way you want."
        valueAndTypeMismatch      | largeArray     | "The value of variable named 'b' does not match the variables type 'field'."
    }

    def "expand template"(ConfigurationIssue.ConfigurationIssueTemplate template, messageContent, expectedIssue) {
        expect:
        template.expand(messageContent) == expectedIssue

        where:
        template                  | messageContent | expectedIssue
        unattachedDollarCharacter | smallArray     | new ConfigurationIssue(unattachedDollarCharacter, "a")
        valueAndTypeMismatch      | largeArray     | new ConfigurationIssue(valueAndTypeMismatch, "b", "field")
    }

    def "expand template with exception"(ConfigurationIssue.ConfigurationIssueTemplate template, messageContent, expectedException, expectedMessage) {
        when:
        template.expand(messageContent)

        then:
        def ex = thrown(expectedException)
        ex.message == expectedMessage

        where:
        template                  | messageContent | expectedException | expectedMessage
        unattachedDollarCharacter | null           | RuntimeException  | "You need to supply a value for all #REPLACE_[n]# fields for the configuration issue template 'unattachedDollarCharacter'."
        unattachedDollarCharacter | new String[0]  | RuntimeException  | "You need to supply a value for all #REPLACE_[n]# fields for the configuration issue template 'unattachedDollarCharacter'."
        valueAndTypeMismatch      | smallArray     | RuntimeException  | "You need to supply a value for all #REPLACE_[n]# fields for the configuration issue template 'valueAndTypeMismatch'."
        unattachedDollarCharacter | largeArray     | RuntimeException  | "You supplied too many values for #REPLACE_[n]# fields for the configuration issue template 'unattachedDollarCharacter'."
    }

    def "equalitycheck"() {
        expect:
        unattachedDollarCharacter.expand("a") == unattachedDollarCharacter.expand("a")
        unattachedDollarCharacter.expand("b") != unattachedDollarCharacter.expand("c")
    }

    def "test toString() {"() {
        expect:
        unattachedDollarCharacter.expand("a").toString() == "The variable named 'a' contains one or more dollar signs, which do not belong to a Roddy variable definition (\${variable identifier}). This might impose problems, so make sure, that your results job configuration is created in the way you want."
        valueAndTypeMismatch.expand("b", "c").toString() == "The value of variable named 'b' does not match the variables type 'c'."
    }
}
