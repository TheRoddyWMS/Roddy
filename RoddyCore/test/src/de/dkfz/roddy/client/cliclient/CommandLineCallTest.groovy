/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.cliclient

import static de.dkfz.roddy.config.ConfigurationConstants.*

import de.dkfz.roddy.client.RoddyStartupModes
import de.dkfz.roddy.config.ConfigurationValue
import spock.lang.Specification

class CommandLineCallTest extends Specification {

    def "parse command line call"() {
        given:
        List<String> parameters = ['run', 'prj@ana',
                                   "--cvalues=a:( a bash array ),b:1.0,c:2:double,d:'quoted string',e:1.0f,f:1"]

        when:
        CommandLineCall clc = new CommandLineCall(parameters)

        then:
        clc.startupMode == RoddyStartupModes.run
        clc.analysisID == 'prj@ana'
        clc.parameters.size() == 1
        clc.parameters == ['prj@ana']
        clc.configuration.configurationValues.size() == 6
    }


    def "get typed configuration values from command-line call (success)"(String cvalueString, String type,
                                                                          Integer size, String id, String value) {
        when:
        CommandLineCall clc = new CommandLineCall(['run', 'prj@ana', "--cvalues=${cvalueString}"])
        List<ConfigurationValue> configurationValues = clc.configuration.configurationValues.allValuesAsList

        then:
        configurationValues.size() == size
        configurationValues[size - 1].id == id
        configurationValues[size - 1].evaluatedValue == value
        configurationValues[size - 1].type == type

        where:
        cvalueString               | type                    | size | id   | value
        ' i:(string array):string' | CVALUE_TYPE_STRING      |   1  | 'i'  | '(string array)'
        'a:( a bash array )'       | CVALUE_TYPE_BASH_ARRAY  |   1  | 'a'  | '( a bash array )'
        'b:1.0'                    | CVALUE_TYPE_DOUBLE      |   1  | 'b'  | '1.0'
        'c:2:double'               | CVALUE_TYPE_DOUBLE      |   1  | 'c'  | '2'
        "d:'quoted string'"        | CVALUE_TYPE_STRING      |   1  | 'd'  | "'quoted string'"
        'e:1.0f'                   | CVALUE_TYPE_FLOAT       |   1  | 'e'  | '1.0f'
        'f:1'                      | CVALUE_TYPE_INTEGER     |   1  | 'f'  | '1'
        'g:(without spaces array)' | CVALUE_TYPE_BASH_ARRAY  |   1  | 'g'  | '(without spaces array)'
        'h:escaped colon\\:double' | CVALUE_TYPE_STRING      |   1  | 'h'  | 'escaped colon:double'
        'j:unescaped colon:double' | CVALUE_TYPE_DOUBLE      |   1  | 'j'  | 'unescaped colon'
        'a:valA,b:${a}'            | CVALUE_TYPE_STRING      |   2  | 'b'  | 'valA'
    }


    def "split by non escaped character"(String query, List<String> split) {
        when:
        def result = CommandLineCall.splitByNonEscapedCharacter(query, ',' as Character)

        then:
        result == split

        where:
        query            | split
        ','              | []
        '\\,'            | ['\\,']
        '\\\\,'          | ['\\\\']
        '\\\\,x'         | ['\\\\', 'x']
        '\\\\\\,'        | ['\\\\\\,']
        '\\\\\\\\,'      | ['\\\\\\\\']
        '\\\\\\\\,x'     | ['\\\\\\\\', 'x']
    }

}
