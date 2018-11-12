package de.dkfz.roddy.config.validation

import de.dkfz.roddy.config.ConfigurationIssue
import de.dkfz.roddy.config.ConfigurationValue
import spock.lang.Specification

import static de.dkfz.roddy.config.ConfigurationIssue.ConfigurationIssueTemplate.*

class DefaultValidatorSpec extends Specification {

    def 'test validation of types'(value, type, expected, expectedErrors) {
        when:
        ConfigurationValue cval = new ConfigurationValue('bla', value.toString(), type)

        then:
        cval.valid == expected
        cval.warnings == []
        cval.errors == expectedErrors

        where:
        value      | type        | expected | expectedErrors
        1          | 'integer'   | true     | []
        1.0f       | 'float'     | true     | []
        1.0        | 'double'    | true     | []
        true       | 'boolean'   | true     | []
        'badvalue' | 'boolean'   | true     | [] // Validates always!
        'a'        | 'integer'   | false    | [new ConfigurationIssue(valueAndTypeMismatch, 'bla', 'integer')]
        'b'        | 'float'     | false    | [new ConfigurationIssue(valueAndTypeMismatch, 'bla', 'float')]
        'c'        | 'double'    | false    | [new ConfigurationIssue(valueAndTypeMismatch, 'bla', 'double')]
        '( abc )'  | 'bashArray' | true     | []
        'bla'      | 'bashArray' | true     | [] // Bash arrays can be empty. But then they actually may not contain whitespace
        'bla bla'  | 'bashArray' | true     | [] // Bash arrays need to be checked in a totally different way.
    }

    def 'test check proper dollar sign usage in variables'(value, type, expected, expectedWarnings, expectedErrors) {
        when:
        ConfigurationValue cval = new ConfigurationValue('bla', value, type)

        then:
        cval.valid == expected
        cval.warnings == expectedWarnings
        cval.errors == []

        where:
        value                   | type     | expected | expectedWarnings                          | expectedErrors
        'abc${var}bcd${var}'    | 'string' | true     | []                                        | []
        'abc${var}bcd${var}def' | 'string' | true     | []                                        | []
        'abc$'                  | 'string' | false    | [unattachedDollarCharacter.expand('bla')] | []
        'abc${'                 | 'string' | false    | [unattachedDollarCharacter.expand('bla')] | []
        'tr$ue'                 | 'string' | false    | [unattachedDollarCharacter.expand('bla')] | []

    }
}
