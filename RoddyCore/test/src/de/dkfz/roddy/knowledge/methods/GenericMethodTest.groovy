/*
 * Copyright (c) 2017 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.methods

import static de.dkfz.roddy.config.FilenamePattern.DEFAULT_SELECTIONTAG

import de.dkfz.roddy.config.*
import de.dkfz.roddy.core.ContextResource
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.knowledge.files.FileGroup
import de.dkfz.roddy.knowledge.files.FileObject
import de.dkfz.roddy.knowledge.files.GenericFileGroup
import de.dkfz.roddy.plugins.LibrariesFactory
import groovy.transform.CompileStatic
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test

import java.lang.reflect.Constructor

/**
 * Created by heinold on 19.01.16.
 */
@CompileStatic
class GenericMethodTest {

    @ClassRule
    final public static ContextResource contextResource = new ContextResource()

    static final List<String> stringIndices = ['A', 'B', 'C', 'D']

    static Class<BaseFile> fileBaseClass
    static Class<BaseFile> derivedFileClass

    static ExecutionContext mockupContext

    @BeforeClass
    static void setup() {

        fileBaseClass = LibrariesFactory.instance.loadRealOrSyntheticClass('FileBaseClass', BaseFile.name)
        derivedFileClass = LibrariesFactory.instance.loadRealOrSyntheticClass('DerivedFileClass', BaseFile.name)

        mockupContext = contextResource.createSimpleContext(GenericMethodTest)
        def cfg = mockupContext.getConfiguration()
        cfg.getTools().add(new ToolEntry('testTool', 'testTools', '/tmp/testTool'))
        cfg.getFilenamePatterns().add(new DerivedFromFilenamePattern(derivedFileClass, fileBaseClass, '/tmp/anoutputfile_${fgindex}.txt', DEFAULT_SELECTIONTAG))
    }

    BaseFile getBaseFile() {
        return BaseFile.constructSourceFile(fileBaseClass, new File('/tmp/someFile'), mockupContext)
    }

    @Test
    @Deprecated
    void testCreateOutputFileGroupWithSubFiles() {
        Class<BaseFile> afileClass = LibrariesFactory.getInstance().loadRealOrSyntheticClass('AFile', BaseFile as Class<FileObject>)
        def tfg = new ToolFileGroupParameter(GenericFileGroup as Class<FileGroup>, [
                new ToolFileParameter(afileClass, null, 'PARMA', new ToolFileParameterCheckCondition(true)),
                new ToolFileParameter(afileClass, null, 'PARMB', new ToolFileParameterCheckCondition(true))
        ], 'APARM', DEFAULT_SELECTIONTAG)
        BaseFile baseFile = getBaseFile()
        FilenamePattern aparmFp =
                new OnScriptParameterFilenamePattern(afileClass, 'testTool', 'PARMA', 'blabla', DEFAULT_SELECTIONTAG)
        FilenamePattern bparmFp =
                new OnScriptParameterFilenamePattern(afileClass, 'testTool', 'PARMB', 'blubbiblub', DEFAULT_SELECTIONTAG)
        baseFile.executionContext.configuration.filenamePatterns.add(aparmFp)
        baseFile.executionContext.configuration.filenamePatterns.add(bparmFp)
        FileGroup fg = new GenericMethod('testTool', null, baseFile, null).createOutputFileGroup(tfg) as FileGroup
        assert fg.filesInGroup.size() == 2
    }

    @Test
    void testCreateOutputFileGroupWithNumericFGIndex() {
        def tfg = new ToolFileGroupParameter(GenericFileGroup as Class<FileGroup>, derivedFileClass, 'APARM', DEFAULT_SELECTIONTAG)
        def numericCount = 4
        FileGroup fg = new GenericMethod('testTool', null, getBaseFile(), numericCount).createOutputFileGroup(tfg) as FileGroup
        assert fg.filesInGroup.size() == numericCount
        for (int i = 0; i < numericCount; i++) {
            assert ((BaseFile) fg.filesInGroup[i]).getAbsolutePath() == "/tmp/anoutputfile_${i}.txt"
        }
    }

    @Test
    void testCreateOutputFileGroupWithStringFGIndex() {
        def tfg = new ToolFileGroupParameter(GenericFileGroup as Class<FileGroup>, derivedFileClass, 'APARM', ToolFileGroupParameter.PassOptions.parameters, ToolFileGroupParameter.IndexOptions.strings, DEFAULT_SELECTIONTAG)
        FileGroup fg = new GenericMethod('testTool', null, getBaseFile(), stringIndices).createOutputFileGroup(tfg) as FileGroup
        assert fg.filesInGroup.size() == stringIndices.size()
        for (int i = 0; i < stringIndices.size(); i++) {
            assert ((BaseFile) fg.filesInGroup[i]).getAbsolutePath() == "/tmp/anoutputfile_${stringIndices[i]}.txt"
        }
    }

    @Test(expected = NegativeArraySizeException)
    void testCreateOutputFileGroupWithNegativeIndexValues() {
        def tfg = new ToolFileGroupParameter(GenericFileGroup as Class<FileGroup>, fileBaseClass, 'APARM', ToolFileGroupParameter.PassOptions.parameters, ToolFileGroupParameter.IndexOptions.strings, DEFAULT_SELECTIONTAG)
        new GenericMethod('testTool', null, getBaseFile(), -1).createOutputFileGroup(tfg) as FileGroup
    }

    @Test(expected = RuntimeException)
    void testCreateOutputFileGroupWithMissingIndexValues() {
        def tfg = new ToolFileGroupParameter(GenericFileGroup as Class<FileGroup>, fileBaseClass, 'APARM', ToolFileGroupParameter.PassOptions.parameters, ToolFileGroupParameter.IndexOptions.strings, DEFAULT_SELECTIONTAG)
        new GenericMethod('testTool', null, getBaseFile(), []).createOutputFileGroup(tfg) as FileGroup
    }

    @Test
    void testSearchConstructor() {
        Constructor constructor = GenericMethod.searchBaseFileConstructorForConstructionHelperObject(fileBaseClass)
        assert constructor != null
    }

}