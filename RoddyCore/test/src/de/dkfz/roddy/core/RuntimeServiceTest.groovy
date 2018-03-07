/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core

import de.dkfz.roddy.RunMode
import de.dkfz.roddy.StringConstants
import de.dkfz.roddy.config.*
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.LocalExecutionService
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import groovy.transform.CompileStatic
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test

/**
 * Created by heinold on 09.11.15.
 */
@CompileStatic
class RuntimeServiceTest {

    final static File baseTestDirectory = new File("/tmp/roddyCentralDirectory")
    final static File outputBaseDirectory = new File(baseTestDirectory, "output/\${projectName}")
    static File inputBaseDirectory
    final static String outputAnalysisBaseDirectory = "\${outputBaseDirectory}/\${dataSet}"
    final static String inputAnalysisBaseDirectory = "\${inputBaseDirectory}/\${dataSet}"

    private static ExecutionContext mockedContext

    @BeforeClass
    static void initialize() {
        ExecutionService.initializeService(LocalExecutionService, RunMode.CLI)
        FileSystemAccessProvider.initializeProvider(true)
    }

    public <T extends Configuration> T setDirectories(T config) {
        inputBaseDirectory= new File(getClass().classLoader.getResource("testpids").path)
        config.configurationValues.add(new ConfigurationValue(ConfigurationConstants.CFG_INPUT_BASE_DIRECTORY, inputBaseDirectory.toString()))
        //config.configurationValues.add(new ConfigurationValue(ConfigurationConstants.CFG_INPUT_ANALYSIS_BASE_DIRECTORY, inputAnalysisBaseDirectory))
        config.configurationValues.add(new ConfigurationValue(ConfigurationConstants.CFG_OUTPUT_BASE_DIRECTORY, outputBaseDirectory.toString()))
        //config.configurationValues.add(new ConfigurationValue(ConfigurationConstants.CFG_OUTPUT_ANALYSIS_BASE_DIRECTORY, outputAnalysisBaseDirectory))
        config.configurationValues.add(new ConfigurationValue(RuntimeService.RODDY_CENTRAL_EXECUTION_DIRECTORY, baseTestDirectory.toString()))
        return config
    }

    @Before
    void setup() {
        final Configuration mockupConfig = new Configuration(new PreloadedConfiguration(null, Configuration.ConfigurationType.OTHER, "test", "", "", null, "", ResourceSetSize.l, null, null, null, null))
        mockedContext = MockupExecutionContextBuilder.createSimpleContext(RuntimeServiceTest.class, setDirectories(mockupConfig), new RuntimeService())
    }

    @Test
    void testGetCommonExecutionDirectory() throws Exception {
        assert mockedContext.runtimeService.getCommonExecutionDirectory(mockedContext).getAbsolutePath() == baseTestDirectory.toString()
    }

    @Test
    void testGetAnalysedMD5OverviewFile() throws Exception {
        assert mockedContext.runtimeService.getAnalysedMD5OverviewFile(mockedContext).getAbsolutePath() == new File(baseTestDirectory,"zippedAnalysesMD5.txt").toString()
    }

    @Test
    void testGetTestAnalysis() {
        def a = mockedContext.analysis
        assert a.getInputBaseDirectory().toString() ==
                inputBaseDirectory.toString().replace('${projectName}', mockedContext.project.name)
        assert a.getOutputBaseDirectory().toString() ==
                outputBaseDirectory.toString().replace('${projectName}', mockedContext.project.name)
    }

    @Test
    void testGetInputFolderForAnalysis() {
        def analysis = mockedContext.analysis
        assert analysis.runtimeService.getInputBaseDirectory(analysis).toString() ==
                inputBaseDirectory.toString().replace('${projectName}', mockedContext.project.name)
    }

    @Test
    void testGetOutputFolderForAnalysis() {
        def analysis = mockedContext.analysis
        assert analysis.runtimeService.getOutputBaseDirectory(analysis).toString() ==
                outputBaseDirectory.toString().replace('${projectName}', mockedContext.project.name)
    }

    @Test
    void testGetOutputFolderForProject() {
        assert mockedContext.runtimeService.getOutputBaseDirectory(mockedContext).toString() ==
                outputBaseDirectory.toString().replace('${projectName}', mockedContext.project.name)
    }

    @Test
    void testGetInputFolderForDataSetAndAnalysis() {
        assert mockedContext.runtimeService.getInputAnalysisBaseDirectory(mockedContext.dataSet, mockedContext.analysis).toString() ==
                inputAnalysisBaseDirectory.toString().
                        replace('${inputBaseDirectory}', inputBaseDirectory.toString()).
                        replace('${projectName}', mockedContext.project.name).
                        replace('${dataSet}', mockedContext.dataSet.id)
    }

