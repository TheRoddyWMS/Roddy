/*
 * Copyright (c) 2025 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */
package de.dkfz.roddy.execution.jobs

import de.dkfz.roddy.RunMode
import de.dkfz.roddy.config.TestFileStageSettings
import de.dkfz.roddy.core.ContextResource
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.LocalExecutionService
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import de.dkfz.roddy.knowledge.files.BaseFile
import groovy.transform.CompileStatic
import spock.lang.Shared
import spock.lang.Specification

class JobSpec extends Specification {

    @Shared
    ContextResource contextResource = new ContextResource()

    def setupSpec() {
        ExecutionService.initializeService(LocalExecutionService, RunMode.CLI)
        FileSystemAccessProvider.initializeProvider(true)
        contextResource.before() // Manually initialize context resource incl. tempDir
    }

    @CompileStatic
    static class TestFile extends BaseFile {
        TestFile(BaseFile.ConstructionHelperForSourceFiles helper) {
            super(helper)
        }

        @Override
        File getPath() {
            return new File('/a/test/${jobParameter,name="abc"}/${jobParameter,name="def"}/text.txt')
        }
    }

    def "replaceParametersInFilePath replaces parameters"() {
        given:
        BaseFile bf = new TestFile(new BaseFile.ConstructionHelperForSourceFiles(
                new File("/invalid"),
                contextResource.createSimpleContext(JobSpec.name),
                new TestFileStageSettings(),
                null))

        def parm = ["abc": "avalue",
                    "def": "anothervalue"]

        expect:
        Job.replaceParametersInFilePath(bf, parm) == new File('/a/test/avalue/anothervalue/text.txt')
    }

    def "verifyFiles (placeholder)"() {
        expect:
        false // Placeholder for future implementation
    }
}
