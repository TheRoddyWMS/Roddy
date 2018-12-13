/*
 * Copyright (c) 2018 German Cancer Research Center (DKFZ).
 *
 * Distributed under the MIT License (https://opensource.org/licenses/MIT).
 */
package de.dkfz.roddy.client.cliclient.clioutput


import spock.lang.Specification

class CommandLineParameterParserTest extends Specification {

    def "arbitrary parameter"(String target) {
        when:
        def result = CommandLineParameterParser.arbitraryParameterExpr.parse(target)

        then:
        result.success
        result.get().class == ArbitraryParameter.class
        result.get().name == target

        where:
        target | _
        ''     | _
        'run'  | _
        '\"'   | _
    }


    def "parameter without value (success)"(String target, String name) {
        when:
        def result = CommandLineParameterParser.parameterWithoutValueExpr.parse(target)

        then:
        result.success
        result.get().class == ParameterWithoutValue
        result.get().name == name

        where:
        target     | name
        '--'       | ''        // this is a common pattern to separate two lists of different parameter sets
        '--appIni' | 'appIni'
    }

    def "parameter without value (failure)"(String target) {
        when:
        def result = CommandLineParameterParser.parameterWithoutValueExpr.parse(target)

        then:
        !result.success

        where:
        target | _
        ''     | _
    }




    def "parameter with value (success)"(String target, String name, String value) {
        when:
        def result = CommandLineParameterParser.parameterWithValueExpr.parse(target)

        then:
        result.success
        result.get().class == ParameterWithValue
        result.get().name == name
        result.get().value == value

        where:
        target              |   name        | value
        '--appIni='         |    'appIni'   | ''           // reasonable
        '--appIni=bla'      |    'appIni'   | 'bla'
        '--appIni=bla=blub' |    'appIni'   | 'bla=blub'    // stop parameter name at first equal sign
        '--appIni=bla blub' |    'appIni'   | 'bla blub'    // if it was passed by the shell, it is the value
    }

    def "parameter with value (failure)"(String target) {
        when:
        def result = CommandLineParameterParser.parameterWithValueExpr.parse(target)

        then:
        !result.success

        where:
        target   | _
        '--='    | _
        '--=bla' | _
        '--'     | _
    }


    def "cvalue parameter (success)"(String target, Map<String,String> cvalues) {
        when:
        def result = CommandLineParameterParser.cvalueParameterExpr.parse(target)

        then:
        result.success
        result.get().getClass() == "CValueParameter"
        (result.get() as CValueParameter).name == 'cvalues'
        (result.get() as CValueParameter).cvalues == cvalues


        where:
        target                             | cvalues
        '--cvalues='                       | [:]                 // reasonable
        '--cvalues=bla'                    | [bla:null]
        '--cvalues=bla:blub'               | [bla:'blub']
        '--cvalues=a:'                     | [a:'']
        '--cvalues=a: '                    | [a:' ']
        '--cvalues=a:( )'                  | [a:'( )']
        '--cvalues=a: ( )'                 | [a:' ( )']
        "--cvalues=a:' '"                  | [a:"' '"]
        '--cvalues=a::'                    | [a:':']              // colons as values are allowed
        '--cvalues=a:\\: --malformedInput' | [a:'\\: --malformedInput']   // input is preprocessed parameters, not command-lines
        '--cvalues=a:b,c:d'                | [a:'b', c:'d']
        '--cvalues=a:b,c:d ,e:f'           | [a:'b', c:'d ', e:'f']
        '--cvalues=a:b, c:d'               | [a:'b', c:'d']
        '--cvalues=a:b,c:'                 | [a:'b', c:'']
        '--cvalues=_a:b'                   | [_a:'b']
        '--cvalues=a::,b'                  | [a:':', b:null]
        '--cvalues=a:\\,,b'                | [a:',', b:null]      // escaped commas are fine
        '--cvalues=a:\\:,b:c'              | [a:'\\:', b:'c']
    }

    def "cvalue parameter (failure)"(String target) {
        when:
        def result = CommandLineParameterParser.cvalueParameterExpr.parse(target)

        then:
        !result.success

        where:
        target                             | _
        '--cval=a:yip'                     | _  // cvalue parameter name doesn't match

        '--cvalues=a:,'                    | _  // empty cvalue (second)
        '--cvalues=,a:'                    | _  // empty cvalue (first)
        '--cvalues=a: ,'                   | _  // empty cvalue (second)

        '--cvalues=0a:'                    | _  // not Bash identifier
        '--cvalues=_^:nope'                | _  // not Bash identifier

        '--cvalues=a:\\\\,,b'              | _  // incorrectly escaped comma (in the middle)
    }

    def "bash variable parser (success)"(String target) {
        when:
        def result = CommandLineParameterParser.bashVariableNameExpr.parse(target)

        then:
        result.success

        where:
        target     | _
        '_a'       | _
        '__'       | _
        '_1'       | _
        'a_'       | _
        'a_1'      | _
        'ab_x1_'    | _
    }

    def "bash variable parser (failure)"(String target) {
        when:
        def result = CommandLineParameterParser.bashVariableNameExpr.parse(target)

        then:
        !result.success

        where:
        target     | _
        '_'        | _
        '1'        | _
        '1_'       | _
        '1a'       | _
        '.'        | _
        '/'        | _
        '\\'       | _
        '^'        | _
    }

    def "complete commandline parameter parser test (success)"(String target) {
        when:
        def result = CommandLineParameterParser.commandLineParameterExpr.parse(target)

        then:
        result.success

        where:
        target            | _
        '--'              | _
        '--abc'           | _
        '--cvalues'       | _
        "--cvalues='a:b'" | _
    }

}
