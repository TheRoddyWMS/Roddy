/*
 * Copyright (c) 2018 German Cancer Research Center (DKFZ).
 *
 * Distributed under the MIT License (https://opensource.org/licenses/MIT).
 */
package de.dkfz.roddy.client.cliclient.clioutput

import groovy.transform.CompileStatic
import org.parboiled.BaseParser
import org.parboiled.Rule
import org.parboiled.annotations.BuildParseTree

@CompileStatic
trait Parameter {
    String name
}

@CompileStatic
class ArbitraryParameter implements Parameter {

    ArbitraryParameter(String name) {
        assert null != name
        this.name = name
    }

}

@CompileStatic
class ParameterWithoutValue implements Parameter {

    ParameterWithoutValue(String name) {
        assert null != name
        this.name = name
    }

}

@CompileStatic
class ParameterWithValue implements Parameter {

    String value

    ParameterWithValue(String name, String value) {
        assert null != name
        this.name = name
        this.value = value
    }

}

@CompileStatic
class CValueParameter implements Parameter {

    Map<String, String> cvalues

    CValueParameter(Map<String, String> cvalues) {
        assert null != cvalues
        name = "cvalue"
        this.cvalues = cvalues
    }

}


/** A parser for commandline values. The assumptions are
 *
 * * Parameters are provide in the form '--parameter=value' to the parser.
 * * On the Shell command-line the parameters are first evaluated according to the shell's evaluation rules,
 *   e.g. concerning quoting, escaping, dollar. E.g. '--parameter="varA:valA"' becomes '--parameter=varA:valA'
 * * Generally, empty values are allowed and accepted by the parser. We leave it to the external code to
 *   decide about the semantics. Also the commonly used '--' parameter is allowed.
  */
@CompileStatic
@BuildParseTree
class CommandLineParameterParser extends BaseParser<Parameter> {

    Rule CommandLineParameterExpr() {
        FirstOf(//CValueParameterExpr(),
                //ParameterWithValueExpr(),
                //ParameterWithoutValueExpr(),
                ArbitraryParameterExpr(),
                EOI)
    }

    Rule ArbitraryParameterExpr() {
        Sequence(OneOrMore(ANY), push(new ArbitraryParameter(match())))
        // Zero is for the special case of roddy.sh ... '' ... . The parser should not break on this input
        // as the semantics is decided elsewhere.
    }

//    Rule ParameterWithoutValueExpr() {
//        Sequence('--', VariableNameExpr(),
//                push(new ParameterWithoutValue(match())))
//    }
//
//    Rule ParameterWithValueExpr() {
//        def name = new Var<String>()
//        def value = new Var<String>()
//        Sequence('--', VariableNameExpr(),name.set(match()),
//                '=', ZeroOrMore(ANY), value.set(match()) ,
//                push(new ParameterWithValue(name.toString(), value.toString())))
//        // This also allows for empty values.
//    }
//
//    Rule CValueParameterExpr() {
//        def name = new Var<String>()
//        def cvalues = new Var<Map<String,String>>([:])
//        Sequence('--', VariableNameExpr(),name.set(match()),
//                '=', CValueParameterValueExpr(cvalues),
//                push(new CValueParameter(cvalues.get())))
//    }
//
//
//    Rule CValueParameterValueExpr(Var<Map<String,String>> cvalues) {
//        Optional(             // allow empty cvalues parameter
//                Sequence(
//                        VariableExpression(cvalues),
//                        ZeroOrMore(',', VariableExpression(cvalues))))
//    }
//
//    Rule VariableExpression(Var<Map<String,String>> cvalues) {
//        def name = new Var<String>()
//        def value = new Var<String>()
//        Sequence(
//                VariableDeclaratorExpr(name),
//                ":",
//                VariableValueExpr(), value.set(match()),
//                cvalues.get().put(name.get(), value.get())
//        )
//    }
//
//    // Variable declarators are variable names with optional surrounding whitespace. Whitespace around variable
//    // names will be ignored, though.
//    Rule VariableDeclaratorExpr(Var<String> name) {
//        Sequence(ZeroOrMore(' '), BashIdentifierExpr(), name.set(match()), ZeroOrMore(' '))
//    }
//
//    @SuppressSubnodes
//    Rule BashIdentifierNameExpr() {
//        Sequence(FirstOf('_', AlphaExpr()), ZeroOrMore(FirstOf('_', AlphaNumericExpr())))
//    }
//
//    // Variable values are just the full part between the colon and the comma. This is the convention used until
//    // now in Roddy. However, comma has to be considered as special characters in variable values because it also
//    // marks the value end. Therefore it needs to be escaped -- like the escape character itself.
//    @SuppressSubnodes
//    Rule VariableValueExpr() {
//        ZeroOrMore(FirstOf(EscapedEscapeExpr(), EscapedCommaExpr(), ANY))
//    }
//
//    @SuppressSubnodes
//    Rule EscapedCommaExpr() {
//        Sequence('\\', ',')
//    }
//
//    @SuppressSubnodes
//    Rule EscapedEscapeExpr() {
//        Sequence('\\', '\\')
//    }
//
//    @SuppressSubnodes
//    Rule AlphaNumericExpr() {
//        FirstOf(AlphaExpr(), NumericExpr())
//    }
//
//    @SuppressSubnodes
//    Rule AlphaExpr() {
//        FirstOf(CharRange('a' as Character, 'z' as Character), CharRange('A' as Character, 'Z' as Character))
//    }
//
//    @SuppressSubnodes
//    Rule NumericExpr() {
//        CharRange('0' as Character, '9' as Character)
//    }

}
