/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.files

import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationFactory
import de.dkfz.roddy.config.DerivedFromFilenamePattern
import de.dkfz.roddy.config.FileStageFilenamePattern
import de.dkfz.roddy.config.FilenamePattern
import de.dkfz.roddy.config.FilenamePatternDependency
import de.dkfz.roddy.config.InformationalConfigurationContent
import de.dkfz.roddy.config.OnMethodFilenamePattern
import de.dkfz.roddy.config.OnScriptParameterFilenamePattern
import de.dkfz.roddy.config.OnToolFilenamePattern
import de.dkfz.roddy.config.ResourceSetSize
import de.dkfz.roddy.config.ToolEntry
import de.dkfz.roddy.config.ToolFileParameter
import de.dkfz.roddy.config.ToolFileParameterCheckCondition
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.MockupExecutionContextBuilder
import de.dkfz.roddy.execution.jobs.JobResult
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.plugins.LibrariesFactoryTest
import groovy.transform.TypeCheckingMode
import groovy.util.slurpersupport.NodeChild
import org.junit.Before
import org.junit.Test

import java.lang.reflect.Method

/**
 * Created by heinold on 20.01.16.
 */
@groovy.transform.CompileStatic
public class BaseFileTest {
    private static Class<BaseFile> syntheticTestFileClass;

    public static ExecutionContext mockedContext;

    public static final File mockedTestFilePath = new File("/tmp/RoddyTests/testfile")


    private static final String STR_VALID_DERIVEDFROM_PATTERN = "<filename class='FileWithFileStage' derivedFrom='FileWithFileStage' pattern='/tmp/onderivedFile'/>"
    private static final String STR_VALID_ONMETHOD_PATTERN_WITH_CLASSNAME = "<filename class='FileWithFileStage' onMethod='getFilename' pattern='/tmp/onMethodwithClassName'/>"
    private static final String STR_VALID_ONTOOL_PATTERN = "<filename class='FileWithFileStage' onTool='testScript' pattern='/tmp/onTool'/>"
    private static final String STR_VALID_FILESTAGE_PATTERN = "<filename class='FileWithFileStage' fileStage=\"GENERIC\" pattern='/tmp/FileWithFileStage'/>"
    private static final String STR_VALID_ONSCRIPTPARAMETER_WITH_TOOL_AND_PARAMNAME =  "<filename class='FileWithFileStage' onScriptParameter='testScript:BAM_INDEX_FILE' pattern='/tmp/onScript' />"

    private NodeChild getParsedFilenamePattern(String filenamePattern) { return parseXML("<filenames filestagesbase='de.dkfz.roddy.knowledge.files.FileStage'>${filenamePattern}</filenames>"); }

    private static NodeChild parseXML(String xml) {
        return (NodeChild) new XmlSlurper().parseText(xml);
    }

