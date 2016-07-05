package de.dkfz.roddy.config.converters;

import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.core.Analysis
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextLevel
import de.dkfz.roddy.core.MockupExecutionContextBuilder
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

    @BeforeClass
    public static final void setup() {
        FileSystemAccessProvider.initializeProvider(true);
    }

    private Configuration createTestConfiguration() {
        Configuration configuration = new Configuration(null);
        configuration.getConfigurationValues().addAll([
                new ConfigurationValue(configuration, CVAL_OUTPUT_BASE_DIRECTORY, "/tmp", "path"),
                new ConfigurationValue(configuration, CVAL_OUTPUT_ANALYSIS_BASE_DIRECTORY, '${outputBaseDirectory}/Dideldum', "path"),
                new ConfigurationValue(configuration, CVAL_TEST_OUTPUT_DIRECTORY, "testvalue", "path"),
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
//    @Test
//    public void appendDebugVariables() throws Exception {
//
//    }
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
        Map<String, String> list = [:]
        list[CVAL_OUTPUT_BASE_DIRECTORY] = "${CVAL_OUTPUT_BASE_DIRECTORY}=/tmp".toString()
        list[CVAL_OUTPUT_ANALYSIS_BASE_DIRECTORY] = "${CVAL_OUTPUT_ANALYSIS_BASE_DIRECTORY}=/tmp/Dideldum".toString();
        list[CVAL_TEST_OUTPUT_DIRECTORY] = "testOutputDirectory=testvalue".toString();

        ExecutionContext context = MockupExecutionContextBuilder.createSimpleContext(BashConverterTest, configuration)

        list.each { String id, String expected ->
            def val = new BashConverter().convertConfigurationValueToShellScriptLine(configuration.getConfigurationValues()[id], context).toString();

            assert val == expected
        }
    }

}