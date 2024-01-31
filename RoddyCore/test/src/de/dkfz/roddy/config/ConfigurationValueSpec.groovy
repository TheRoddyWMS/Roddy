/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */
package de.dkfz.roddy.config

import de.dkfz.roddy.RoddyTestSpec
import de.dkfz.roddy.RunMode
import de.dkfz.roddy.core.ContextResource
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.LocalExecutionService
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import org.junit.ClassRule
import spock.lang.Shared
import spock.lang.Specification

import static de.dkfz.roddy.config.ConfigurationConstants.CVALUE_TYPE_INTEGER
import static de.dkfz.roddy.config.ConfigurationConstants.CVALUE_TYPE_STRING

class ConfigurationValueSpec extends RoddyTestSpec {

    @Shared
    static final EnumerationValue evString = ConfigurationValue.defaultCValueTypeEnumeration.getValue(CVALUE_TYPE_STRING)

    @Shared
    static final EnumerationValue evInt = ConfigurationValue.defaultCValueTypeEnumeration.getValue(CVALUE_TYPE_INTEGER)

    def "get enumeration value type (cvalue type)"(cvalue, defaultType, expectedValue) {
        expect:
        cvalue.getEnumerationValueType(defaultType) == expectedValue

        where:
        cvalue                                                 | defaultType | expectedValue
        new ConfigurationValue("a", "b")                       | null        | evString
        new ConfigurationValue("a", "b")                       | evString    | evString
        new ConfigurationValue("b", "abc", CVALUE_TYPE_STRING) | null        | evString
        new ConfigurationValue("c", 1)                         | null        | evInt
    }

    def "test variable replacement for toFile()"() {
        when:
        ExecutionContext context = contextResource.createSimpleContext(ConfigurationValueSpec.class)
        def values = context.configurationValues
        values << new ConfigurationValue(context.configuration, "b", 'abc')
        values << new ConfigurationValue(context.configuration, "c", 'def')
        values << new ConfigurationValue(context.configuration, "a", '/tmp/${projectName}/${b}/something${c}fjfj/${dataSet}')
        values << new ConfigurationValue(context.configuration, "complex", '/$USERHOME/$USERNAME/$USERGROUP')

        then:
        values["complex"].toFile(context.analysis, context.dataSet).absolutePath.count("\$") == 0
        values["a"].toFile(context.analysis).absolutePath == "/tmp/TestProject/abc/somethingdeffjfj/\${dataSet}"
        values["a"].toFile(context.analysis, context.dataSet).absolutePath == "/tmp/TestProject/abc/somethingdeffjfj/TEST_PID"
    }

    @Deprecated
    def "test obsolete variable replacement upon cvalue creation"(value, expectedValue) {
        expect:
        new ConfigurationValue("bla", value).value == expectedValue

        where:
        value                | expectedValue
        'a $USERNAME value'  | 'a ${USERNAME} value'
        'a $USERGROUP value' | 'a ${USERGROUP} value'
        'a $USERHOME value'  | 'a ${USERHOME} value'
        'a $PWD value'       | "a \${$ExecutionService.RODDY_CVALUE_DIRECTORY_EXECUTION} value"
    }

    def "test bashArray"(value, expectedValue) {
        expect:
        new ConfigurationValue("bla", value).toStringList() == expectedValue

        where:
        value               | expectedValue
        ''                  | []
        'a'                 | ["a"]
        '( a b )'           | ["a", "b"]
        '(  a  b  )'        | ["a", "b"]
//        '(a  b)'            | ["a", "b"]     // The parser really sucks ... but not gonna change it significantly.

    }
}
