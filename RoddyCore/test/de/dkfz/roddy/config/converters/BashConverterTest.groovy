/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.converters;

import de.dkfz.roddy.RunMode;
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationConstants
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.MockupExecutionContextBuilder
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.NoNoExecutionService
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import org.junit.BeforeClass;
import org.junit.Test

/**
 * Created by heinold on 30.06.16.
 */
@groovy.transform.CompileStatic
public class BashConverterTest {


    public static final String CVAL_TEST_OUTPUT_DIRECTORY = "testOutputDirectory"
    public static final String CVAL_OUTPUT_BASE_DIRECTORY = "outputBaseDirectory"
    public static final String CVAL_OUTPUT_ANALYSIS_BASE_DIRECTORY = "outputAnalysisBaseDirectory"
    public static final String CVAL_TEST_BASHARRAY = "testBashArray"
    public static final String CVAL_TEST_BASHARRAY_QUOTES = "testBashArrayQuotes"
    public static final String CVAL_TEST_INTEGER = "testInteger"
    public static final String CVAL_TEST_FLOAT = "testFloat"
    public static final String CVAL_TEST_DOUBLE = "testDouble"

    @BeforeClass
    public static final void setup() {
        ExecutionService.initializeService(NoNoExecutionService, RunMode.UI)
        FileSystemAccessProvider.initializeProvider(true)
    }

    private Configuration createTestConfiguration() {
        Configuration configuration = new Configuration(null);
        configuration.getConfigurationValues().addAll([
                new ConfigurationValue(configuration, CVAL_OUTPUT_BASE_DIRECTORY, "/tmp", "path"),
                new ConfigurationValue(configuration, CVAL_OUTPUT_ANALYSIS_BASE_DIRECTORY, '${outputBaseDirectory}/Dideldum', "path"),
                new ConfigurationValue(configuration, CVAL_TEST_OUTPUT_DIRECTORY, "testvalue", "path"),
                new ConfigurationValue(configuration, CVAL_TEST_BASHARRAY, "( a b c d )", "bashArray"),
                new ConfigurationValue(configuration, CVAL_TEST_BASHARRAY_QUOTES, "'( a b c d )'", "bashArray"),
                new ConfigurationValue(configuration, CVAL_TEST_INTEGER, "100", "integer"),
                new ConfigurationValue(configuration, CVAL_TEST_FLOAT, "1.0", "float"),
                new ConfigurationValue(configuration, CVAL_TEST_DOUBLE, "1.0", "double"),
        ] as List<ConfigurationValue>)
        return configuration;
    }

    // Too much of an integration test.
    //    @Test
    //    public void convert() throws Exception {
    //
    //    }

//    @Test
//    public void createNewDocumentStringBuilder() throws Exception {
//
//    }
//
//    @Test
//    public void appendConfigurationValues() throws Exception {
//
//    }
//
//    @Test
//    public void appendConfigurationValueBundles() throws Exception {
//
//    }
//
//    @Test
//    public void appendToolEntries() throws Exception {
//
//    }
//
    @Test
    public void appendDebugVariables() throws Exception {
        Configuration configuration = createTestConfiguration()

        assert  new BashConverter().
                    appendDebugVariables(configuration).
                    toString().
                    trim() == ["set -o pipefail",
                               "set -v",
                               "set -x"].join("\n")

        configuration.configurationValues.put(ConfigurationConstants.DEBUG_OPTIONS_USE_EXTENDED_EXECUTE_OUTPUT, "true", "boolean")
        assert  new BashConverter().
                    appendDebugVariables(configuration).
                    toString().
                    trim() == ["set -o pipefail",
                               "set -v",
                               "set -x",
                               "export PS4='+(\${BASH_SOURCE}:\${LINENO}): \${FUNCNAME[0]: +\$ { FUNCNAME[0] }():}'"].join("\n")

        configuration.configurationValues.put(ConfigurationConstants.DEBUG_OPTIONS_USE_EXECUTE_OUTPUT, "false", "boolean")
        configuration.configurationValues.put(ConfigurationConstants.DEBUG_OPTIONS_USE_EXTENDED_EXECUTE_OUTPUT, "false", "boolean")
        assert  new BashConverter().
                    appendDebugVariables(configuration).
                    toString().
                    trim() == ["set -o pipefail",
                               "set -v"].join("\n")

    }
//
//    @Test
//    public void appendPathVariables() throws Exception {
//
//    }
//
//    @Test
//    public void getConfigurationValuesSortedByDependencies() throws Exception {
//
//    }

    @Test
    public void convertConfigurationValueToShellScriptLine() throws Exception {
        def configuration = createTestConfiguration()
        Map<String, String> list = [
                (CVAL_TEST_OUTPUT_DIRECTORY)         : "declare -x    testOutputDirectory=testvalue",
                (CVAL_TEST_BASHARRAY)                : "declare -x    testBashArray=\"( a b c d )\"",
                (CVAL_TEST_BASHARRAY_QUOTES)         : "declare -x    testBashArrayQuotes='( a b c d )'",
                (CVAL_TEST_INTEGER)                  : "declare -x -i testInteger=100",
                (CVAL_TEST_FLOAT)                    : "declare -x    testFloat=1.0",
                (CVAL_TEST_DOUBLE)                   : "declare -x    testDouble=1.0",
                // These tests here fail, if you only start this test. Leave them at the end, so we can at least test the other tests.
                (CVAL_OUTPUT_BASE_DIRECTORY)         : "declare -x    ${CVAL_OUTPUT_BASE_DIRECTORY}=/tmp".toString(),
                (CVAL_OUTPUT_ANALYSIS_BASE_DIRECTORY): "declare -x    ${CVAL_OUTPUT_ANALYSIS_BASE_DIRECTORY}=/tmp/Dideldum".toString(),
        ]

        ExecutionContext context = MockupExecutionContextBuilder.createSimpleContext(BashConverterTest, configuration)

        list.each { String id, String expected ->
            def val = new BashConverter().convertConfigurationValue(configuration.getConfigurationValues()[id], context).toString();

            assert val.toString() == expected
        }
    }

}