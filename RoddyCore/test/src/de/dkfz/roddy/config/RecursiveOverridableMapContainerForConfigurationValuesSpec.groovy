package de.dkfz.roddy.config

import spock.lang.Ignore
import spock.lang.Specification

class RecursiveOverridableMapContainerForConfigurationValuesSpec extends Specification {

    def "Get"() {
        given:
        String containerId = "testContainer"
        RecursiveOverridableMapContainerForConfigurationValues container =
                new RecursiveOverridableMapContainerForConfigurationValues<>(
                        new Configuration(),
                        containerId)

        container.put("existingKey", "existingValue")
        when:
        String nonExistingResultWithImplicitDefault = container.get("nonExistingKey")
        String nonExistingResultWithExplicitDefault = container.get("nonExistingKey", "defaultValue")
        String existingResult = container.get("existingKey")


        then:
        nonExistingResultWithImplicitDefault == ""
        nonExistingResultWithExplicitDefault == "defaultValue"
        existingResult == "existingValue"
    }

    def "GetOrThrow"() {
        given:
        String containerId = "testContainer"
        RecursiveOverridableMapContainerForConfigurationValues container =
                new RecursiveOverridableMapContainerForConfigurationValues<>(
                        new Configuration(),
                        containerId)

        when:
        container.getOrThrow("nonExistingKey")

        then:
        thrown(ConfigurationError)
    }

    def "GetAt"() {
        // Same as get(key), with implicit default value of ""
        given:
        String containerId = "testContainer"
        RecursiveOverridableMapContainerForConfigurationValues container =
                new RecursiveOverridableMapContainerForConfigurationValues<>(
                        new Configuration(),
                        containerId)
        container.put("existingKey", "existingValue")
        when:
        String nonExistingValue = container.getAt("nonExistingKey")
        String existingValue = container.getAt("existingKey")

        then:
        nonExistingValue == ""
        existingValue == "existingValue"
    }

    def "GetBoolean"() {
        given:
        String containerId = "testContainer"
        RecursiveOverridableMapContainerForConfigurationValues container =
                new RecursiveOverridableMapContainerForConfigurationValues<>(
                        new Configuration(),
                        containerId)
        container.put("trueKey", "true")

        when:
        Boolean trueValue = container.getBoolean("trueKey")
        Boolean falseValue = container.getBoolean("falseKey", false)
        Boolean explicitDefaultValue = container.getBoolean("nonExistingKey", true)
        Boolean implicitDefaultValue = container.getBoolean("anotherNonExistingKey")  // Should throw

        then:
        trueValue == true
        falseValue == false
        explicitDefaultValue == true
//        thrown(ConfigurationError)
        implicitDefaultValue == false
    }

    def "GetInteger"() {
        given:
        String containerId = "testContainer"
        RecursiveOverridableMapContainerForConfigurationValues container =
                new RecursiveOverridableMapContainerForConfigurationValues<>(
                        new Configuration(),
                        containerId)
        container.put("intKey", "42")

        when:
        Integer intValue = container.getInteger("intKey")
        Integer explicitDefaultValue = container.getInteger("nonExistingKey", 7)
        container.getInteger("anotherNonExistingKey") // Should throw

        then:
        intValue == 42
        explicitDefaultValue == 7
        thrown(ConfigurationError)
    }

    def "GetList"() {
        given:
        String containerId = "testContainer"
        RecursiveOverridableMapContainerForConfigurationValues container =
                new RecursiveOverridableMapContainerForConfigurationValues<>(
                        new Configuration(),
                        containerId)
        container.put("listKey", "value1,val:ue2,value3")


        when:
        List<String> listValue = container.getList("listKey")
        List<String> changedSeparater = container.getList("listKey", ":")
        List<String> implicitDefaultValue = container.getList("anotherNonExisting")

        then:
        listValue == ["value1", "val:ue2", "value3"]
        changedSeparater == ["value1,val", "ue2,value3"]
        implicitDefaultValue == []
    }

    def "GetString"() {
        given:
        String containerId = "testContainer"
        RecursiveOverridableMapContainerForConfigurationValues container =
                new RecursiveOverridableMapContainerForConfigurationValues<>(
                        new Configuration(),
                        containerId)
        container.put("stringKey", "stringValue")

        when:
        String stringValue = container.getString("stringKey")
        String explicitDefaultValue = container.getString("nonExistingKey", "defaultString")
        String implicitDefaultValue = container.getString("anotherNonExistingKey")

        then:
        stringValue == "stringValue"
        explicitDefaultValue == "defaultString"
        implicitDefaultValue == ""
    }

    @Ignore
    def "TemporarilyElevateValue"() {}

}
