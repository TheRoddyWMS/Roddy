/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */
package de.dkfz.roddy.config.validation

import de.dkfz.roddy.RoddyTestSpec
import de.dkfz.roddy.config.ConfigurationConstants
import de.dkfz.roddy.config.ConfigurationIssue
import de.dkfz.roddy.config.ConfigurationValue
import spock.lang.Specification

import static de.dkfz.roddy.config.ConfigurationConstants.CVALUE_TYPE_BASH_ARRAY
import static de.dkfz.roddy.config.ConfigurationConstants.CVALUE_TYPE_BOOLEAN
import static de.dkfz.roddy.config.ConfigurationConstants.CVALUE_TYPE_DOUBLE
import static de.dkfz.roddy.config.ConfigurationConstants.CVALUE_TYPE_FLOAT
import static de.dkfz.roddy.config.ConfigurationConstants.CVALUE_TYPE_INTEGER
import static de.dkfz.roddy.config.ConfigurationConstants.CVALUE_TYPE_STRING
import static de.dkfz.roddy.config.ConfigurationIssue.ConfigurationIssueTemplate.*

class DefaultValidatorSpec extends RoddyTestSpec {

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
        value      | type                   | expected | expectedErrors
        1          | CVALUE_TYPE_INTEGER    | true     | []
        1.0f       | CVALUE_TYPE_FLOAT      | true     | []
        1.0        | CVALUE_TYPE_DOUBLE     | true     | []
        true       | CVALUE_TYPE_BOOLEAN    | true     | []
        'badvalue' | CVALUE_TYPE_BOOLEAN    | true     | [] // Validates always!
        'a'        | CVALUE_TYPE_INTEGER    | false    | [new ConfigurationIssue(valueAndTypeMismatch, 'bla', CVALUE_TYPE_INTEGER)]
        'b'        | CVALUE_TYPE_FLOAT      | false    | [new ConfigurationIssue(valueAndTypeMismatch, 'bla', CVALUE_TYPE_FLOAT)]
        'c'        | CVALUE_TYPE_DOUBLE     | false    | [new ConfigurationIssue(valueAndTypeMismatch, 'bla', CVALUE_TYPE_DOUBLE)]
        '( abc )'  | CVALUE_TYPE_BASH_ARRAY | true     | []
        '(abc)'    | CVALUE_TYPE_BASH_ARRAY | true     | []
        'bla'      | CVALUE_TYPE_BASH_ARRAY | true     | [] // Bash arrays can be empty. But then they actually may not contain whitespace
        'bla bla'  | CVALUE_TYPE_BASH_ARRAY | true     | [] // Bash arrays need to be checked in a totally different way.
    }

    def 'test check proper dollar sign usage in variables'(value, type, expected, expectedWarnings, expectedErrors) {
        when:
        ConfigurationValue cval = new ConfigurationValue('bla', value, type)

        then:
        cval.warnings == expectedWarnings
        cval.errors == expectedErrors
        cval.valid == expected

        where:
        value                   | type               | expected | expectedWarnings                        | expectedErrors
        // All are ok
        'abc${var}bcd${var}'    | CVALUE_TYPE_STRING | true     | []                                      | []
        'abc${var}bcd${var}def' | CVALUE_TYPE_STRING | true     | []                                      | []

        '\\$'                   | CVALUE_TYPE_STRING | true     | []                                      | []  //Escaped dollar
        'abc\\$'                | CVALUE_TYPE_STRING | true     | []                                      | []  //Escaped dollar
        'abc\\${'               | CVALUE_TYPE_STRING | true     | []                                      | []  //Escaped dollar with open brace
        'a\\${b}c'              | CVALUE_TYPE_STRING | true     | []                                      | []  //Escaped dollar with complete braces

        // Not ok
        'abc$'                  | CVALUE_TYPE_STRING | false    | [detachedDollarCharacter.expand('bla')] | []
        'tr$ue'                 | CVALUE_TYPE_STRING | false    | [detachedDollarCharacter.expand('bla')] | []
        '$'                     | CVALUE_TYPE_STRING | false    | [detachedDollarCharacter.expand('bla')] | []
        'abc$\\{'               | CVALUE_TYPE_STRING | false    | [detachedDollarCharacter.expand('bla')] | []
    }

    def "test check for proper variable usage definition"() {
        when:
        ConfigurationValue cval = new ConfigurationValue('bla', value, type)

        then:
        cval.warnings == expectedWarnings
        cval.errors == expectedErrors
        cval.valid == expected

        where:
        value              | type               | expected | expectedWarnings                           | expectedErrors
        'abc${var}'        | CVALUE_TYPE_STRING | true     | []                                         | []
        'abc\\${var}'      | CVALUE_TYPE_STRING | true     | []                                         | []
        'abc${var}v${var}' | CVALUE_TYPE_STRING | true     | []                                         | []
        '\\${c}'           | CVALUE_TYPE_STRING | true     | []                                         | []
        '\\${c'            | CVALUE_TYPE_STRING | true     | []                                         | []
        'abc${${var}}'     | CVALUE_TYPE_STRING | false    | [inproperVariableExpression.expand('bla')] | []
        'abc${'            | CVALUE_TYPE_STRING | false    | [inproperVariableExpression.expand('bla')] | []
        'ab${c'            | CVALUE_TYPE_STRING | false    | [inproperVariableExpression.expand('bla')] | []
    }
}
