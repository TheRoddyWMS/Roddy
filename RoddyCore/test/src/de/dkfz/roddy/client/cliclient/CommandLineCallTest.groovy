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
        List<String> parameters = ['run', 'prj@ana', "--cvalues=a:( a bash array ),b:1.0,c:2:double,d:'quoted string',e:1.0f,f:1"]

        when:
        CommandLineCall clc = new CommandLineCall(parameters)

        then:
        clc.startupMode == RoddyStartupModes.run
        clc.analysisID == 'prj@ana'
        clc.parameters.size() == 1
        clc.parameters == ['prj@ana']
        clc.configurationValues.size() == 6
    }


    def "get typed configuration values from command-line call (success)"(String cvalues, String type, String id, String value) {
        when:
        CommandLineCall clc = new CommandLineCall(['run', 'prj@ana', "--cvalues=${cvalues}"])
        List<ConfigurationValue> configurationValues = clc.configurationValues

        then:
        configurationValues.size() == 1
        configurationValues[0].id == id
        configurationValues[0].value == value
        configurationValues[0].type == type

        where:
        cvalues                    | type                    | id   | value
        ' i:(string array):string' | CVALUE_TYPE_STRING      | 'i'  | '(string array)'
        'a:( a bash array )'       | CVALUE_TYPE_BASH_ARRAY  | 'a'  | '( a bash array )'
        'b:1.0'                    | CVALUE_TYPE_DOUBLE      | 'b'  | '1.0'
        'c:2:double'               | CVALUE_TYPE_DOUBLE      | 'c'  | '2'
        "d:'quoted string'"        | CVALUE_TYPE_STRING      | 'd'  | "'quoted string'"
        'e:1.0f'                   | CVALUE_TYPE_FLOAT       | 'e'  | '1.0f'
        'f:1'                      | CVALUE_TYPE_INTEGER     | 'f'  | '1'
        'g:(without spaces array)' | CVALUE_TYPE_BASH_ARRAY  | 'g'  | '(without spaces array)'
        'h:escaped colon\\:double' | CVALUE_TYPE_STRING      | 'h'  | 'escaped colon:double'
        'j:unescaped colon:double' | CVALUE_TYPE_DOUBLE      | 'j'  | 'unescaped colon'
    }

    def "get typed configuration values from command-line call (failure)"(String parameter) {
        when:
        CommandLineCall clc = new CommandLineCall(['run', 'prj@ana', parameter])

        then:
        clc.malformed

        where:
        parameter       | _
        '--cvalues'     | _
        '--cvalues=a'   | _
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
