package de.dkfz.roddy.config

import de.dkfz.roddy.RunMode
import de.dkfz.roddy.core.ContextResource
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.LocalExecutionService
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import org.junit.ClassRule
import spock.lang.Shared
import spock.lang.Specification

class ConfigurationValueSpec extends Specification {
//    def "GetEnumerationValueType"() {
//    }
//

    @ClassRule
    static ContextResource contextResource = new ContextResource() {
        {
            before()
        }
    }

    def setupSpec() {
        ExecutionService.initializeService(LocalExecutionService, RunMode.CLI)
        FileSystemAccessProvider.initializeProvider(true)
    }

    @Shared
    static final EnumerationValue evString = Enumeration.defaultCValueTypeEnumeration.getValue("string")

    @Shared
    static final EnumerationValue evInt = Enumeration.defaultCValueTypeEnumeration.getValue("integer")

    def "get enumeration value type (cvalue type)"(cvalue, defaultType, expectedValue) {
        expect:
        cvalue.getEnumerationValueType(defaultType) == expectedValue

        where:
        cvalue                                       | defaultType | expectedValue
        new ConfigurationValue("a", "b")             | null        | evString
        new ConfigurationValue("a", "b")             | evString    | evString
        new ConfigurationValue("b", "abc", "string") | null        | evString
        new ConfigurationValue("c", 1)               | null        | evInt
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
}
