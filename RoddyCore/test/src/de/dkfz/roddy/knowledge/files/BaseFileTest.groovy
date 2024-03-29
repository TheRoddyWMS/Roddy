/*
 * Copyright (c) 2017 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.files

import static de.dkfz.roddy.Constants.DEFAULT

import de.dkfz.roddy.RunMode
import de.dkfz.roddy.config.*
import de.dkfz.roddy.config.loader.ConfigurationFactory as CF
import de.dkfz.roddy.core.ContextResource
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.LocalExecutionService
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.execution.jobs.BEJobResult
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.plugins.LibrariesFactoryTest
import de.dkfz.roddy.tools.Tuple2
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.slurpersupport.NodeChild
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

import java.lang.reflect.Method

/**
 * Created by heinold on 20.01.16.
 */
@CompileStatic
class BaseFileTest {

    @Rule
    public final ContextResource contextResource = new ContextResource()

    private static Class<BaseFile> syntheticTestFileClass
    static ExecutionContext mockedContext
    static RecursiveOverridableMapContainer<String, FilenamePattern, Configuration> filenamePatternsInMockupContext

    static final File mockedTestFilePath = new File('/tmp/RoddyTests/testfile')

    /**
     * The caching mechanism in BaseFile used to load patterns related to the class will not work for these tests. If they all use the same class
     * only the first active test will be working. This is why we have so many differnt synthetic classes here.
     */
    static final String TEST_BASE_FILE_PREFIX = "TestBaseFile_"
    private static final String STR_VALID_DERIVEDFROM_PATTERN =
            "<filename class='${TEST_BASE_FILE_PREFIX}DERIVEDFROM' derivedFrom='GenericFile' pattern='/tmp/onderivedFile'/>"
    private static final String STR_VALID_ONMETHOD_PATTERN_WITH_CLASSNAME =
            "<filename class='${TEST_BASE_FILE_PREFIX}ONMETHOD' onMethod='BaseFileTest.testFindFilenameFromOnMethodPatterns' pattern='/tmp/onMethodwithClassName'/>"
    private static final String STR_VALID_ONTOOL_PATTERN =
            "<filename class='${TEST_BASE_FILE_PREFIX}ONTOOL' onTool='testScript' pattern='/tmp/onTool'/>"
    private static final String STR_VALID_FILESTAGE_PATTERN =
            "<filename class='${TEST_BASE_FILE_PREFIX}FILESTAGE' fileStage=\"GENERIC\" pattern='/tmp/FileWithFileStage'/>"
    private static final String STR_VALID_ONSCRIPTPARAMETER_WITH_TOOL_AND_PARAMNAME =
            "<filename class='${TEST_BASE_FILE_PREFIX}ONSCRIPT' onScriptParameter='testScript:BAM_INDEX_FILE' pattern='/tmp/onScript' />"

    private NodeChild getParsedFilenamePattern(String filenamePattern) {
        parseXML("<filenames filestagesbase='de.dkfz.roddy.knowledge.files.FileStage'>${filenamePattern}</filenames>")
    }

    private static NodeChild parseXML(String xml) {
        new XmlSlurper().parseText(xml) as NodeChild
    }

