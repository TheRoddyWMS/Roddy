/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config

import de.dkfz.roddy.Roddy
import de.dkfz.roddy.config.validation.BashValidator
import de.dkfz.roddy.config.validation.DefaultValidator
import de.dkfz.roddy.config.validation.FileSystemValidator
import de.dkfz.roddy.core.Analysis
import de.dkfz.roddy.core.DataSet
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.RoddyConversionHelperMethods
import groovy.transform.CompileStatic

import java.util.regex.Matcher

import static de.dkfz.roddy.StringConstants.*
import static de.dkfz.roddy.config.ConfigurationConstants.*

/**
 * A configuration value
 */
@CompileStatic
class ConfigurationValue implements RecursiveOverridableMapContainer.Identifiable, ConfigurationIssue.IConfigurationIssueContainer {

    private static final LoggerWrapper logger = LoggerWrapper.getLogger(ConfigurationValue.class.simpleName)

    /**
     * API Level 3.4+
     */
    static Enumeration getDefaultCValueTypeEnumeration() {
        new Enumeration(CVALUE_TYPE, [
                new EnumerationValue(CVALUE_TYPE_FILENAME, FileSystemValidator.name),
                new EnumerationValue(CVALUE_TYPE_FILENAME_PATTERN, FileSystemValidator.name),
                new EnumerationValue(CVALUE_TYPE_PATH, FileSystemValidator.name),
                new EnumerationValue(CVALUE_TYPE_BASH_ARRAY, BashValidator.name),
                new EnumerationValue(CVALUE_TYPE_BOOLEAN, DefaultValidator.name),
                new EnumerationValue(CVALUE_TYPE_INTEGER, DefaultValidator.name),
                new EnumerationValue(CVALUE_TYPE_FLOAT, DefaultValidator.name),
                new EnumerationValue(CVALUE_TYPE_DOUBLE, DefaultValidator.name),
                new EnumerationValue(CVALUE_TYPE_STRING, DefaultValidator.name),
        ])
    }

    final String id

    final String value

    private final Configuration configuration

    private final String type

    /**
     * A description or comment for a configuration value.
     */
    private final String description

    private final List<String> tags = [] as LinkedList<String>

    /**
     * API Level 3.4+
     */
    final boolean valid

    /**
     * API Level 3.4+
     */
    final DefaultValidator validator = new DefaultValidator(null)

    ConfigurationValue(String id, String value) {
        this(null, id, value, null)
    }

    /**
     * API Level 3.4+
     */
    ConfigurationValue(Configuration config = null, String id, boolean value) {
        this(config, id, value.toString(), CVALUE_TYPE_BOOLEAN)
    }

    /**
     * API Level 3.4+
     */
    ConfigurationValue(Configuration config = null, String id, int value) {
        this(config, id, value.toString(), CVALUE_TYPE_INTEGER)
    }

    /**
     * API Level 3.4+
     */
    ConfigurationValue(Configuration config = null, String id, float value) {
        this(config, id, value.toString(), CVALUE_TYPE_FLOAT)
    }

    /**
     * API Level 3.4+
     */
    ConfigurationValue(Configuration config = null, String id, double value) {
        this(config, id, value.toString(), CVALUE_TYPE_DOUBLE)
    }

    ConfigurationValue(Configuration config, String id, String value) {
        this(config, id, value, null)
    }

    ConfigurationValue(String id, String value, String type) {
        this(null, id, value, type)
    }

    ConfigurationValue(Configuration config, String id, String value, String type) {
        this(config, id, value, type, "", null)
    }

    ConfigurationValue(Configuration config, String id, String value, String type, String description, List<String> tags) {
        this.id = id
        this.value = value != null ? replaceDeprecatedVariableIdentifiers(value) : null
        this.configuration = config
        this.type = !RoddyConversionHelperMethods.isNullOrEmpty(type) ? type : guessTypeOfValue(value)
        this.description = description
        if (tags != null)
            this.tags.addAll(tags)
        valid = validator.validate(this)
    }

    /**
     * Copy constructor for cvalue elevation
     * @param config
     * @param parent
     */
    private ConfigurationValue(Configuration config, ConfigurationValue parent) {
        this.id = parent.id
        this.value = parent.value
        this.configuration = config
        this.type = parent.type
        this.description = parent.description
        this.tags += parent.tags
        this.tags << "ELEVATED"
        this.validator = parent.validator // Do not revalidate, this is much too expensive for many values
        this.valid = parent.valid
    }


    ConfigurationValue elevate(Configuration newParent) {
        return new ConfigurationValue(newParent, this)
    }

