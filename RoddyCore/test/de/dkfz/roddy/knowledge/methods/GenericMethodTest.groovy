/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.methods

import de.dkfz.roddy.config.DerivedFromFilenamePattern
import de.dkfz.roddy.config.ToolEntry
import de.dkfz.roddy.config.ToolFileGroupParameter
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.MockupExecutionContextBuilder
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.knowledge.files.FileGroup
import de.dkfz.roddy.knowledge.files.GenericFileGroup
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.plugins.LibrariesFactoryTest
import groovy.transform.CompileStatic
import org.junit.BeforeClass
import org.junit.Test

import java.lang.reflect.Constructor

/**
 * Created by heinold on 19.01.16.
 */
@CompileStatic
class GenericMethodTest {

    public static final List<String> stringIndices = ["A", "B", "C", "D"]

    static Class<BaseFile> fileBaseClass
    static Class<BaseFile> derivedFileClass

//    static class FileTestClass extends BaseFile {
//        FileTestClass(BaseFile.ConstructionHelperForBaseFiles helper) {
//            super(helper)
//        }
//    }
//
//    static class DerivedFileClass extends BaseFile {
//        DerivedFileClass(BaseFile.ConstructionHelperForBaseFiles helper) {
//            super(helper)
//        }
//    }

    static ExecutionContext mockupContext

    @BeforeClass
    static void setup() {

        fileBaseClass = LibrariesFactory.getInstance().loadRealOrSyntheticClass("FileBaseClass", BaseFile.name)
        derivedFileClass = LibrariesFactory.getInstance().loadRealOrSyntheticClass("DerivedFileClass", BaseFile.name)

        mockupContext = MockupExecutionContextBuilder.createSimpleContext(GenericMethodTest)
        def cfg = mockupContext.getConfiguration()
        cfg.getTools().add(new ToolEntry("testTool", "testTools", "/tmp/testTool"))
        cfg.getFilenamePatterns().add(new DerivedFromFilenamePattern(derivedFileClass, fileBaseClass, '/tmp/anoutputfile_${fgindex}.txt', "default"))
    }

    BaseFile getBaseFile() {
        return BaseFile.constructSourceFile(fileBaseClass, new File("/tmp/someFile"), mockupContext)
    }

    @Test
    void testCreateOutputFileGroupWithNumericFGIndex() {
        def tfg = new ToolFileGroupParameter(GenericFileGroup as Class<FileGroup>, derivedFileClass, null, "APARM")
        def numericCount = 4
        FileGroup fg = new GenericMethod("testTool", null, getBaseFile(), numericCount).createOutputFileGroup(tfg) as FileGroup
        assert fg.filesInGroup.size() == numericCount
        for (int i = 0; i < numericCount; i++) {
            assert ((BaseFile) fg.filesInGroup[i]).getAbsolutePath() == "/tmp/anoutputfile_${i}.txt"
        }
    }

    @Test
    void testCreateOutputFileGroupWithStringFGIndex() {
        def tfg = new ToolFileGroupParameter(GenericFileGroup as Class<FileGroup>, derivedFileClass, null, "APARM", ToolFileGroupParameter.PassOptions.parameters, ToolFileGroupParameter.IndexOptions.strings)
        FileGroup fg = new GenericMethod("testTool", null, getBaseFile(), stringIndices).createOutputFileGroup(tfg) as FileGroup
        assert fg.filesInGroup.size() == stringIndices.size()
        for (int i = 0; i < stringIndices.size(); i++) {
            assert ((BaseFile) fg.filesInGroup[i]).getAbsolutePath() == "/tmp/anoutputfile_${stringIndices[i]}.txt"
        }
    }

    @Test(expected = Exception)
    void testCreateOutputFileGroupWithNegativeIndexValues() {
        def tfg = new ToolFileGroupParameter(GenericFileGroup as Class<FileGroup>, fileBaseClass, null, "APARM", ToolFileGroupParameter.PassOptions.parameters, ToolFileGroupParameter.IndexOptions.strings)
        FileGroup fg = new GenericMethod("testTool", null, getBaseFile(), -1).createOutputFileGroup(tfg) as FileGroup
    }

    @Test(expected = RuntimeException)
    void testCreateOutputFileGroupWithMissingIndexValues() {
        def tfg = new ToolFileGroupParameter(GenericFileGroup as Class<FileGroup>, fileBaseClass, null, "APARM", ToolFileGroupParameter.PassOptions.parameters, ToolFileGroupParameter.IndexOptions.strings)
        FileGroup fg = new GenericMethod("testTool", null, getBaseFile(), []).createOutputFileGroup(tfg) as FileGroup
    }

    @Test
    void testSearchConstructor() {
        Constructor constructor = GenericMethod.searchBaseFileConstructorForConstructionHelperObject(fileBaseClass)
        assert constructor != null
    }

}