/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */
package de.dkfz.roddy.config

import groovy.transform.CompileStatic

/**
 * API Level 3.4+
 *
 * Class that holds warnings for a configuration object.
 */
@CompileStatic
class ConfigurationIssue {

    static interface IConfigurationIssueContainer {
        boolean hasWarnings()

        void addWarning(ConfigurationIssue warning)

        List<ConfigurationIssue> getWarnings()

        boolean hasErrors()

        void addError(ConfigurationIssue error)

        List<ConfigurationIssue> getErrors()
    }

    static enum ConfigurationIssueLevel {
        CFG_INFO,
        CFG_WARNING,
        CFG_ERROR,
        CVALUE_INFO,
        CVALUE_WARNING,
        CVALUE_ERROR,
    }

    /**
     * Various templates for configuration related issues.
     */
    static enum ConfigurationIssueTemplate {
        detachedDollarCharacter(
                ConfigurationIssueLevel.CVALUE_WARNING,
                "Variables contain plain dollar sign(s). Roddy does not interpret them as variables and cannot guarantee correct ordering of assignments for such variables in the job parameter file.",
                "Variable '#REPLACE_0#' contains plain dollar sign(s) without braces. Roddy does not interpret them as variables and cannot guarantee correct ordering of assignments for such variables in the job parameter file."
        ),
        valueAndTypeMismatch(
                ConfigurationIssueLevel.CVALUE_ERROR,
                "Variables in your configuration mismatch regarding their type and value. See the extended logs for more information.",
                "The value of variable named '#REPLACE_0#' is not of its declared type '#REPLACE_1#'."
        ),
        inproperVariableExpression(
                ConfigurationIssueLevel.CVALUE_WARNING,
                "Variables in your configuration appear to misuse variable imports. Import variables like \${variable identifier}, nesting like '\${\${innerVar }}' is forbidden and it must not be empty.",
                "Variable '#REPLACE_0#' may use malformatted variable imports. Import variables like \${variable identifier}, nesting like '\${\${innerVar}}' is forbidden and it must not be empty."
        )

        final ConfigurationIssueLevel level
        final String collectiveMessage
        final String message
        final int noOfPlaceholders

        ConfigurationIssueTemplate(ConfigurationIssueLevel level, String collectiveMessage, String message) {
            this.collectiveMessage = collectiveMessage
            this.message = message
            this.level = level
            this.noOfPlaceholders = message.findAll('[#]REPLACE_([0-9]){0,}[#]').size()
        }

        ConfigurationIssue expand(String... messageContent) {
            // Programmatic errors, thus a RuntimeException s
            if (noOfPlaceholders != 0 && messageContent == null || noOfPlaceholders > messageContent.size()) {
                throw new RuntimeException("You need to supply a value for all #REPLACE_[n]# fields for the configuration issue template '${this.name()}'.")
            }

            if(noOfPlaceholders < messageContent.size()) {
                throw new RuntimeException("You supplied too many values for #REPLACE_[n]# fields for the configuration issue template '${this.name()}'.")
            }

            return new ConfigurationIssue(this, messageContent)
        }
    }

    final ConfigurationIssueTemplate id
    final String message

    ConfigurationIssue(ConfigurationIssueTemplate template, String... messageContent) {
        this.id = template
        String _message = id.message
        for (int i = 0; i < messageContent.length; i++) {
            _message = _message.replace("#REPLACE_${i}#", messageContent[i])
        }
        this.message = _message
    }

    ConfigurationIssueLevel getLevel() {
        return id.level
    }

    String getCollectiveMessage() {
        return id.collectiveMessage
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        ConfigurationIssue that = (ConfigurationIssue) o

        if (id != that.id) return false
        if (message != that.message) return false

        return true
    }

    int hashCode() {
        int result
        result = (id != null ? id.hashCode() : 0)
        result = 31 * result + (message != null ? message.hashCode() : 0)
        return result
    }

    @Override
    String toString() {
        return message
    }

}