    @Before
    void setupBaseFileTests() {
        ExecutionService.initializeService(LocalExecutionService, RunMode.CLI)
        FileSystemAccessProvider.initializeProvider(true)
        //Setup plugins and default configuration folders
        LibrariesFactory libFact = LibrariesFactory.initializeFactory(true)

        libFact.loadLibraries(
                LibrariesFactory.buildupPluginQueue(
                        LibrariesFactoryTest.callLoadMapOfAvailablePlugins(), 'DefaultPlugin'
                ).values() as List
        )
        CF.initialize(libFact.getLoadedPlugins().collect { it -> it.getConfigurationDirectory() })

        final Configuration mockupConfig = new Configuration(
                new PreloadedConfiguration(null, Configuration.ConfigurationType.OTHER, DEFAULT,
                        '', '', null, '', ResourceSetSize.l, null,
                        null, null, null),
                CF.instance.getConfiguration(DEFAULT)) {
            @Override
            File getSourceToolPath(String tool) {
                if (tool == 'wrapinScript')
                    return super.getSourceToolPath(tool)
                return new File('/tmp/RoddyTests/RoddyTestScript_ExecutionServiceTest.sh')
            }
        }
        syntheticTestFileClass = libFact.loadRealOrSyntheticClass('FileWithFileStage', BaseFile.class as Class<FileObject>)
        ToolEntry toolEntry = new ToolEntry('RoddyTests', 'RoddyTests', 'RoddyTestScript_ExecutionServiceTest.sh')
        toolEntry.getOutputParameters(mockupConfig).add(new ToolFileParameter(syntheticTestFileClass, null, 'TEST',
                new ToolFileParameterCheckCondition(true)))

        mockupConfig.tools.add(toolEntry)
        mockedContext = contextResource.createSimpleContext(BaseFileTest, mockupConfig)
        filenamePatternsInMockupContext = mockedContext.configuration.filenamePatterns
    }

    static Class<BaseFile> getTestFileClass(String method) {
        LibrariesFactory.instance.loadRealOrSyntheticClass("${TEST_BASE_FILE_PREFIX}${method}", BaseFile.class as Class<FileObject>)
    }

    @Test
    void testCreationOfBaseFileWithSourceHelper() {
        BaseFile.ConstructionHelperForSourceFiles helperObject =
                new BaseFile.ConstructionHelperForSourceFiles(mockedTestFilePath, mockedContext, null, null)
        BaseFile instance = syntheticTestFileClass.newInstance(helperObject)
        assert instance
        assert instance.executionContext == mockedContext
        assert instance.path == mockedTestFilePath
    }

    @Test
    void testConstructForCreationOfBaseFileWithSourceHelper() {
        def obj = BaseFile.constructSourceFile(syntheticTestFileClass, mockedTestFilePath, mockedContext, null, null)
        assert obj instanceof BaseFile
    }

    @Test
    void testConstructForGenericCreationWithParentFile() {
        BaseFile parentObject = ContextResource.makeTestBaseFileInstance(mockedContext, 'parentFile')
        ToolEntry toolEntry = new ToolEntry('testTool', '/blabla', 'bla')
        String toolID = 'testTool'
        String parameterID = null
        String selectionTag = null
        FileStageSettings fileStageSettings = null
        BEJobResult jobResult = null
        BaseFile file = BaseFile.constructGeneric(GenericFile, parentObject, null as List,
                toolEntry, toolID, parameterID, selectionTag, null,
                fileStageSettings, jobResult)
        assert file && file.class == GenericFile
        assert file.executionContext == mockedContext
    }

    @Test
    void testConstructForGenericCreationWithExecutionContext() {
        mockedContext.configuration.filenamePatterns.
                add(new OnToolFilenamePattern(syntheticTestFileClass as Class<BaseFile>, 'testTool', '/bla/bla', null))
        ToolEntry tool = new ToolEntry('testTool', '/tmp', 'testTool')
        mockedContext.currentExecutedTool = tool
        def obj = BaseFile.constructGeneric(syntheticTestFileClass, mockedContext,
                tool, 'testTool', null, null, null, null)
        assert obj instanceof BaseFile
    }

    @Ignore('What is the difference between generic creation and manual creation? What does this test add that is not already tested elsewhere?')
    @Test
    void testConstructForManualCreationWithParentFile() {
        BaseFile parentObject = new GenericFile(new BaseFile.ConstructionHelperForSourceFiles(mockedTestFilePath, mockedContext, null, null))
        ToolEntry toolEntry = null
        String methodID = null
        String slotID = null
        String selectionTag = null
        FileStageSettings fileStageSettings = null
        BEJobResult jobResult = null
        BaseFile instance = BaseFile.constructManual(syntheticTestFileClass, parentObject, null, toolEntry, methodID, slotID, selectionTag, fileStageSettings, jobResult)
        assert instance && instance.class == syntheticTestFileClass
        assert instance.executionContext == mockedContext
    }