    @Deprecated
    static String replaceDeprecatedVariableIdentifiers(String cval) {
        ([
                '$USERNAME' : '${USERNAME}',
                '$USERGROUP': '${USERGROUP}',
                '$USERHOME' : '${USERHOME}',
                '$PWD'      : "\${$ExecutionService.RODDY_CVALUE_DIRECTORY_EXECUTION}"
        ] as Map<String, String>).each { String k, String v ->
            cval = cval.replace(k, v)
        }
        return cval
    }

    @Override
    boolean equals(Object o) {
        // Auto-code from Idea.
        if (this == o) return true
        if (o == null || getClass() != o.getClass()) return false

        ConfigurationValue that = (ConfigurationValue) o

        if (id != null ? id != that.id : that.id != null) return false
        if (this.value != null ? this.value != that.value : that.value != null) return false
        return type != null ? type == that.type : that.type == null
    }

    @Override
    boolean hasWarnings() {
        validator.hasWarnings()
    }

    @Override
    void addWarning(ConfigurationIssue warning) { }

    @Override
    List<ConfigurationIssue> getWarnings() {
        validator.warnings
    }

    @Override
    boolean hasErrors() {
        validator.hasErrors()
    }

    @Override
    void addError(ConfigurationIssue error) { }

    @Override
    List<ConfigurationIssue> getErrors() {
        validator.errors
    }

    Configuration getConfiguration() {
        configuration
    }

    /**
     * If the type of the value is set to null, we can try to auto detect the value.
     * Defaults to string and can detect integers, doubles and arrays.
     * <p>
     * Maybe, this should be put to a different location?
     *
     * @return
     */
    static String guessTypeOfValue(String value) {
        if (RoddyConversionHelperMethods.isInteger(value)) return CVALUE_TYPE_INTEGER
        if (RoddyConversionHelperMethods.isDouble(value)) return CVALUE_TYPE_DOUBLE
        if (RoddyConversionHelperMethods.isFloat(value)) return CVALUE_TYPE_FLOAT
        if (RoddyConversionHelperMethods.isDefinedArray(value)) return CVALUE_TYPE_BASH_ARRAY
        return CVALUE_TYPE_STRING
    }

    @Deprecated
    private String checkAndCorrectPath(String temp) {
        return temp
    }

    private ExecutionContext _toFileExecutionContext
    private File _toFileCache

    /**
     * Converts this configuration value to a path and fills in data set and analysis specific settings.
     *
     * If other values are imported by this value, the used configuration objects are in the following order
     * - The first configuration is the configuration in which the configuration value resides.
     *   As we are mostly working with elevated configuration values (context configurations),
     *   it is the uppermost configuration.
     * - The second configuration is the configuration for the analysis. If the value is evaluated,
     *   the analysis configuration might already have been used.
     * - The third configuration replaces identifiers for pid/dataset and is directly taken from the dataset
     *
     */
    File toFile(Analysis analysis, DataSet dataSet = null) throws ConfigurationError {
        String temp = ConfigurationValueHelper.evaluateValue(id, value ?: "", configuration)

        if (analysis) temp = ConfigurationValueHelper.evaluateValue(id, temp, analysis.configuration)
        if (dataSet) temp = ConfigurationValueHelper.evaluateValue(id, temp, dataSet.configuration)

        return new File(temp)
    }

    File toFile(ExecutionContext context) {
        if (context != _toFileExecutionContext) {
            _toFileExecutionContext = context
            _toFileCache = null
        }
        if (_toFileCache != null) {
            return _toFileCache
        }

        if (context == null) {
            _toFileCache = new File(value)
            return _toFileCache
        }
        try {
            String temp = toFile(context.analysis, context.dataSet).path
            if (value.startsWith("\${DIR_BUNDLED_FILES}") || value.startsWith("\${DIR_RODDY}")) {
                temp = Roddy.getApplicationDirectory().absolutePath +
                        FileSystemAccessProvider.instance.pathSeparator +
                        temp
            }
            _toFileCache = new File(temp)
            return _toFileCache
        } catch (Exception ex) {
            return null
        }
    }

    Boolean toBoolean() {
        String v = value != null ? value.toLowerCase() : "f"
        if (v.startsWith("y") || v.startsWith("j") || v.startsWith("t") || v == "1") {
            if (v != "true") {
                logger.warning("Boolean configuration value '" + id + "' must be 'true' or 'false'. Found: " + v)
            }
            if (v == "1") {
                logger.warning("Boolean configuration value '" + id + "' is '1'. Since Roddy 3.0.8 interpreted as 'true'.")
            }
            return true
        }
        if (v.startsWith("n") || v.startsWith("f") || v == "0") {
            if (v != "false") {
                logger.warning("Boolean configuration value '" + id + "' must be 'true' or 'false'. Found: " + v)
            }
            if (v == "0") {
                logger.warning("Boolean configuration value '" + id + "' is '0'. Since Roddy 3.0.8 interpreted as 'false'.")
            }
            return false
        }
        return false
    }

