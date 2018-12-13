/*
 * Copyright (c) 2018 German Cancer Research Center (DKFZ).
 *
 * Distributed under the MIT License (https://opensource.org/licenses/MIT).
 */
package de.dkfz.roddy.client.cliclient.clioutput

import groovy.transform.CompileStatic
import org.petitparser.parser.Parser
import org.petitparser.parser.primitive.StringParser

import static org.petitparser.parser.primitive.CharacterParser.*

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
        name = "cvalues"
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
 *
 * Implementation:
 * * flatten internal values to strings where it makes sense (i.e. where the values are of use as unit) as early as possible (i.e. deep in the tree)
 * * start transformations of input (map(), flatten(), etc.) in a new line
  */
@CompileStatic
class CommandLineParameterParser {

    /**
     * Arbitrary parameters: .*
     *
     * "" is for the special case of roddy.sh ... '' ... . The parser should not break on this input. Its semantics
     * (if any)  is decided elsewhere.
      */
    static Parser arbitraryParameterExpr = any().star().end().         // .*
            flatten().
            map { String m ->
                new ArbitraryParameter(m)
            }

    static Parser parameterPrefix = of('-' as Character).times(2)

    /**
     * --.*
     *
     * This includes for instance "--", which is a common delimiter of parameter lists.
     */
    static Parser parameterWithoutValueExpr = parameterPrefix.seq(any().star().flatten()).end().
            map { List<String> r ->
                new ParameterWithoutValue(r.get(1))
            }

    /**
     * --..*=.*
     *
     * Parameters with values must have at least one character as name. Stop consuming the identifier at the first
     * encountered equal sign.
     */
    static Parser parameterWithValueExpr =
            parameterPrefix.seq(any().plusLazy(of('=' as Character)).flatten()).seq(of('=' as Character)).seq(any().star().flatten()).end().
            map { List<String> r ->
                new ParameterWithValue(r.get(1), r.get(3))
            }

    /**
     * Bash variable names: [a-zA-Z_][a-zA-Z0-9_]*
     */
    static Parser underscoreExpr = of('_' as Character)

    static Parser commaExpr = of(',' as Character)

    /**
     * Just a convenience name for backslash. '\'
     */
    static Parser escapeExpr = of('\\' as Character)

    /**
     * Commas are the variable assignment separators. Therefore, escaped commas are just commas. '\,'
     */
    static Parser escapedCommaExpr = escapeExpr.seq(commaExpr)

    /**
     * Escaped escapes are just escape-characters. '\\'
     */
    static Parser escapedEscapeExpr = escapeExpr.seq(escapeExpr)

    /**
     * Configuration value in the '--cvalues' value are either '\\' (first, to get consumed), '\,' (to ensure
     * escaped commas are taken as is, or non-commas. The matching ends at the first non-escaped comma that
     * is necessarily the delimiter to the next configuration variable.
     */
    static Parser variableValueExpr = escapedEscapeExpr.or(escapedCommaExpr).or(commaExpr.neg()).star().
            flatten()

    /** Although '_' is a bash-variable, it is none that can be set. Therefore here we have a dichotomy. */
    static Parser bashVariableNameExpr =
            letter().seq(word().or(underscoreExpr).star()).
            or(underscoreExpr.seq(word().or(underscoreExpr).plus())).
                    flatten()

    static Parser cvalueExpr = bashVariableNameExpr.seq(of(':' as Character).seq(variableValueExpr).optional()).
        map { List r ->
            new MapEntry(r.get(0), (r.getAt(1) as List)?.getAt(1))
        }

    static Parser cvalueSeparatorExpr = of(',' as Character).seq(whitespace().star()).
        map { null }      // simplify the returned value


    /**
     * The cvalueListExtensionExprs exists mostly to produce a clean CValueParameter list and thus simplify the map in cvalueListExpr.
     */
    static Parser cvalueListExtensionExpr = cvalueSeparatorExpr.seq(cvalueExpr).star().
        map { List<List> r -> r*.get(1) }  // return the cvalue MapEntry

    /**
     * For convenience, also empty '--cvalues=' is allowed. Therefore the optional().
     */
    static Parser cvalueListExpr = cvalueExpr.seq(cvalueListExtensionExpr).optional().
        map { List r ->
            if (null == r)
                []  // no cvalues, optional == empty
            else {
                [r.get(0)] + (r.get(1) as List)
            }
        }    // flatten out the list extension

    /**
     * Special subparser for --cvalues parameter that traverses into the content and ensures correct bash-variable
     * assignments (with colons as assignment operators) within.
     */
    static Parser cvalueParameterExpr =
            parameterPrefix.seq(StringParser.of('cvalues')).seq(of('=' as Character)).seq(cvalueListExpr).end().
            map { List<List> parseTree ->
                new CValueParameter(parseTree.get(3).toList().collectEntries())
            }

    static Parser commandLineParameterExpr =
            cvalueParameterExpr.
                    or(parameterWithValueExpr).
                    or(parameterWithoutValueExpr).
                    or(arbitraryParameterExpr)

}
