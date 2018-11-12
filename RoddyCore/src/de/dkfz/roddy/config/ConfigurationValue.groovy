/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config

import de.dkfz.roddy.Constants
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.config.loader.ConfigurationLoadError
import de.dkfz.roddy.config.validation.ConfigurationValueValidator
import de.dkfz.roddy.config.validation.DefaultValidator
import de.dkfz.roddy.core.Analysis
import de.dkfz.roddy.core.DataSet
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.tools.CollectionHelperMethods
import de.dkfz.roddy.tools.RoddyConversionHelperMethods
import groovy.transform.CompileStatic

import java.io.File
import java.util.Arrays
import java.util.LinkedList
import java.util.List
import java.util.Map
import java.util.regex.Matcher
import java.util.regex.Pattern

import static de.dkfz.roddy.StringConstants.*
import static de.dkfz.roddy.config.ConfigurationConstants.CVALUE_TYPE_BASH_ARRAY

/**
 * A configuration value
 */
@CompileStatic
class ConfigurationValue implements RecursiveOverridableMapContainer.Identifiable, ConfigurationIssue.IConfigurationIssueContainer {

    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(ConfigurationValue.class.getSimpleName())

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
    final boolean invalid

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
        this(config, id, value.toString(), "boolean")
    }

    /**
     * API Level 3.4+
     */
    ConfigurationValue(Configuration config = null, String id, int value) {
        this(config, id, value.toString(), "integer")
    }

    /**
     * API Level 3.4+
     */
    ConfigurationValue(Configuration config = null, String id, float value) {
        this(config, id, value.toString(), "float")
    }

    /**
     * API Level 3.4+
     */
    ConfigurationValue(Configuration config = null, double value) {
        this(config, value.toString(), "double")
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
        this.value = value
        this.configuration = config
        this.type = !RoddyConversionHelperMethods.isNullOrEmpty(type) ? type : determineTypeOfValue(value)
        this.description = description
        if (tags != null)
            this.tags.addAll(tags)
        valid = validator.validate(this)
        invalid = !valid
    }

    @Override
    boolean equals(Object o) {
        // Auto-code from Idea.
        if (this == o) return true
        if (o == null || getClass() != o.getClass()) return false

        ConfigurationValue that = (ConfigurationValue) o

        if (id != null ? !id.equals(that.id) : that.id != null) return false
        if (value != null ? !value.equals(that.value) : that.value != null) return false
        return type != null ? type.equals(that.type) : that.type == null
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
    static String determineTypeOfValue(String value) {
        if (RoddyConversionHelperMethods.isInteger(value)) return "integer"
        if (RoddyConversionHelperMethods.isDouble(value)) return "double"
        if (RoddyConversionHelperMethods.isFloat(value)) return "float"
        if (RoddyConversionHelperMethods.isDefinedArray(value)) return "bashArray"
        return "string"
    }

    private String replaceString(String src, String pattern, String text) {
        if (src.contains(pattern)) {
            return src.replace(pattern, text)
        }
        return src
    }


    private String checkAndCorrectPathThrow(String temp) throws ConfigurationError {
        String curUserPath = (new File("")).getAbsolutePath()
        String applicationDirectory = Roddy.getApplicationDirectory().getAbsolutePath()
        //TODO Make something like a blacklist. This is not properly handled now. Initially this was done because Java sometimes puts something in front of the file paths.
        if (value.startsWith('${') || value.startsWith('$') || value.startsWith("~") || !value.startsWith("/")) {
            if (temp == applicationDirectory) {
                throw new ConfigurationError(id + " configuration value is empty", configuration)
            } else if (temp.startsWith(applicationDirectory)) {
                temp = temp.substring(applicationDirectory.length() + 1)
            } else if (temp.startsWith(curUserPath)) {
                temp = temp.substring(curUserPath.length() + 1)
            }
        }

        return temp
    }

    @Deprecated
    private String checkAndCorrectPath(String temp) {
        try {
            return checkAndCorrectPathThrow(temp)
        } catch (Exception e) {
            return null
        }
    }

    private ExecutionContext _toFileExecutionContext
    private File _toFileCache

    /**
     * Converts this configuration value to a path and fills in data set and analysis specific settings.
     *
     * @param analysis
     * @param dataSet
     * @return
     */
    File toFile(Analysis analysis, DataSet dataSet) throws ConfigurationError {
        File f = toFile(analysis)

        String temp = f.getAbsolutePath()
        temp = temp.contains("\${" + Constants.PID + "}") ? replaceString(temp, "\${" + Constants.PID + "}", dataSet.getId()) : temp
        temp = temp.contains("\${" + Constants.PID_CAP + "}") ? replaceString(temp, "\${" + Constants.PID_CAP + "}", dataSet.getId()) : temp
        temp = temp.contains("\${" + Constants.DATASET + "}") ? replaceString(temp, "\${" + Constants.DATASET + "}", dataSet.getId()) : temp
        temp = temp.contains("\${" + Constants.DATASET_CAP + "}") ? replaceString(temp, "\${" + Constants.DATASET_CAP + "}", dataSet.getId()) : temp

        return new File(temp)
    }

    File toFile(Analysis analysis) {
        if (analysis == null) return toFile()

        String temp = toFile().getAbsolutePath()
        temp = replaceConfigurationBasedValues(temp, analysis.getConfiguration())
        temp = replaceString(temp, "\${" + Constants.PROJECT_NAME + "}", analysis.getProject().getName())
        temp = checkAndCorrectPath(temp)

        String userID = analysis.getUsername()
        String groupID = analysis.getUsergroup()

        if (userID != null) {
            temp = replaceString(temp, "\$USERNAME", userID)
            temp = replaceString(temp, "\${USERNAME}", userID)
        }
        if (groupID != null) {
            temp = replaceString(temp, "\$USERGROUP", groupID)
            temp = replaceString(temp, "\${USERGROUP}", groupID)
        }

        String ud = FileSystemAccessProvider.getInstance().getUserDirectory().getAbsolutePath()
        temp = replaceString(temp, "\$USERHOME", ud)
        temp = replaceString(temp, "\${USERHOME}", ud)
        temp = checkAndCorrectPath(temp)

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
            _toFileCache = toFile()
            return _toFileCache
        }
        try {
            String temp = toFile(context.getAnalysis(), context.getDataSet()).getAbsolutePath()
            temp = checkAndCorrectPath(temp)
            if (value.startsWith("\${DIR_BUNDLED_FILES}") || value.startsWith("\${DIR_RODDY}"))
                temp = Roddy.getApplicationDirectory().getAbsolutePath() + FileSystemAccessProvider.getInstance().getPathSeparator() + temp

            if (temp.contains(ConfigurationConstants.CVALUE_PLACEHOLDER_EXECUTION_DIRECTORY)) {
                String pd = context.getExecutionDirectory().getAbsolutePath()
                temp = temp.replace(ConfigurationConstants.CVALUE_PLACEHOLDER_EXECUTION_DIRECTORY, pd)
            }
            _toFileCache = new File(temp)
            return _toFileCache
        } catch (Exception ex) {
            return null
        }
    }

    static String replaceConfigurationBasedValues(String temp, Configuration configuration) {
        String[] allValues = temp.split("/")//File.separator);
        for (int i = 0; i < allValues.length; i++) {
            if (!allValues[i].startsWith('$')) continue
            String cValName = allValues[i].substring(2, allValues[i].length() - 1)
            if (!configuration.getConfigurationValues().hasValue(cValName))
                continue
            ConfigurationValue cval = configuration.getConfigurationValues().get(cValName)
            if (cValName.toLowerCase().contains("directory") || cval.type.equals("path")) {
                temp = temp.replace(allValues[i], replaceConfigurationBasedValues(cval.value, configuration))
            } else {
                temp = temp.replace(allValues[i], cval.toString())
            }
        }
        return temp
    }

    File toFile(Map<String, String> replacements) {
        String temp = value

        for (String key : replacements.keySet()) {
            String val = replacements.get(key)
            temp = temp.replace(key, val)
        }
        return new File(temp)
    }

    File toFile() {
        return new File(value)
    }

    Boolean toBoolean() {
        String v = value != null ? value.toLowerCase() : "f"
        if (v.startsWith("y") || v.startsWith("j") || v.startsWith("t") || v.equals("1")) {
            if (!v.equals("true")) {
                logger.warning("Boolean configuration value '" + id + "' must be 'true' or 'false'. Found: " + v)
            }
            if (v.equals("1")) {
                logger.warning("Boolean configuration value '" + id + "' is '1'. Since Roddy 3.0.8 interpreted as 'true'.")
            }
            return true
        }
        if (v.startsWith("n") || v.startsWith("f") || v.equals("0")) {
            if (!v.equals("false")) {
                logger.warning("Boolean configuration value '" + id + "' must be 'true' or 'false'. Found: " + v)
            }
            if (v.equals("0")) {
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

    Pattern variableDetection = Pattern.compile('[$][{][a-zA-Z0-9_]*[}]')

    @Override
    String toString() {
        return toString(new LinkedList<>())
    }

    @Deprecated
    String toString(List<String> blackList) {
        return toEvaluatedValue(blackList)
    }

    String toEvaluatedValue(List<String> blackList) {
        String temp = value
        if (configuration != null) {
            List<String> valueIDs = getIDsForParentValues()
            if (CollectionHelperMethods.intersects(blackList, valueIDs)) {
                ConfigurationError exc = new ConfigurationError("Cyclic dependency found for cvalue '" + this.id + "' in file " + (configuration.preloadedConfiguration != null ? configuration.preloadedConfiguration.file : configuration.getID()), configuration, "Cyclic dependency", null)
                configuration.addLoadError(new ConfigurationLoadError(configuration, "cValues", exc.getMessage(), exc))
                throw exc
            }

            for (String vName : valueIDs) {
                if (configuration.getConfigurationValues().hasValue(vName)) {
                    List<String> subBlackList = new LinkedList<>(blackList)
                    subBlackList.add(vName)
                    temp = temp.replace("\${" + vName + '}', configuration.getConfigurationValues().get(vName).toString(subBlackList))
                }
            }
        }
        return temp
    }

    List<String> getIDsForParentValues() {
        List<String> parentValues = new LinkedList<>()

        Matcher m = variableDetection.matcher(value)
        // Findall is not available in standard java, so I use groovy here.
        for (String s : ConfigurationValueHelper.callFindAllForPatternMatcher(m)) {
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
            enumeration = getConfiguration()?.getEnumerations()?.getValue("cvalueType")
            if (!enumeration) {
                // Get default types...
                enumeration = Enumeration.getDefaultCValueTypeEnumeration()
            }
            String _ev = getType()
            if (_ev == null || _ev.trim().equals(""))
                _ev = "string"
            EnumerationValue ev = enumeration.getValue(_ev)
            return ev
        } catch (ConfigurationError e) {
            return defaultType
        }
    }

    @Deprecated
    boolean isInvalid() {
        return invalid
    }

    @Deprecated
    void setInvalid(boolean invalid) {
//        this.invalid = invalid
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
        if (CVALUE_TYPE_BASH_ARRAY.equals(type)) {
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
