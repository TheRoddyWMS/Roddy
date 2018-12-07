/*
 * Copyright (c) 2018 German Cancer Research Center (DKFZ).
 *
 * Distributed under the MIT License (https://opensource.org/licenses/MIT).
 */
package de.dkfz.roddy.client.cliclient.clioutput


import org.parboiled.Parboiled
import org.parboiled.parserunners.ReportingParseRunner
import org.parboiled.support.ParsingResult
import spock.lang.Specification

class CommandLineParameterParserTest extends Specification {

    CommandLineParameterParser parser = Parboiled.createParser(CommandLineParameterParser.class)

    def "arbitrary parameter"(String target, String name) {
        when:
        ParsingResult<Parameter> result = new ReportingParseRunner(parser.CommandLineParameterExpr()).run(target)

        then:
        result.parseErrors.isEmpty()
        result.resultValue.class == ArbitraryParameter
        (result.resultValue as ArbitraryParameter).name == name

        where:
        target     |   name
        ""         |    ""
        "run"      |    "run"
    }


    def "parameter without value"(String target, String name) {
        when:
        ParsingResult<Parameter> result = new ReportingParseRunner(parser.CommandLineParameterExpr()).run(target)

        then:
        result.parseErrors.isEmpty()
        result.class == ParameterWithoutValue
        (result.resultValue as ParameterWithoutValue).name == name

        where:
        target     |   name
        "--"       |    ""         // this is a common pattern to separate two lists of different parameter sets
        "--appIni" |    "appIni"
    }


    def "parameter with value"(String target, String name, String value) {
        when:
        ParsingResult<Parameter> result = new ReportingParseRunner(parser.CommandLineParameterExpr()).run(target)

        then:
        assert result.parseErrors.isEmpty()
        result.class == ParameterWithValue
        (result.resultValue as ParameterWithValue).name == name
        (result.resultValue as ParameterWithValue).value == value

        where:
        target              |   name        | value
        "--appIni="         |    "appIni"   | ""      // reasonable
        "--appIni=bla"      |    "appIni"   | "bla"
        "--appIni=bla blub" |    "appIni"   | "bla blub"   // if it was passed by the shell, it is the value


        // TODO Fail on '--=' or '--=bla'
    }


    def "cvalue parameter"(String target, String name, Map<String,String> cvalues) {
        when:
        ParsingResult<Parameter> result = new ReportingParseRunner(parser.CommandLineParameterExpr()).run(target)

        then:
        result.parseErrors.isEmpty()
        result.class == CValueParameter
        (result.resultValue as CValueParameter).name == name
        (result.resultValue as CValueParameter).cvalues == cvalues

        where:
        target                             |   name        | cvalues
        '--cvalues='                       |    'cvalues'  | [:]      // reasonable
        '--cvalues=bla'                    |    'cvalues'  | [bla:null]
        '--cvalues=bla:blub'               |    'cvalues'  | [bla:'blub']
        '--cvalues=a:'                     |    'cvalues'  | [a:'']
        '--cvalues=a: '                    |    'cvalues'  | [a:' ']
        '--cvalues=a:( )'                  |    'cvalues'  | [a:'( )']
        '--cvalues=a: ( )'                 |    'cvalues'  | [a:' ( )']
        '--cvalues=a:b,c:d'                |    'cvalues'  | [a:'b', c:'d']
        '--cvalues=a:b,c:'                 |    'cvalues'  | [a:'b', c:'']
        '--cvalues=_a:b'                   |    'cvalues'  | [_a:'b']
        "--cvalues=a:' '"                  |    'cvalues'  | [a:"' '"]            // do not interpret the content!
        '--cvalues=a::'                    |    'cvalues'  | [a:':']              // colons as values are allowed
        '--cvalues=a::,b'                  |    'cvalues'  | [a:':', b:null]
        '--cvalues=a:\\,,b'                |    'cvalues'  | [a:',', b:null]      // escaped commas are fine
        '--cvalues=a:\\\\,,b'              |    'cvalues'  | [a:'\\,', b:null]    // escaped escapes are fine
        '--cvalues=a:\\:,b:c'              |    'cvalues'  | [a:'\\:', b:'c']
        '--cvalues=a:\\: --malformedInput' |    'cvalues'  | [a:'\\: --malformedInput']   // input is preprocessed parameters, not command-lines

        // TODO Fail on
        // "--cvalues=a:,"   // empty second cvalue
        // "--cvalues=0a:"   // not Bash identifier
        // "--cvalues=_^:nope"  // not Bash identifier
    }



}
