/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs

import de.dkfz.roddy.config.TestFileStageSettings
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.MockupExecutionContextBuilder;
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.knowledge.files.FileStageSettings
import groovy.transform.CompileStatic;
import org.junit.Test;
import org.reflections.vfs.Vfs;

import java.io.File;

import static org.junit.Assert.*;
import de.dkfz.roddy.knowledge.files.BaseFile.ConstructionHelperForBaseFiles
import de.dkfz.roddy.knowledge.files.BaseFile.ConstructionHelperForManualCreation

/**
 * Created by heinold on 10.01.17.
 */
@CompileStatic
public class JobTest {
    @CompileStatic
    public static class TestFile extends BaseFile {

        public TestFile(de.dkfz.roddy.knowledge.files.BaseFile.ConstructionHelperForSourceFiles helper) {
            super(helper);
        }

        public File getPath() {
            return new File('/a/test/${jobParameter,name=\"abc\"}/${jobParameter,name=\"def\"}/text.txt');
        }

    }

    @Test
    public void replaceParametersInFilePath() throws Exception {
        BaseFile bf = new TestFile(new BaseFile.ConstructionHelperForSourceFiles(new File("/invalid"), MockupExecutionContextBuilder.createSimpleContext(JobTest.name), new TestFileStageSettings<>(), null))
        def parm = ["abc": "avalue",
                    "def": "anothervalue"
        ] as Map<String, Object>
        assert Job.replaceParametersInFilePath(bf, parm) == new File('/a/test/avalue/anothervalue/text.txt')
    }

}