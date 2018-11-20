package de.dkfz.roddy.config.validation

import de.dkfz.roddy.config.ConfigurationIssue
import de.dkfz.roddy.config.ConfigurationValue
import spock.lang.Specification

import static de.dkfz.roddy.config.ConfigurationIssue.ConfigurationIssueTemplate.*

class DefaultValidatorSpec extends Specification {

    def 'test removeEscapedEscapeCharacters'(value, expected) {
        expect:
        DefaultValidator.removeEscapedEscapeCharacters(value) == expected

        where:
        value        | expected
        "a \\\\ b"   | "a  b"
        "a \\\\\\ b" | "a \\ b"
        "a \\ b"     | "a \\ b"
        "a \\ b\\"   | "a \\ b\\"
        "a \\ b\\\\" | "a \\ b"
    }


    def 'test validation of types'(value, type, expected, expectedErrors) {
        when:
        ConfigurationValue cval = new ConfigurationValue('bla', value.toString(), type)

        then:
        cval.warnings == []
        cval.errors == expectedErrors
        cval.valid == expected

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
        '(abc)'    | 'bashArray' | true     | []
        'bla'      | 'bashArray' | true     | [] // Bash arrays can be empty. But then they actually may not contain whitespace
        'bla bla'  | 'bashArray' | true     | [] // Bash arrays need to be checked in a totally different way.
    }

    def 'test check proper dollar sign usage in variables'(value, type, expected, expectedWarnings, expectedErrors) {
        when:
        ConfigurationValue cval = new ConfigurationValue('bla', value, type)

        then:
        cval.warnings == expectedWarnings
        cval.errors == expectedErrors
        cval.valid == expected

        where:
        value                   | type     | expected | expectedWarnings                          | expectedErrors
        // All are ok
        'abc${var}bcd${var}'    | 'string' | true     | []                                        | []
        'abc${var}bcd${var}def' | 'string' | true     | []                                        | []

        '\\$'                   | 'string' | true     | []                                      | []  //Escaped dollar
        'abc\\$'                | 'string' | true     | []                                      | []  //Escaped dollar
        'abc\\${'               | 'string' | true     | []                                      | []  //Escaped dollar with open brace
        'a\\${b}c'              | 'string' | true     | []                                      | []  //Escaped dollar with complete braces

        // Not ok
        'abc$'                  | 'string' | false    | [detachedDollarCharacter.expand('bla')] | []
        'tr$ue'                 | 'string' | false    | [detachedDollarCharacter.expand('bla')] | []
        '$'                     | 'string' | false    | [detachedDollarCharacter.expand('bla')] | []
        'abc$\\{'               | 'string' | false    | [detachedDollarCharacter.expand('bla')] | []
    }

    def "test check for proper variable usage definition"() {
        when:
        ConfigurationValue cval = new ConfigurationValue('bla', value, type)

        then:
        cval.warnings == expectedWarnings
        cval.errors == expectedErrors
        cval.valid == expected

        where:
        value              | type     | expected | expectedWarnings                           | expectedErrors
        'abc${var}'        | "string" | true     | []                                         | []
        'abc\\${var}'      | "string" | true     | []                                         | []
        'abc${var}v${var}' | "string" | true     | []                                         | []
        '\\${c}'           | 'string' | true     | []                                         | []
        '\\${c'            | 'string' | true     | []                                         | []
        'abc${${var}}'     | "string" | false    | [inproperVariableExpression.expand('bla')] | []
        'abc${'            | 'string' | false    | [inproperVariableExpression.expand('bla')] | []
        'ab${c'            | 'string' | false    | [inproperVariableExpression.expand('bla')] | []
    }
}
