package de.dkfz.roddy.config

import spock.lang.Shared
import spock.lang.Specification

class ConfigurationValueSpec extends Specification {
//    def "GetEnumerationValueType"() {
//    }
//
    @Shared
    static final EnumerationValue evString = ConfigurationValue.defaultCValueTypeEnumeration.getValue("string")

    @Shared
    static final EnumerationValue evInt = ConfigurationValue.defaultCValueTypeEnumeration.getValue("integer")

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
}
