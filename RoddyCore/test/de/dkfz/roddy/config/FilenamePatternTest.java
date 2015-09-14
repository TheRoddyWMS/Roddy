package de.dkfz.roddy.config;

import de.dkfz.roddy.core.DataSet;
import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.core.ExecutionContextLevel;
import de.dkfz.roddy.knowledge.examples.SimpleFileStageSettings;
import de.dkfz.roddy.knowledge.examples.TextFile;

import java.io.*;

import de.dkfz.roddy.knowledge.files.Tuple2;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

public class FilenamePatternTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }
//
//    @Test
//    public void testAssembleID() throws Exception {
//        assert false;
//    }
//
//    @Test
//    public void testAssembleID1() throws Exception {
//        assert false;
//    }
//
//    @Test
//    public void testAssembleID2() throws Exception {
//        assert false;
//    }
//
//    @Test
//    public void testGetID() throws Exception {
//
//    }
//
//    @Test
//    public void testGetCls() throws Exception {
//
////    }
//
//    @Test
//    public void testGetDerivedFromCls() throws Exception {
//
//    }
//
//    @Test
//    public void testGetCalledMethodsName() throws Exception {
//
//    }
//
//    @Test
//    public void testGetCalledMethodsClass() throws Exception {
//
//    }
//
//    @Test
//    public void testGetPattern() throws Exception {
//
//    }
//
//    @Test
//    public void testGetFileStage() throws Exception {
//
//    }
//
//    @Test
//    public void testDoesAcceptFileArrays() throws Exception {
//
//    }
//
//    @Test
//    public void testHasEnforcedArraySize() throws Exception {
//
//    }
//
//    @Test
//    public void testGetEnforcedArraySize() throws Exception {
//
//    }
//
//    @Test
//    public void testGetFilenamePatternDependency() throws Exception {
//
//    }
//
//    @Test
//    public void testHasSelectionTag() throws Exception {
//
//    }
//
//    @Test
//    public void testGetSelectionTag() throws Exception {
//
//    }

    public static Tuple2 testCreateFile(ExecutionContext context) {
        File srcFilePath = new File("/tmp/srcFilePath.txt");
        TextFile srcFile = new TextFile(srcFilePath, context, null, null, new TestFileStageSettings());
        TextFile textFileWithDefaultName = new TextFile(srcFile);

        TextFile textFileWithSelectionPattern = new TextFile(srcFile);
        textFileWithSelectionPattern.overrideFilenameUsingSelectionTag("selectionTag");

        return new Tuple2(textFileWithDefaultName, textFileWithSelectionPattern);
    }

    @Test
    public void testApply() throws Exception {
        File tempDir = new File("/tmp");
        AnalysisConfiguration analysisConfiguration = new AnalysisConfiguration(null, "", null, null, null, null, null);
        analysisConfiguration.getConfigurationValues().add(new ConfigurationValue("cfgval", "abc"));
        analysisConfiguration.getConfigurationValues().add(new ConfigurationValue("analysisMethodNameOnOutput", "genome"));
        TestAnalysis analysis = new TestAnalysis(analysisConfiguration);
        ExecutionContext context = new ExecutionContext("test", analysis, new DataSet(analysis, "testid", tempDir), ExecutionContextLevel.QUERY_STATUS, tempDir, tempDir, tempDir, System.nanoTime(), false);
        Class srcClass = TextFile.class;
        Class testClass = FilenamePatternTest.class;
        Method testMethod = testClass.getMethod("testCreateFile", context.getClass());
        String defaultFilename = "/tmp/${cfgval}_${analysisMethodNameOnOutput}_test_default.txt";
        String taggedFilename = "/tmp/${cfgval}_test_tagged.txt";
        String defaultFilenameFnl = "/tmp/abc_test_default.txt";
        String taggedFilenameFnl = "/tmp/abc_test_tagged.txt";
        FilenamePattern fp0 = new FilenamePattern(srcClass, testClass, testMethod, defaultFilename, "default");
        FilenamePattern fp1 = new FilenamePattern(srcClass, testClass, testMethod, taggedFilename, "selectionTag");
        analysisConfiguration.getFilenamePatterns().add(fp0);
        analysisConfiguration.getFilenamePatterns().add(fp1);

        Tuple2 tuple2 = testCreateFile(context);
        assert ((TextFile)tuple2.value0).getAbsolutePath().equals(defaultFilenameFnl);
        assert ((TextFile)tuple2.value1).getAbsolutePath().equals(taggedFilenameFnl);
    }

    @Test
    public void testExtractCommand() throws Exception {

    }
}