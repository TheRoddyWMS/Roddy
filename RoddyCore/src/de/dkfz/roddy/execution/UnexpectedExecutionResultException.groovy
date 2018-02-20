package de.dkfz.roddy.execution

/** When some external command is executed but the result is not consistent with the expected result this exception should be thrown. */
class UnexpectedExecutionResultException extends Exception {

    final Collection<String> output

    UnexpectedExecutionResultException(String message, Collection<String> output) {
        super(message)
        assert(null != output)
        this.output = output
    }

    UnexpectedExecutionResultException(String message, Collection<String> output, boolean enableSuppression, boolean writableStackTrace) {
        super(message, null, enableSuppression, writableStackTrace)
        assert(null != output)
        this.output = output
    }
}
