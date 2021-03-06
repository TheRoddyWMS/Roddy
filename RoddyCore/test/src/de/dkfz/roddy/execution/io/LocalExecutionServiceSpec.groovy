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

    def "execute synchronously and succeed with captured stdout and non!-ignored stderr"() {
        when:
        ExecutionResult res = es.execute("echo 'hello'; echo 'error' >> /dev/stderr", true)

        then:
        res.exitCode == 0
        res.successful
        res.resultLines.size() == 2
        // The order of stderr and stdout is not predictable in the current implementation in RoddyToolLib.
        // Note in rare cases (1/30 testruns) a "hello" followed by 5 null values is printed an the test fails.
        (res.resultLines[0] == "hello" && res.resultLines[1] == "error") ||
            (res.resultLines[0] == "error" && res.resultLines[1] == "hello") ||
            res.resultLines[0] == "helloerror" ||
            res.resultLines[0] == "errorhello"
    }

    def "execute synchronously and fail with captured stdout and non!-ignored stderr"() {
        when:
        ExecutionResult res = es.execute("echo 'hello'; echo 'error' >> /dev/stderr; false", true)

        then:
        res.exitCode == 1
        !res.successful
        res.resultLines.size() == 2
        // The order of stderr and stdout is not predictable in the current implementation in RoddyToolLib.
        (res.resultLines[0] == "hello" && res.resultLines[1] == "error") \
            || (res.resultLines[0] == "error" && res.resultLines[1] == "hello")
    }

    def "execute synchronously and fail with empty stdout and non!-ignored stderr"() {
        when:
        ExecutionResult res = es.execute("echo 'error' >> /dev/stderr; false", true)

        then:
        res.exitCode == 1
        !res.successful
        res.resultLines.size() == 1
        res.resultLines[0] == "error"
    }

    def "execute asynchronously and fail with captured stdout but ignored! stderr"() {
        when:
        ExecutionResult res = es.execute("echo 'hello'; echo 'error' >> /dev/stderr; false", false)

        then:
        res.exitCode == 1
        !res.successful
        res.resultLines.size() == 1
        res.resultLines[0] == "hello"  // Note that the newline is removed.
    }

    def "execute asynchronously and succeed with empty stdout and ignored! stderr"() {
        when:
        ExecutionResult res = es.execute("sleep 2; echo 'error' >> /dev/stderr; true", false)

        then:
        res.exitCode == 0
        res.successful
        res.resultLines.size() == 0
    }

}