    @Test
    void testConstructForManualCreationWithExecutionContext() {
        mockedContext.configuration.filenamePatterns.
                add(new OnToolFilenamePattern(syntheticTestFileClass as Class<BaseFile>, 'testTool', '/bla/bla', null))
        ToolEntry tool = new ToolEntry('testTool', '/tmp', 'testTool')
        mockedContext.currentExecutedTool = tool
        def obj = BaseFile.constructManual(syntheticTestFileClass, mockedContext, tool, tool.ID, null, null, null, null)
        assert obj instanceof BaseFile
    }

    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    private NodeChild parseFilenamePattern(String patternXML) {
        getParsedFilenamePattern(patternXML).filename[0] as NodeChild
    }

    Tuple2<File, FilenamePattern> callBaseFileFindFilenameDerivateMethod(String method, BaseFile obj, FilenamePatternDependency dependency, String tag = DEFAULT) {
        LinkedHashMap<FilenamePatternDependency, LinkedList<FilenamePattern>> availableFilenamePatterns =
                BaseFile.loadAvailableFilenamePatternsForBaseFileClass(obj, mockedContext) as LinkedHashMap<FilenamePatternDependency, LinkedList<FilenamePattern>>;
        assert availableFilenamePatterns[dependency].size() == 1

        Method m = BaseFile.getDeclaredMethod(method, BaseFile, LinkedList, String)
        m.setAccessible(true)
        m.invoke(null, obj, availableFilenamePatterns[dependency], tag) as Tuple2<File, FilenamePattern>
    }

    @Test
    void testFindFilenameFromOnMethodPatterns() {
        OnMethodFilenamePattern pattern = CF.
                readOnMethodFilenamePattern(null, parseFilenamePattern(STR_VALID_ONMETHOD_PATTERN_WITH_CLASSNAME)) as OnMethodFilenamePattern
        filenamePatternsInMockupContext.add(pattern)

        BaseFile obj = BaseFile.constructManual(getTestFileClass('ONMETHOD'), mockedContext,
                null, null, null, null, null, null)
        assert obj instanceof BaseFile


        LinkedHashMap<FilenamePatternDependency, LinkedList<FilenamePattern>> availableFilenamePatterns =
                BaseFile.loadAvailableFilenamePatternsForBaseFileClass(obj, mockedContext) as LinkedHashMap<FilenamePatternDependency, LinkedList<FilenamePattern>>;
        assert availableFilenamePatterns[FilenamePatternDependency.onMethod].size() == 1
    }

    @Test
    void testFindFilenameFromOnToolIDPatterns() {
        OnToolFilenamePattern pattern = CF.readOnToolFilenamePattern(null, parseFilenamePattern(STR_VALID_ONTOOL_PATTERN)) as OnToolFilenamePattern
        filenamePatternsInMockupContext.add(pattern)
        def obj = BaseFile.constructSourceFile(getTestFileClass('ONTOOL'), mockedTestFilePath, mockedContext, null, null)
        assert obj instanceof BaseFile
        def toolEntry = new ToolEntry('testScript', 'RoddyTests', 'RoddyTestScript_ExecutionServiceTest.sh')
        toolEntry.getOutputParameters(mockedContext.getConfiguration()).add(new ToolFileParameter(syntheticTestFileClass, null, 'BAM_INDEX_FILE', new ToolFileParameterCheckCondition(true)))
        obj.getExecutionContext().setCurrentExecutedTool(toolEntry)

        Tuple2<File, FilenamePattern> filenamePatterns = callBaseFileFindFilenameDerivateMethod('findFilenameFromOnToolIDPatterns', obj, FilenamePatternDependency.onTool)
        assert filenamePatterns != null
    }

