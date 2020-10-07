/*
 * Copyright (c) 2020 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.io

import de.dkfz.roddy.SystemProperties
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationConstants
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.execution.UnexpectedExecutionResultException
import de.dkfz.roddy.tools.LoggerWrapper
import groovy.transform.CompileStatic

import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * The local execution service executes commands on the local machine. For this groovy's execute() method is used.
 * All commands are executed using Bash as interpreter!
 */
@CompileStatic
class LocalExecutionService extends ExecutionService {

    private static final LoggerWrapper logger = LoggerWrapper.getLogger(LocalExecutionService.class.name)

    // Note that the thread pool is unbounded.
    private ExecutorService executorService = Executors.newCachedThreadPool()

    @Override
    String getUsername() {
        return SystemProperties.getUserName()
    }

    @Override
    void addSpecificSettingsToConfiguration(Configuration configuration) {
        //Disable several things which don't work.
        configuration.getConfigurationValues().add(new ConfigurationValue(ConfigurationConstants.CVALUE_PROCESS_OPTIONS_SETUSERGROUP, "false"))
        configuration.getConfigurationValues().add(new ConfigurationValue(ConfigurationConstants.CVALUE_PROCESS_OPTIONS_SETUSERMASK, "false"))
        configuration.getConfigurationValues().add(new ConfigurationValue(ConfigurationConstants.CVALUE_PROCESS_OPTIONS_QUERY_ID, "false"))
    }

    @Override
    boolean isLocalService() {
        return true
    }

    /** Read from an InputStream asynchronously using the executorService.
        May throw an UncheckedIOException.
     **/
    private CompletableFuture<List<String>> asyncReadStringStream(
            InputStream inputStream,
            ExecutorService executorService = executorService) {
        return CompletableFuture.supplyAsync({
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))
            reader.lines().toArray() as List<String>
        } as Supplier<List<String>>, executorService)
    }

    /**
     * @param command        The command to execute. Note, this is always executed with Bash as the command interpreter.
     * @param waitFor        Whether to do synchronous (true) or asynchronous execution. Asynchronous execution
     *                       will be done on the class-internal thread-pool. Don't do this with mutually blocking
     *                       processes.
     * @param ignoreErrors   Ignored. No execution errors are reported. This is really nasty, but that's the way it
     *                       is in the moment that doesn't break dependencies. Instead the method logs errors to the
     *                       logging output.
     * @param outputStream   Ignored.
     * @return
     */
    // This method actually overrides a base class. But if we keep the @Override, the Groovy (or Java) compiler constantly
    // claims, that the method does not override it's base method.
    // That is, why we keep it in but only as a comment.
//     @Override
    protected ExecutionResult _execute(String command,
                                       boolean waitFor,
                                       boolean ignoreErrors,
                                       OutputStream outputStream = null) {
        if (waitFor) {
            return LocalExecutionHelper.executeCommandWithExtendedResult(command, outputStream)
        } else {
            ProcessBuilder processBuilder = new ProcessBuilder(["bash", "-c", command])
            Process process = processBuilder.start()
            Future<List<String>> stdout = asyncReadStringStream(process.getInputStream())
            Future<List<String>> stderr = asyncReadStringStream(process.getErrorStream())
            Future<Integer> exitCode = CompletableFuture.supplyAsync({
                int exitCode = 0
                Optional<String> message = Optional.empty()
                try {
                    exitCode = process.waitFor()
                    if (exitCode != 0) {
                        message = Optional.of(["Error executing command (exitCode=${exitCode}, command=${command}):",
                                               "stdout={${stdout.get().join("\n")}}",
                                               "stderr={${stderr.get().join("\n")}}"].join("\n"))
                    }
                } catch (InterruptedException e) {
                    message = Optional.of("Interrupted command=${command}" as String)
                }
                message.ifPresent(new Consumer<String>() {
                    // Groovy 2.4.19 fails to generate the Closure->Consumer shim.
                    @Override
                    void accept(String msgString) {
                        if (ignoreErrors) {
                            logger.postAlwaysInfo(msgString)
                        } else {
                            throw new UnexpectedExecutionResultException(msgString, [])
                        }
                    }
                })
                exitCode as Integer
            } as Supplier<Integer>, executorService)

            return new AsyncExecutionResult(exitCode, stdout, stderr)
        }
    }

    /**
     * Should be the current directory
     */
    @Override
    File queryWorkingDirectory() {
        return new File("")
    }

    @Override
    boolean isAvailable() {
        return true
    }

}