    int toInt() {
        Integer.parseInt(evaluatedValue)
    }

    float toFloat() {
        Float.parseFloat(evaluatedValue)
    }

    double toDouble() {
        Double.parseDouble(evaluatedValue)
    }

    long toLong() {
        Long.parseLong(evaluatedValue)
    }

    @Override
    String toString() {
        evaluatedValue
    }


    String getType() {
        return type
    }

    List<String> getListOfTags() {
        return tags
    }

    boolean hasTag(String tag) {
        return tags.contains(tag)
    }

    EnumerationValue getEnumerationValueType() {
        return getEnumerationValueType(null)
    }

    EnumerationValue getEnumerationValueType(EnumerationValue defaultType) {
        Enumeration enumeration
        try {
            enumeration = getConfiguration()?.getEnumerations()?.getValue(CVALUE_TYPE, null)
            if (!enumeration) {
                // Get default types...
                enumeration = getDefaultCValueTypeEnumeration()
            }
            String _ev = getType()
            if (_ev == null || _ev.trim() == "")
                _ev = CVALUE_TYPE_STRING
            EnumerationValue ev = enumeration.getValue(_ev)
            return ev
        } catch (ConfigurationError e) {
            return defaultType
        }
    }

    boolean isValid() {
        return valid
    }

    boolean isInvalid() {
        return !valid
    }

    private List<String> _bashArrayToStringList()
            throws ConfigurationError {
        Matcher matcher = value=~ /^\s*\((?<stripped>(.*))\)\s*$/
        if (!matcher.matches()) {
            throw new ConfigurationError("Bash array value does not start with '(' or end with ')'")
        }
        String stripped = matcher.group('stripped')

        String[] arrayElements = stripped.split(SPLIT_WHITESPACE)
        List <String> resultStrings = [] as LinkedList<String>
        for (int i = 0; i < arrayElements.length; i++) {

            // Detect if value is a range { .. }
            // TODO Enable array of this form {1..N}C = 1C 2C 3C 4C .. NC
            if (arrayElements[i].startsWith(BRACE_LEFT)
                    && arrayElements[i].contains(BRACE_RIGHT)
                    && arrayElements[i].contains(DOUBLESTOP)) {
                String[] rangeTemp = arrayElements[i].split(SPLIT_DOUBLESTOP)
                int start = Integer.parseInt(rangeTemp[0].replace(BRACE_LEFT, EMPTY).trim())
                int end = Integer.parseInt(rangeTemp[1].replace(BRACE_RIGHT, EMPTY).trim())
                for (int j = start; j <= end; j++) {
                    resultStrings.add(EMPTY + j)
                }
            } else if (arrayElements[i].length() > 0) {
                // Just append the non-empty value.
                resultStrings.add(arrayElements[i])
            }
        }

        return resultStrings
    }

    List<String> toStringList() {
        return toStringList(",", new String[0])
    }

    /** Parse the parameter into a string list.
     *
     * @param delimiters      If the parameter is declared as bashArray, then the delimiter is ignored.
     * @param ignoreStrings
     * @return
     */
    List<String> toStringList(String delimiters, String[] ignoreStrings) {
        if (CVALUE_TYPE_BASH_ARRAY == type) {
            return _bashArrayToStringList()
        } else {
            String[] temp = value.split(SBRACKET_LEFT + delimiters + SBRACKET_RIGHT)
            List<String> tempList = [] as LinkedList<String>
            List<String> ignorable = Arrays.asList(ignoreStrings)

            for (String _t : temp) {
                String trimmed = _t.trim()
                if (trimmed.length() == 0) continue
                if (ignorable.contains(trimmed))
                    continue
                tempList.add(trimmed)
            }
            return tempList
        }
    }

    @Override
    String getID() {
        id
    }

    /**
     * Returns the single, unformatted value string.
     *
     * @return
     */
    String getValue() {
        this.value
    }

    String getEvaluatedValue() {
        ConfigurationValueHelper.evaluateValue(id, value, configuration)
    }

    String getDescription() {
        description
    }

    /**
     * Returns if the contained value is either null or has an empty string.
     *
     * @return
     */
    boolean isNullOrEmpty() {
        value == null || value.length() == 0 || value.trim().length() == 0
    }
}