    @Before
    public void setupBaseFileTests() {
        //Setup plugins and default configuration folders
        LibrariesFactory.initializeFactory(true);
        LibrariesFactory.getInstance().loadLibraries(LibrariesFactory.buildupPluginQueue(LibrariesFactoryTest.callLoadMapOfAvailablePlugins(), "DefaultPlugin").values() as List);
        ConfigurationFactory.initialize(LibrariesFactory.getInstance().getLoadedPlugins().collect { it -> it.getConfigurationDirectory() })

        final Configuration mockupConfig = new Configuration(new InformationalConfigurationContent(null, Configuration.ConfigurationType.OTHER, "default", "", "", null, "", ResourceSetSize.l, null, null, null, null), ConfigurationFactory.getInstance().getConfiguration("default")) {
            @Override
            File getSourceToolPath(String tool) {
                if (tool == "wrapinScript")
                    return super.getSourceToolPath(tool);
                return new File("/tmp/RoddyTests/RoddyTestScript_ExecutionServiceTest.sh")
            }
        };

        syntheticTestFileClass = LibrariesFactory.getInstance().loadRealOrSyntheticClass("FileWithFileStage", BaseFile.class as Class<FileObject>);

        ToolEntry toolEntry = new ToolEntry("RoddyTests", "RoddyTests", "RoddyTestScript_ExecutionServiceTest.sh");
        toolEntry.getOutputParameters(mockupConfig).add(new ToolFileParameter(syntheticTestFileClass, null, "TEST", new ToolFileParameterCheckCondition(true)))

        mockupConfig.getTools().add(toolEntry);
        mockedContext = MockupExecutionContextBuilder.createSimpleContext(BaseFileTest, mockupConfig);

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


    @Test
    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    public void testFindFilenameFromOnMethodPatterns () {
        OnMethodFilenamePattern pattern = ConfigurationFactory.readOnMethodFilenamePattern(null, getParsedFilenamePattern(STR_VALID_ONMETHOD_PATTERN_WITH_CLASSNAME).filename.getAt(0)) as OnMethodFilenamePattern;

        mockedContext.getConfiguration().getFilenamePatterns().add(pattern);

        BaseFile obj = BaseFile.constructManual(syntheticTestFileClass, mockedContext, null, null, null, null, null, null);
        assert obj instanceof BaseFile;

        Method loadAvailableFilenamePatternsMethod = BaseFile.class.getDeclaredMethod("loadAVailableFilenamePatterns",BaseFile,ExecutionContext)
        loadAvailableFilenamePatternsMethod.setAccessible(true);
        LinkedHashMap<FilenamePatternDependency, LinkedList<FilenamePattern>> availableFilenamePatterns = loadAvailableFilenamePatternsMethod.invoke(null, obj, mockedContext) as LinkedHashMap<FilenamePatternDependency, LinkedList<FilenamePattern>>;
        assert availableFilenamePatterns[FilenamePatternDependency.onMethod].size() == 1
/*
        Method findFilenameFromOnMethodPatternsMethod = BaseFile.getDeclaredMethod("findFilenameFromOnMethodPatterns", BaseFile,LinkedList, String);
        findFilenameFromOnMethodPatternsMethod.setAccessible(true);
        Tuple2<File, FilenamePattern> filenamePatterns = findFilenameFromOnMethodPatternsMethod.invoke(null, obj,availableFilenamePatterns[FilenamePatternDependency.onMethod],"default") as Tuple2<File, FilenamePattern>
        assert filenamePatterns != null
*/
    }

    @Test
    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    public void testFindFilenameFromOnToolIDPatterns(){
        OnToolFilenamePattern pattern = ConfigurationFactory.readOnToolFilenamePattern(null, getParsedFilenamePattern(STR_VALID_ONTOOL_PATTERN).filename.getAt(0)) as OnToolFilenamePattern;
        mockedContext.getConfiguration().getFilenamePatterns().add(pattern);


        def obj = BaseFile.constructSourceFile(syntheticTestFileClass, mockedTestFilePath, mockedContext, null, null);
        assert obj instanceof BaseFile;

        def toolent = new ToolEntry("testScript", "RoddyTests", "RoddyTestScript_ExecutionServiceTest.sh")
        toolent.getOutputParameters(mockedContext.getConfiguration()).add(new ToolFileParameter(syntheticTestFileClass, null, "BAM_INDEX_FILE", true))
        obj.getExecutionContext().setCurrentExecutedTool( toolent)


        Method loadAvailableFilenamePatternsMethod = BaseFile.class.getDeclaredMethod("loadAVailableFilenamePatterns",BaseFile,ExecutionContext)
        loadAvailableFilenamePatternsMethod.setAccessible(true);
        LinkedHashMap<FilenamePatternDependency, LinkedList<FilenamePattern>> availableFilenamePatterns = loadAvailableFilenamePatternsMethod.invoke(null, obj, mockedContext) as LinkedHashMap<FilenamePatternDependency, LinkedList<FilenamePattern>>;
        assert availableFilenamePatterns[FilenamePatternDependency.onTool].size() == 1

        Method findFilenameFromOnToolIDPatternsMethod = BaseFile.getDeclaredMethod("findFilenameFromOnToolIDPatterns", BaseFile,LinkedList, String);
        findFilenameFromOnToolIDPatternsMethod.setAccessible(true);
        def filenamePatterns = findFilenameFromOnToolIDPatternsMethod.invoke(null, obj,availableFilenamePatterns[FilenamePatternDependency.onTool],"default")
        assert filenamePatterns != null

    }

    @Test
    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    public void testFindFilenameFromOnScriptParameterPatterns(){
        OnScriptParameterFilenamePattern pattern = ConfigurationFactory.readOnScriptParameterFilenamePattern("de.dkfz.roddy.knowledge.files.FileStage", getParsedFilenamePattern(STR_VALID_ONSCRIPTPARAMETER_WITH_TOOL_AND_PARAMNAME).filename.getAt(0)) as OnScriptParameterFilenamePattern;
        mockedContext.getConfiguration().getFilenamePatterns().add(pattern);

        BaseFile obj = BaseFile.constructManual(syntheticTestFileClass, mockedContext, null, null, null, null, null, null);
        assert obj instanceof BaseFile;
        def toolEnt = new ToolEntry("testScript", "RoddyTests", "RoddyTestScript_ExecutionServiceTest.sh")
        toolEnt.getOutputParameters(mockedContext.getConfiguration()).add(new ToolFileParameter(syntheticTestFileClass, null, "BAM_INDEX_FILE", true))
        obj.getExecutionContext().setCurrentExecutedTool( toolEnt)

        Method loadAvailableFilenamePatternsMethod = BaseFile.class.getDeclaredMethod("loadAVailableFilenamePatterns",BaseFile,ExecutionContext)
        loadAvailableFilenamePatternsMethod.setAccessible(true);
        LinkedHashMap<FilenamePatternDependency, LinkedList<FilenamePattern>> availableFilenamePatterns = loadAvailableFilenamePatternsMethod.invoke(null, obj, mockedContext) as LinkedHashMap<FilenamePatternDependency, LinkedList<FilenamePattern>>;
        assert availableFilenamePatterns[FilenamePatternDependency.onScriptParameter].size() == 1

        Method findFilenameFromOnScriptParameterPatternsMethod = BaseFile.getDeclaredMethod("findFilenameFromOnScriptParameterPatterns", BaseFile,LinkedList, String);
        findFilenameFromOnScriptParameterPatternsMethod.setAccessible(true);
        def filenamePatterns = findFilenameFromOnScriptParameterPatternsMethod.invoke(null, obj,availableFilenamePatterns[FilenamePatternDependency.onScriptParameter],"default")
        assert filenamePatterns != null

    }

    @Test
    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    public void testFindFilenameFromSourcefilePatterns(){
        DerivedFromFilenamePattern pattern = ConfigurationFactory.readDerivedFromFilenamePattern("de.dkfz.roddy.knowledge.files.FileStage", getParsedFilenamePattern(STR_VALID_DERIVEDFROM_PATTERN).filename.getAt(0)) as DerivedFromFilenamePattern;
        mockedContext.getConfiguration().getFilenamePatterns().add(pattern);

        BaseFile parentObject= BaseFile.constructManual(syntheticTestFileClass, mockedContext, null, null, null, null, null, null);
        BaseFile obj  = BaseFile.constructGeneric(syntheticTestFileClass, parentObject, null, null, null, null, null, null, null);
        assert obj instanceof BaseFile;

        Method loadAvailableFilenamePatternsMethod = BaseFile.class.getDeclaredMethod("loadAVailableFilenamePatterns",BaseFile,ExecutionContext)
        loadAvailableFilenamePatternsMethod.setAccessible(true);
        LinkedHashMap<FilenamePatternDependency, LinkedList<FilenamePattern>> availableFilenamePatterns = loadAvailableFilenamePatternsMethod.invoke(null, obj, mockedContext) as LinkedHashMap<FilenamePatternDependency, LinkedList<FilenamePattern>>;
        assert availableFilenamePatterns[FilenamePatternDependency.derivedFrom].size() == 1

        Method findFilenameFrommSourcefilePatternsMethod = BaseFile.getDeclaredMethod("findFilenameFromSourcefilePatterns", BaseFile,LinkedList, String);
        findFilenameFrommSourcefilePatternsMethod.setAccessible(true);
        def filenamePattern = findFilenameFrommSourcefilePatternsMethod.invoke(null, obj,availableFilenamePatterns[FilenamePatternDependency.derivedFrom],"default")
        assert filenamePattern != null

    }

    @Test
    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    public void testFindFilenameFromGenericPatterns(){
        FileStageFilenamePattern pattern = ConfigurationFactory.readFileStageFilenamePattern(null,"de.dkfz.roddy.knowledge.files.FileStage", getParsedFilenamePattern(STR_VALID_FILESTAGE_PATTERN).filename.getAt(0)) as FileStageFilenamePattern;
        mockedContext.getConfiguration().getFilenamePatterns().add(pattern);

        def obj = BaseFile.constructSourceFile(syntheticTestFileClass, mockedTestFilePath, mockedContext, null, null);
        assert obj instanceof BaseFile;

        Method loadAvailableFilenamePatternsMethod = BaseFile.class.getDeclaredMethod("loadAVailableFilenamePatterns",BaseFile,ExecutionContext)
        loadAvailableFilenamePatternsMethod.setAccessible(true);
        LinkedHashMap<FilenamePatternDependency, LinkedList<FilenamePattern>> availableFilenamePatterns = loadAvailableFilenamePatternsMethod.invoke(null, obj, mockedContext) as LinkedHashMap<FilenamePatternDependency, LinkedList<FilenamePattern>>;
        assert availableFilenamePatterns[FilenamePatternDependency.FileStage].size() == 1

        Method findFilenameFrommSourcefilePatternsMethod = BaseFile.getDeclaredMethod("findFilenameFromGenericPatterns", BaseFile,LinkedList, String);
        findFilenameFrommSourcefilePatternsMethod.setAccessible(true);
        def filenamePattern = findFilenameFrommSourcefilePatternsMethod.invoke(null, obj,availableFilenamePatterns[FilenamePatternDependency.FileStage],"default")
        assert filenamePattern != null

    }

    @Test
    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    public void testGetFilename(){
        FileStageFilenamePattern pattern = ConfigurationFactory.readFileStageFilenamePattern(null,"de.dkfz.roddy.knowledge.files.FileStage", getParsedFilenamePattern(STR_VALID_FILESTAGE_PATTERN).filename.getAt(0)) as FileStageFilenamePattern;
        mockedContext.getConfiguration().getFilenamePatterns().add(pattern);

        BaseFile obj = BaseFile.constructManual(syntheticTestFileClass, mockedContext, null, null, null, null, null, null);
        assert obj instanceof BaseFile;

        Method getFilenameMethod = BaseFile.getDeclaredMethod("getFilename", BaseFile,String);
        getFilenameMethod.setAccessible(true);
        def filenamePatterns = getFilenameMethod.invoke(null, obj,"default")
        assert filenamePatterns != null

    }

}