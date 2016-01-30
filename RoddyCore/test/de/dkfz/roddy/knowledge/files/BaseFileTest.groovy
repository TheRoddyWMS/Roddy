package de.dkfz.roddy.knowledge.files

import de.dkfz.roddy.config.ToolEntry
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextLevel
import de.dkfz.roddy.execution.jobs.JobResult
import de.dkfz.roddy.plugins.LibrariesFactory
import org.junit.BeforeClass
import org.junit.Test

/**
 * Created by heinold on 20.01.16.
 */
@groovy.transform.CompileStatic
public class BaseFileTest {
    private static Class<BaseFile> syntheticTestFileClass;

    public static ExecutionContext mockedContext;

    public static final File mockedTestFilePath = new File("/tmp/RoddyTests/testfile")

    @BeforeClass
    public static void setupBaseFileTests() {
        syntheticTestFileClass = LibrariesFactory.generateSyntheticFileClassWithParentClass("TestFileClass", "BaseFile");
        mockedContext = new ExecutionContext("default", null, null, ExecutionContextLevel.QUERY_STATUS, null, null, null);
    }

    @Test
    public void testCreationOfBaseFileWithSourceHelper() {
        BaseFile.ConstructionHelperForSourceFiles helperObject = new BaseFile.ConstructionHelperForSourceFiles(mockedTestFilePath, mockedContext, null, null);
        BaseFile instance = syntheticTestFileClass.newInstance(helperObject);
        assert instance;
        assert instance.executionContext == mockedContext;
        assert instance.path == mockedTestFilePath;
    }

    @Test
    public void testConstructForCreationOfBaseFileWithSourceHelper() {
        def obj = BaseFile.constructSourceFile(syntheticTestFileClass, mockedTestFilePath, mockedContext, null, null);
        assert obj instanceof BaseFile;
    }

    @Test
    public void testConstructForGenericCreationWithParentFile() {

        BaseFile parentObject = new GenericFile(new BaseFile.ConstructionHelperForSourceFiles(mockedTestFilePath, mockedContext, null, null));
        ToolEntry toolEntry = null;
        String methodID = null;
        String slotID = null;
        String selectionTag = null;
        FileStageSettings fileStageSettings = null;
        JobResult jobResult = null;
        BaseFile instance  = BaseFile.constructGeneric(syntheticTestFileClass, parentObject, null, toolEntry, methodID, slotID, selectionTag, fileStageSettings, jobResult);
        assert instance && instance.class == syntheticTestFileClass;
        assert instance.executionContext == mockedContext;
    }

    @Test
    public void testConstructForGenericCreationWithExecutionContext() {
        def obj = BaseFile.constructGeneric(syntheticTestFileClass, mockedContext, null, null, null, null, null, null);
        assert obj instanceof BaseFile;
    }

    @Test
    public void testConstructForManualCreationWithParentFile() {
        BaseFile parentObject = new GenericFile(new BaseFile.ConstructionHelperForSourceFiles(mockedTestFilePath, mockedContext, null, null));
        ToolEntry toolEntry = null;
        String methodID = null;
        String slotID = null;
        String selectionTag = null;
        FileStageSettings fileStageSettings = null;
        JobResult jobResult = null;
        BaseFile instance = BaseFile.constructManual(syntheticTestFileClass, parentObject, null, toolEntry, methodID, slotID, selectionTag, fileStageSettings, jobResult);
        assert instance && instance.class == syntheticTestFileClass;
        assert instance.executionContext == mockedContext;
    }

    @Test
    public void testConstructForManualCreationWithExecutionContext() {
        def obj = BaseFile.constructManual(syntheticTestFileClass, mockedContext, null, null, null, null, null, null);
        assert obj instanceof BaseFile;
    }
}