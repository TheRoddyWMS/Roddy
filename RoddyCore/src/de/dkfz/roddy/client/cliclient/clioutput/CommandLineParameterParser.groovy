/*
 * Copyright (c) 2018 German Cancer Research Center (DKFZ).
 *
 * Distributed under the MIT License (https://opensource.org/licenses/MIT).
 */
package de.dkfz.roddy.client.cliclient.clioutput

import com.google.common.collect.ImmutableMap
import groovy.transform.CompileStatic
import org.petitparser.parser.Parser
import org.petitparser.parser.primitive.FailureParser
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

/** CValue is a value object and not modifiable. Therefore there are equals and hashCode methods. */
@CompileStatic
class CValue {
    final String name
    final String value
    final String type

    CValue(String name, String value = null, String type = null) {
        assert(null != name)
        this.name = name
        this.value = value
        this.type = type
    }

    String toString() {
        "CValue('$name','$value','$type')"
    }

    String toCValueParameterString() {
        def res = name + ':' + value
        if (type != null) {
            res += ':' + type
        }
        res
    }

    @Override
    int hashCode() {
        Objects.hash(name, value, type)
    }

    @Override
    boolean equals(Object obj) {
        if(obj == null)
            return false
        if(!(obj instanceof CValue))
            return false

        CValue other = obj as CValue
        name == other.name &&
                value == other.value &&
                type == other.type
    }

}

@CompileStatic
class CValueParameter extends ParameterWithValue {

    private final ImmutableMap<String, CValue> cvaluesMap

    private static String cvalueMapToValue(Map<String, CValue> cvalues) {
        cvalues.values()*.toCValueParameterString().join(',')
    }

    CValueParameter(Map<String, CValue> cvalues) {
        super('cvalues', cvalueMapToValue(cvalues))
        this.cvaluesMap = ImmutableMap.builder().putAll(cvalues).build() as ImmutableMap<String, CValue>
    }

    Map<String, CValue> getCvaluesMap() {
        cvaluesMap.collectEntries { it } as Map<String, CValue>
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

    static Parser colonExpr = of(':' as Character)

    /**
     * Just a convenience name for backslash. '\'
     */
    static Parser escapeExpr = of('\\' as Character)

    /**
     * Commas are the variable assignment separators. Therefore, escaped commas are just commas. '\,'
     */
    static Parser escapedCommaExpr = escapeExpr.seq(commaExpr).
            map { List r -> r.get(1) }    // strip off escape

    /**
     * Escaped escapes are just escape-characters. '\\'
     */
    static Parser escapedEscapeExpr = escapeExpr.seq(escapeExpr).
            map { List r -> r.get(1) }    // strip off escape

    /**
     * Colons ':' separate values from types declarations (value:type)
     */
    static Parser escapedColonExpr = escapeExpr.seq(colonExpr).
            map { List r -> r.get(1) }    // strip off escape

    /**
     * Configuration value in the '--cvalues' value are either '\\' (first, to get consumed), '\,' (to ensure
     * escaped commas are taken as is, or non-commas. The matching ends at the first non-escaped comma that
     * is necessarily the delimiter to the next configuration variable.
     */
    static Parser variableValueExpr = escapedEscapeExpr.or(escapedCommaExpr).or(escapedColonExpr).or(commaExpr.or(colonExpr).neg()).star().
            map { List r -> r.join('') }  // flatten() didn't work: it kept the escapes that should be stripped already be the nested expressions

    /** Note that although '_' is a bash-variable, it is none that can be set. */
    static Parser bashVariableNameExpr =
            letter().seq(word().or(underscoreExpr).star()).
            or(underscoreExpr.seq(word().or(underscoreExpr).plus())).
                    trim().     // ignore whitespace around variable names during parsing
                    map { List r ->
                        r.flatten().join('').trim()
                    }

    static Parser variableTypeExpr = word().plus().
        flatten()

    static Parser separatedVariableValueExpr = colonExpr.seq(variableValueExpr).
        map { List r -> r.get(1) }   // drop the ':'

    static Parser separatedVariableTypeExpr = colonExpr.seq(variableTypeExpr).
        map { List r -> r.get(1) }   // drop the ':'

    /**
     * A complete --cvalue option value must include a variable name and a colon as kind of declaration operator and an optionally empty value ('').
     * It may or may not contain a variable type declaration that itself must not be empty (so rather leave the type out, empty type names are not allowed.
     */
    static Parser cvalueExpr =
            bashVariableNameExpr.seq(separatedVariableValueExpr.seq(separatedVariableTypeExpr.optional())).
        map { List r ->
            String name = r[0]
            if (null == name) {
                return null
            } else {
                String value = (r[1] as List)?.getAt(0)
                String type =  (r[1] as List)?.getAt(1)
                new MapEntry(r.get(0), new CValue(name, value, type))
            }
        }

    static Parser cvalueSeparatorExpr = commaExpr.seq(whitespace().star()).
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
            parameterPrefix.seq(StringParser.of('cvalues')).
                    seq(of('=' as Character).or(FailureParser.withMessage("No content"))).
                    seq(cvalueListExpr.or(FailureParser.withMessage("Malformed content"))).
            map { List<List> parseTree ->
                new CValueParameter(parseTree.get(3).toList().collectEntries() as Map<String,CValue>)
            }

}
