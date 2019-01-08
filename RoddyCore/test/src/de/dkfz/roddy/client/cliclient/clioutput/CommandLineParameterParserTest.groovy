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
        def result = CommandLineParameterParser.arbitraryParameterExpr.end().parse(target)

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
        def result = CommandLineParameterParser.parameterWithoutValueExpr.end().parse(target)

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
        def result = CommandLineParameterParser.parameterWithoutValueExpr.end().parse(target)

        then:
        !result.success

        where:
        target | _
        ''     | _
    }




    def "parameter with value (success)"(String target, String name, String value) {
        when:
        def result = CommandLineParameterParser.parameterWithValueExpr.end().parse(target)

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
        def result = CommandLineParameterParser.parameterWithValueExpr.end().parse(target)

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
        def result = CommandLineParameterParser.cvalueParameterExpr.end().parse(target)

        then:
        result.success
        result.get().getClass() == CValueParameter
        (result.get() as CValueParameter).name == 'cvalues'
        (result.get() as CValueParameter).cvalues == cvalues


        where:
        target                             | cvalues
        '--cvalues='                       | [:]                 // reasonable
        '--cvalues=bla:blub'               | [bla:new CValue('bla','blub')]
        '--cvalues=a:'                     | [a:new CValue('a', '')]
        '--cvalues=a: '                    | [a:new CValue('a', ' ')]
        '--cvalues= a:b'                   | [a:new CValue('a', 'b')]    // ignore spaces around variable names
        '--cvalues=a:( )'                  | [a:new CValue('a', '( )')]
        '--cvalues=a: ( )'                 | [a:new CValue('a', ' ( )')]
        "--cvalues=a:' '"                  | [a:new CValue('a', "' '")]
        '--cvalues=a::string'              | [a:new CValue('a', '', 'string')] // colons as values are allowed, a type may follow
        '--cvalues=a:\\: --parameter'      | [a:new CValue('a', ': --parameter')] // input is preprocessed parameters, not command-lines
        '--cvalues=a:\\:,b:c'              | [a:new CValue('a', ':'), b:new CValue('b', 'c')] // escaped colons are fine
        '--cvalues=a:\\,,b:c'              | [a:new CValue('a', ','), b:new CValue('b', 'c')] // escaped commas are fine
        '--cvalues=a:b,c:d'                | [a:new CValue('a', 'b'), c:new CValue('c', 'd')]
        '--cvalues=a:b,c:d ,e:f'           | [a:new CValue('a', 'b'), c:new CValue('c', 'd '), e:new CValue('e', 'f')]
        '--cvalues=a:b, c:d'               | [a:new CValue('a', 'b'), c:new CValue('c', 'd')]
        '--cvalues=a:b,c:'                 | [a:new CValue('a', 'b'), c:new CValue('c', '')]
        '--cvalues=_a:b'                   | [_a:new CValue('_a', 'b')]
        '--cvalues=a:A1,b:B, a:A2'         | [a:new CValue('a', 'A2') , b:new CValue('b', 'B')]
        // later redefinition overrides earlier definition; key output order does is arbitrary (thus not [b:'B', a:'A2']
    }

    def "cvalue parameter (failure)"(String target) {
        when:
        def result = CommandLineParameterParser.cvalueParameterExpr.end().parse(target)

        then:
        !result.success

        where:
        target                             | _
        '--cvalues'                        | _  // cvalue parameter without parameter list doesn't match
        '--cval=a:yip'                     | _  // cvalue parameter name doesn't match

        '--cvalues=bla'                    | _  // force explicit definition with colon

        '--cvalues=a:,'                    | _  // empty cvalue (second)
        '--cvalues=,a:'                    | _  // empty cvalue (first)
        '--cvalues=a: ,'                   | _  // empty cvalue (second)

        '--cvalues=0a:'                    | _  // not Bash identifier
        '--cvalues=_^:nope'                | _  // not Bash identifier

        '--cvalues=a::'                    | _  // empty type is not allowed

        '--cvalues=a:\\\\,,b'              | _  // incorrectly escaped comma (in the middle)
    }

    def "bash variable parser (success)"(String target) {
        when:
        def result = CommandLineParameterParser.bashVariableNameExpr.end().parse(target)

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
        def result = CommandLineParameterParser.bashVariableNameExpr.end().parse(target)

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

    def "complete commandline parameter parser test (success)"(String target, Class resultClass) {
        when:
        def result = CommandLineParameterParser.commandLineParameterExpr.end().parse(target)

        then:
        result.success
        result.get().getClass() == resultClass

        where:
        target            | resultClass
        'xy'              | ArbitraryParameter
        '--'              | ParameterWithoutValue
        '--abc'           | ParameterWithoutValue
        '--abc=val'       | ParameterWithValue
        '--cvalues='      | CValueParameter
        '--cvalues=a:b'   | CValueParameter
        "--cvalues=a:( a bash array ),b:1.0,c:2:double,d:'quoted string',e:1.0f,f:1" | CValueParameter
    }

}
