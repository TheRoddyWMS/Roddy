/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config

import de.dkfz.roddy.Roddy
import de.dkfz.roddy.config.loader.ConfigurationLoadError
import de.dkfz.roddy.config.validation.BashValidator
import de.dkfz.roddy.config.validation.DefaultValidator
import de.dkfz.roddy.config.validation.FileSystemValidator
import de.dkfz.roddy.core.Analysis
import de.dkfz.roddy.core.DataSet
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.tools.RoddyConversionHelperMethods
import groovy.transform.CompileStatic

import java.util.regex.Matcher
import java.util.regex.Pattern

import static de.dkfz.roddy.StringConstants.*
import static de.dkfz.roddy.config.ConfigurationConstants.*

/**
 * A configuration value
 */
@CompileStatic
class ConfigurationValue implements RecursiveOverridableMapContainer.Identifiable, ConfigurationIssue.IConfigurationIssueContainer {

    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(ConfigurationValue.class.getSimpleName())

    /**
     * API Level 3.4+
     */
    static Enumeration getDefaultCValueTypeEnumeration() {
        Enumeration _def = new Enumeration(CVALUE_TYPE, [
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
        return _def
    }

    public final String id

    public final String value

    private final Configuration configuration

    private final String type

    /**
     * A description or comment for a configuration value.
     */
    private final String description

    private final List<String> tags = new LinkedList<>()

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
        if (value != null ? value != that.value : that.value != null) return false
        return type != null ? type == that.type : that.type == null
    }

    @Override
    boolean hasWarnings() {
        validator.hasWarnings()
    }

    @Override
    void addWarning(ConfigurationIssue warning) {}

    @Override
    List<ConfigurationIssue> getWarnings() {
        validator.getWarnings()
    }

    @Override
    boolean hasErrors() {
        validator.hasErrors()
    }

    @Override
    void addError(ConfigurationIssue error) {}

    @Override
    List<ConfigurationIssue> getErrors() {
        validator.getErrors()
    }

    Configuration getConfiguration() {
        return configuration
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
        String temp = toEvaluatedValue([], id, value ?: "", configuration)

        if (analysis) temp = toEvaluatedValue([], id, temp, analysis.configuration)
        if (dataSet) temp = toEvaluatedValue([], id, temp, dataSet.configuration)

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
            String temp = toFile(context.getAnalysis(), context.getDataSet()).getPath()
            if (value.startsWith("\${DIR_BUNDLED_FILES}") || value.startsWith("\${DIR_RODDY}"))
                temp = Roddy.getApplicationDirectory().getAbsolutePath() + FileSystemAccessProvider.getInstance().getPathSeparator() + temp

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
        return Integer.parseInt(value)
    }

    float toFloat() {
        return Float.parseFloat(value)
    }

    double toDouble() {
        return Double.parseDouble(value)
    }

    long toLong() {
        return Long.parseLong(value)
    }

    static final Pattern variableDetection = Pattern.compile('[$][{][a-zA-Z0-9_]*[}]')

    @Override
    String toString() {
        return toEvaluatedValue([], id, value, configuration)
    }

    static String toEvaluatedValue(List<String> blackList, String valueID, String initialValue, Configuration configuration) {

        String temp = initialValue
        if (configuration != null) {
            List<String> valueIDs = getIDsForParentValues(temp)
            if (blackList.intersect(valueIDs as Iterable<String>)) {
                def badFile = configuration.preloadedConfiguration != null ? configuration.preloadedConfiguration.file : configuration.getID()
                ConfigurationError exc = new ConfigurationError("Cyclic dependency found for cvalue '${valueID}' in file '${badFile}'", configuration, "Cyclic dependency", null)
                configuration.addLoadError(new ConfigurationLoadError(configuration, "cValues", exc.getMessage(), exc))
                throw exc
            }
            def configurationValues = configuration.getConfigurationValues()
            for (String vName : valueIDs) {
                if (configurationValues.hasValue(vName)) {
                    List<String> subBlackList = blackList + [vName]
                    ConfigurationValue subValue = configurationValues[vName] as ConfigurationValue
                    temp = temp.replace("\${$vName}", toEvaluatedValue(subBlackList, subValue.id, subValue.value, subValue.configuration))
                }
            }
        }
        return temp
    }

    static List<String> getIDsForParentValues(String value) {
        List<String> parentValues = new LinkedList<>()

        Matcher m = variableDetection.matcher(value)
        // Findall is not available in standard java, so I use groovy here.
        for (String s : m.findAll()) {
            String vName = s.replaceAll("[\${}]", "")
            parentValues.add(vName)
        }

        return parentValues
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

    private List<String> _bashArrayToStringList() {
        String vTemp = value.substring(1, value.length() - 2).trim() //Split away () and leading or trailing white characters.
        String[] temp = vTemp.split(SPLIT_WHITESPACE) //Split by white character

        //Ignore leading and trailing brackets
        List<String> resultStrings = new LinkedList<>()

        for (int i = 0; i < temp.length; i++) {

            //Detect if value is a range { .. }
            //TODO Enable array of this form {1..N}C = 1C 2C 3C 4C .. NC
            if (temp[i].startsWith(BRACE_LEFT) && temp[i].contains(BRACE_RIGHT) && temp[i].contains(DOUBLESTOP)) {
                String[] rangeTemp = temp[i].split(SPLIT_DOUBLESTOP)
                int start = Integer.parseInt(rangeTemp[0].replace(BRACE_LEFT, EMPTY).trim())
                int end = Integer.parseInt(rangeTemp[1].replace(BRACE_RIGHT, EMPTY).trim())
                for (int j = start; j <= end; j++) {
                    resultStrings.add(EMPTY + j)
                }
            } else {
                //Just append the value.
                resultStrings.add(temp[i])
            }
        }

        return resultStrings
    }

    List<String> toStringList() {
        return toStringList(",", new String[0])
    }

    List<String> toStringList(String delimiter, String[] ignoreStrings) {
        if (CVALUE_TYPE_BASH_ARRAY == type) {
            return _bashArrayToStringList()
        }

        String[] temp = value.split(SBRACKET_LEFT + delimiter + SBRACKET_RIGHT)
        List<String> tempList = new LinkedList<String>()
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

    @Override
    String getID() {
        return id
    }

    /**
     * Returns the single, unformatted value string.
     *
     * @return
     */
    String getValue() {
        return value
    }

    String getDescription() {
        return description
    }

    /**
     * Returns if the contained value is either null or has an empty string.
     *
     * @return
     */
    boolean isNullOrEmpty() {
        return value == null || value.length() == 0 || value.trim().length() == 0
    }
}
