package de.dkfz.roddy.execution.io

import de.dkfz.roddy.RunMode
import de.dkfz.roddy.core.ContextResource
import org.junit.Rule
import spock.lang.Specification

class LocalExecutionServiceSpec extends Specification {

    @Rule
    final ContextResource contextResource = new ContextResource()

    static {
        ExecutionService.initializeService(LocalExecutionService, RunMode.CLI)
    }

    LocalExecutionService es = ExecutionService.instance

    def "execute('echo')"() {
        when:
        ExecutionResult res = es.execute("echo 'hello'", true)

        then:
        res.exitCode == 0
        res.successful
        res.resultLines.size() == 1
        res.resultLines[0] == "hello"  // Note that the newline is removed.
    }

    def "execute('echo \'hello\'; false')"() {
        when:
        ExecutionResult res = es.execute("echo 'hello'; false", true)

        then:
        res.exitCode == 1
        !res.successful
        res.resultLines.size() == 1
        res.resultLines[0] == "hello"  // Note that the newline is removed.
    }

    def "execute('echo \"hello\"; false', waitFor = false)"() {
        when:
        ExecutionResult res = es.execute("echo 'hello'; false", false)

        then:
        res.exitCode == 1
        !res.successful
        res.resultLines.size() == 1
        res.resultLines[0] == "hello"  // Note that the newline is removed.
    }

    def "execute('sleep 5; false', waitFor = false)"() {
        when:
        ExecutionResult res = es.execute("sleep 2; echo 'error' >> /dev/stderr; false", false)

        then:
        res.exitCode == 1
        !res.successful
        res.resultLines.size() == 0
    }

    def "execute('sleep 5; true', waitFor = false)"() {
        when:
        ExecutionResult res = es.execute("sleep 2; true", false)

        then:
        res.exitCode == 0
        res.successful
        res.resultLines.size() == 0
    }

}
