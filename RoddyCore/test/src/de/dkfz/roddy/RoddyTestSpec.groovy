/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */
package de.dkfz.roddy

import de.dkfz.roddy.core.ContextResource
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.LocalExecutionService
import de.dkfz.roddy.execution.io.fs.FileSystemAccessProvider
import groovy.transform.CompileStatic
import org.junit.ClassRule
import org.junit.Rule
import spock.lang.Shared
import spock.lang.Specification

/**
 * Base class for Spock tests which does some basic initialization stuff.
 */
@CompileStatic
class RoddyTestSpec extends Specification {

    @Rule
    final ContextResource contextResource = new ContextResource()

    static {
        ExecutionService.initializeService(LocalExecutionService, RunMode.CLI)
        FileSystemAccessProvider.initializeProvider(true)
    }
}
