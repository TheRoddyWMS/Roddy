package de.dkfz.roddy.config

import de.dkfz.roddy.Roddy
import de.dkfz.roddy.client.cliclient.CommandLineCall
import de.dkfz.roddy.client.cliclient.RoddyCLIClient
import de.dkfz.roddy.core.Analysis;
import de.dkfz.roddy.core.DataSet;
import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.core.ExecutionContextLevel
import de.dkfz.roddy.core.Project
import de.dkfz.roddy.core.ProjectFactory;
import de.dkfz.roddy.knowledge.examples.SimpleFileStageSettings;
import de.dkfz.roddy.knowledge.examples.TextFile
import de.dkfz.roddy.tools.RoddyIOHelperMethods;

import java.io.*;

import de.dkfz.roddy.knowledge.files.Tuple2;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

@groovy.transform.CompileStatic
public class FilenamePatternTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }
//
//import de.dkfz.roddy.core.DataSet;
//import de.dkfz.roddy.core.ExecutionContext;
//import de.dkfz.roddy.core.ExecutionContextLevel;
//import de.dkfz.roddy.knowledge.examples.SimpleFileStageSettings;
//import de.dkfz.roddy.knowledge.examples.TextFile;
//
//import java.io.*;
//
//import de.dkfz.roddy.knowledge.files.Tuple2;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//
//import java.lang.reflect.Method;
//
//public class FilenamePatternTest {
//
//    @Before
//    public void setUp() throws Exception {
//
//    }
//
//    @After
//    public void tearDown() throws Exception {
//
//    }
////
////    @Test
////    public void testAssembleID() throws Exception {
////        assert false;
////    }
////
////    @Test
////    public void testAssembleID1() throws Exception {
////        assert false;
////    }
////
////    @Test
////    public void testAssembleID2() throws Exception {
////        assert false;
////    }
////
////    @Test
////    public void testGetID() throws Exception {
////
////    }
////
////    @Test
////    public void testGetCls() throws Exception {
////
//////    }
////
////    @Test
////    public void testGetDerivedFromCls() throws Exception {
////
////    }
////
////    @Test
////    public void testGetCalledMethodsName() throws Exception {
////
////    }
////
////    @Test
////    public void testGetCalledMethodsClass() throws Exception {
////
////    }
////
////    @Test
////    public void testGetPattern() throws Exception {
////
////    }
////
////    @Test
////    public void testGetFileStage() throws Exception {
////
////    }
////
////    @Test
////    public void testDoesAcceptFileArrays() throws Exception {
////
////    }
////
////    @Test
////    public void testHasEnforcedArraySize() throws Exception {
////
////    }
////
////    @Test
////    public void testGetEnforcedArraySize() throws Exception {
////
////    }
////
////    @Test
////    public void testGetFilenamePatternDependency() throws Exception {
////
////    }
////
////    @Test
////    public void testHasSelectionTag() throws Exception {
////
////    }
////
////    @Test
////    public void testGetSelectionTag() throws Exception {
////
////    }
//
//    public static Tuple2 testCreateFile(ExecutionContext context) {
//        File srcFilePath = new File("/tmp/srcFilePath.txt");
//        TextFile srcFile = new TextFile(srcFilePath, context, null, null, new TestFileStageSettings());
//        TextFile textFileWithDefaultName = new TextFile(srcFile);
//
//        TextFile textFileWithSelectionPattern = new TextFile(srcFile);
//        textFileWithSelectionPattern.overrideFilenameUsingSelectionTag("selectionTag");
//
//        return new Tuple2(textFileWithDefaultName, textFileWithSelectionPattern);
//    }
//
//    @Test
//    public void testApply() throws Exception {
//        File tempDir = new File("/tmp");
//        AnalysisConfiguration analysisConfiguration = new AnalysisConfiguration(null, "", null, null, null, null, null);
//        analysisConfiguration.getConfigurationValues().add(new ConfigurationValue("cfgval", "abc"));
//        analysisConfiguration.getConfigurationValues().add(new ConfigurationValue("analysisMethodNameOnOutput", "genome"));
//        TestAnalysis analysis = new TestAnalysis(analysisConfiguration);
//        ExecutionContext context = new ExecutionContext("test", analysis, new DataSet(analysis, "testid", tempDir), ExecutionContextLevel.QUERY_STATUS, tempDir, tempDir, tempDir, System.nanoTime(), false);
//        Class srcClass = TextFile.class;
//        Class testClass = FilenamePatternTest.class;
//        Method testMethod = testClass.getMethod("testCreateFile", context.getClass());
//        String defaultFilename = "/tmp/${cfgval}_${analysisMethodNameOnOutput}_test_default.txt";
//        String taggedFilename = "/tmp/${cfgval}_test_tagged.txt";
//        String defaultFilenameFnl = "/tmp/abc_test_default.txt";
//        String taggedFilenameFnl = "/tmp/abc_test_tagged.txt";
//        FilenamePattern fp0 = new FilenamePattern(srcClass, testClass, testMethod, defaultFilename, "default");
//        FilenamePattern fp1 = new FilenamePattern(srcClass, testClass, testMethod, taggedFilename, "selectionTag");
//        analysisConfiguration.getFilenamePatterns().add(fp0);
//        analysisConfiguration.getFilenamePatterns().add(fp1);
//
//        Tuple2 tuple2 = testCreateFile(context);
//        assert ((TextFile)tuple2.value0).getAbsolutePath().equals(defaultFilenameFnl);
//        assert ((TextFile)tuple2.value1).getAbsolutePath().equals(taggedFilenameFnl);
//    }
//
//    @Test
//    public void testExtractCommand() throws Exception {
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
    /**
     * This test takes the test analysis and a test project and looks, if the files in the test project will all have valid paths.
     * In this case, the check will if a path is ! null and does not contain any ${ signs.
     */
    public void testApply() throws Exception {
        // Normally, the folder should be set by Roddy itself, but in this case, Roddy has no knowledge about itself yet!
        Roddy.main(["listdatasets", "TestProjectForUnitTests@test", "stds", "--useRoddyVersion=current", "--disallowexit", "--configurationDirectories="+RoddyIOHelperMethods.assembleLocalPath(Roddy.getApplicationDirectory(), "dist", "bin", "current", "testFiles").absolutePath] as String[]);
        File tempDir = new File("/tmp")

        Analysis analysis = RoddyCLIClient.checkAndLoadAnalysis(Roddy.getCommandLineCall());
        List<ExecutionContext> executionContexts = analysis.run(["stds"], ExecutionContextLevel.QUERY_STATUS);
        assert(executionContexts.size() > 0);
        executionContexts[0].allFilesInRun.each { file -> assert(file != null && !file.absolutePath.contains('${'))}
    }

    @Test
    public void testExtractCommand() throws Exception {

    }
}
