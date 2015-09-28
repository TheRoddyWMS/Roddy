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

    @Test
    /**
     * This test takes the test analysis and a test project and looks, if the files in the test project will all have valid paths.
     * In this case, the check will if a path is ! null and does not contain any ${ signs.
     *
     * Not checked are auto filenames
     * Not checked are synthetic file classes.
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