    @Test
    @Ignore('Fix! Some reflection magic.')
    void testFindFilenameFromOnScriptParameterPatterns() {
        OnScriptParameterFilenamePattern pattern = 
                CF.readOnScriptParameterFilenamePattern(FileStage.class.name, 
                        parseFilenamePattern(STR_VALID_ONSCRIPTPARAMETER_WITH_TOOL_AND_PARAMNAME)) as OnScriptParameterFilenamePattern
        filenamePatternsInMockupContext.add(pattern)
        BaseFile obj = BaseFile.constructManual(getTestFileClass('ONSCRIPT'), mockedContext, null, null, 
                null, null, null, null)
        assert obj instanceof BaseFile
        
        def toolEntry = new ToolEntry('testScript', 'RoddyTests', 'RoddyTestScript_ExecutionServiceTest.sh')
        toolEntry.getOutputParameters(mockedContext.getConfiguration()).
                add(new ToolFileParameter(getTestFileClass('ONSCRIPT'), null, 'BAM_INDEX_FILE',
                        new ToolFileParameterCheckCondition(true)))
        obj.executionContext.currentExecutedTool = toolEntry

        Tuple2<File, FilenamePattern> filenamePatterns = 
                callBaseFileFindFilenameDerivateMethod('findFilenameFromOnScriptParameterPatterns', obj,
                        FilenamePatternDependency.onScriptParameter)
        assert filenamePatterns != null
    }

    @Test
    void testFindFilenameFromSourcefilePatterns() {
        BaseFile parentObject = ContextResource.makeTestBaseFileInstance(mockedContext, 'testParent')

        ToolEntry tool = new ToolEntry('testTool2', '/bla/bla', 'bla')
        DerivedFromFilenamePattern pattern =
                CF.readDerivedFromFilenamePattern(FileStage.class.name, parseFilenamePattern(STR_VALID_DERIVEDFROM_PATTERN)) as DerivedFromFilenamePattern
        filenamePatternsInMockupContext.add(pattern)
        BaseFile obj =
                BaseFile.constructGeneric(getTestFileClass('DERIVEDFROM'), parentObject, [parentObject] as List<FileObject>,
                        tool, tool.ID, null, null, null, null)
        assert obj instanceof BaseFile
        Tuple2<File, FilenamePattern> filenamePatterns =
                callBaseFileFindFilenameDerivateMethod('findFilenameFromSourcefilePatterns', obj, FilenamePatternDependency.derivedFrom)
        assert filenamePatterns != null
    }

    @Test
    void testFindFilenameFromGenericPatterns() {
        FileStageFilenamePattern pattern = CF.readFileStageFilenamePattern(null, FileStage.class.name, parseFilenamePattern(STR_VALID_FILESTAGE_PATTERN)) as FileStageFilenamePattern
        filenamePatternsInMockupContext.add(pattern)
        def obj = BaseFile.constructSourceFile(getTestFileClass('FILESTAGE'), mockedTestFilePath, mockedContext, null, null)
        assert obj instanceof BaseFile
        Tuple2<File, FilenamePattern> filenamePatterns = callBaseFileFindFilenameDerivateMethod('findFilenameFromGenericPatterns', obj, FilenamePatternDependency.FileStage)
        assert filenamePatterns != null
    }

    @Test
    void testGetFilename() {
        FileStageFilenamePattern pattern = CF.readFileStageFilenamePattern(null, FileStage.class.name, parseFilenamePattern(STR_VALID_FILESTAGE_PATTERN)) as FileStageFilenamePattern
        filenamePatternsInMockupContext.add(pattern)
        BaseFile obj = BaseFile.constructManual(getTestFileClass('FILESTAGE'), mockedContext, null, null,
                null, null, null, null)
        assert obj instanceof BaseFile
        Method getFilenameMethod = BaseFile.getDeclaredMethod('getFilename', BaseFile, String)
        getFilenameMethod.setAccessible(true)
        def filenamePatterns = getFilenameMethod.invoke(null, obj, DEFAULT)
        assert filenamePatterns != null
    }

}