    @Test
    void testGetOutputFolderForDataSetAndAnalysis() {
        assert mockedContext.runtimeService.getOutputAnalysisBaseDirectory(mockedContext.dataSet, mockedContext.analysis).toString() ==
                outputAnalysisBaseDirectory.toString().
                        replace('${outputBaseDirectory}', outputBaseDirectory.toString()).
                        replace('${projectName}', mockedContext.project.name).
                        replace('${dataSet}', mockedContext.dataSet.id)
    }

    @Test
    void getDefaultJobParameters() throws Exception {
        def context = mockedContext
        def result = new RuntimeService().getDefaultJobParameters(context, "aTool")
        assert result["pid"] == context.getDataSet().getId()
        assert result["PID"] == context.getDataSet().getId()
        assert result["ANALYSIS_DIR"]
    }

    @Test
    void testValidateDataSetLoadingString() {
        def rs = new RuntimeService()

        assert rs.validateCohortDataSetLoadingString("s[c:ADDD]")
        assert rs.validateCohortDataSetLoadingString("s[c:PID_0]")
        assert rs.validateCohortDataSetLoadingString("s[c:PID_0;PID_1;Pid-1]")
        assert rs.validateCohortDataSetLoadingString("s[c:PID_0;PID_1]")
        assert rs.validateCohortDataSetLoadingString("s[c:PID_0;PID_1;Pid-1|c:PID_0;Pid1]")
        assert rs.validateCohortDataSetLoadingString("s[c:PID_0;PID_1;Pid-1|c:PID_0;Pid1|c:PID-2;p?*]")

        assert !rs.validateCohortDataSetLoadingString("s[c:PID_0;]")
        assert !rs.validateCohortDataSetLoadingString("c:PID_0")
        assert !rs.validateCohortDataSetLoadingString("PID_0;PID_1")
    }

    @Test
    @Ignore("Analysis configuration needs to be non-null! Fix!")
    void loadDatasetsWithFilter() throws Exception {

        Analysis a = mockedContext.analysis

        // Try good cases
        assert a.getRuntimeService().loadDatasetsWithFilter(a, ["s[c:ADDD]"]).size() == 1
        assert a.getRuntimeService().loadDatasetsWithFilter(a, ["s[c:ADDD;BDDD]"]).size() == 1
        assert a.getRuntimeService().loadDatasetsWithFilter(a, ["s[c:CD*]"]).size() == 1
        assert a.getRuntimeService().loadDatasetsWithFilter(a, ["s[c:*DDD]"]).size() == 1

        // Two super cohorts
        assert a.getRuntimeService().loadDatasetsWithFilter(a, "s[c:ADDD;BDDD|c:ADDD|c:CD*],s[c:ADDD;BDDD|c:ADDD|c:CD*]".split(StringConstants.SPLIT_COMMA) as List<String>).size() == 2

        // Check one result.
        List<SuperCohortDataSet> result = a.getRuntimeService().loadDatasetsWithFilter(a, ["s[c:ADDD;BDDD;ACCD]"]) as List<SuperCohortDataSet>
        assert result[0].allCohorts.size() == 1 && result[0].allCohorts[0] instanceof CohortDataSet
        assert result[0].allCohorts[0].allCohortDatasets.size() == 3
        assert result[0].allCohorts[0].primarySet.id == "ADDD" // Make sure, sort order is right!
        assert result[0].allCohorts[0].allCohortDatasets[1].id == "ACCD" // Make sure, sort order is right!
        assert result[0].allCohorts[0].allCohortDatasets[2].id == "BDDD" // Make sure, sort order is right!
    }

    @Test
    @Ignore("Analysis configuration needs to be non-null! Fix!")
    void loadInproperDatasetsWithFilterAndFail() throws Exception {

        Analysis a = mockedContext.analysis

        // Try erroneous cases first.
        assert !a.getRuntimeService().loadDatasetsWithFilter(a, ["ADDD;ADDD"]) // Missing prefix
        assert !a.getRuntimeService().loadDatasetsWithFilter(a, ["s[c:ADDF]"]) // Missing directory
        assert !a.getRuntimeService().loadDatasetsWithFilter(a, ["c:ADDF"]) // Missing directory
        assert !a.getRuntimeService().loadDatasetsWithFilter(a, ["s[c:AE??;AE*]"]) // Missing directory

    }
}