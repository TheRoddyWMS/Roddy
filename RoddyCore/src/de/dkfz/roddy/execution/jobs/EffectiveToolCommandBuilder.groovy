package de.dkfz.roddy.execution.jobs

import com.google.common.base.Preconditions
import de.dkfz.roddy.config.ConfigurationError
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextError
import de.dkfz.roddy.core.JobExecutionEnvironment
import de.dkfz.roddy.execution.ApptainerCommandBuilder
import de.dkfz.roddy.execution.Code
import de.dkfz.roddy.execution.CommandReferenceI
import de.dkfz.roddy.execution.Command
import de.dkfz.roddy.execution.Executable
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.jetbrains.annotations.NotNull
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import java.util.logging.Level


/** The following functions unroll/flatten a decision tree on the types of parameters using dynamic dispatch.
 *
 *  The effective command, after the possible application of wrapping into a container.
 *  As of now, the information for the binding/mounts and the container engine and arguments are
 *  taken from the global configuration (i.e. one container image far all jobs). This could be moved
 *  into the tool-declarations (maybe as resources?).
 *
 *  NOTE: The methods are static and take the ExecutionContext as parameter, because they are called in the
 *        constructor, before the Job.executionContext has been set. */
@CompileDynamic
class EffectiveToolCommandBuilder {

    public static final String TOOLID_WRAPIN_SCRIPT = "wrapinScript"

    private final ExecutionContext context

    @CompileStatic
    private EffectiveToolCommandBuilder(@NotNull ExecutionContext context) {
        this.context = context
    }

    @CompileStatic
    @NotNull static EffectiveToolCommandBuilder from(@NotNull ExecutionContext context) {
        new EffectiveToolCommandBuilder(context)
    }

    @CompileStatic
    private Command getWrapInCommand() {
        new Command(new Executable(
                context.configuration.getProcessingToolPath(context, TOOLID_WRAPIN_SCRIPT).toPath(),
                context.getToolMd5(TOOLID_WRAPIN_SCRIPT)))
    }

    /** This function keeps the CompileDynamic to the part of the code that needs it. Thus we keep the benefits of
     *  static type checking in other complex code. For this function here, no dynamic dispatch is needed
     *  to find this function, in contrast to the ...Impl functions below. */
    Optional<ToolCommand> build(@NotNull AnyToolCommand toolCommand) {
        getEffectiveToolCommandImpl(toolCommand)
    }

    /** If there is no toolId, command, or code object to execute provided, then we basically can't execute anything.
     *  This only happens, if we are in "simulation" mode. We continue by returning just the (empty) original
     *  toolCommand. It does not really make sense to add any Apptainer/Singularity code here, because also that
     *  would not be executed and could even not get executed, because there simply is no final command to execute.
     */
    private Optional<ToolCommand> getEffectiveToolCommandImpl(@NotNull UnknownToolCommand toolCommand) {
        Preconditions.checkArgument(!context.executionContextLevel.canSubmitJobs)
        // Not sure whether here we should be `context.addWarning()`?
        Optional.empty()
    }

    /** This method decomposes the decision on how to progress on the type of the Command object contained
     *  in the toolCommand. This could be any of the concrete subclasses of CommandI.
     *
     *  It also covers the special case, where no command object is stored in the toolCommand, in which case
     *  command object is created using the tool path (remote) retrieved using the toolId. This is done using
     *  the WRAPPED_SCRIPT environment variable used by the the wrapInScript.sh and set in the Job constructor.
     */
    private Optional<ToolCommand> getEffectiveToolCommandImpl(@NotNull ToolCommand toolCommand) {
        // Continue dispatching dependent on the type of the command.
        getEffectiveToolCommandImpl(toolCommand, toolCommand.command)
    }

    /** If we have no explicit command object then the command is implicit in the toolId.
     *  We therefore retrieve the information from the execution context, if possible. */
    private Optional<ToolCommand> getEffectiveToolCommandImpl(@NotNull ToolIdCommand toolCommand)
            throws ConfigurationError {
        maybeWrappedInContainerCommand(context.getToolCommand(toolCommand.toolId))
    }

    /** A Code command (string) is fed into a submission command in different way than reference (path) to
     *  a script. Here we compose the effective ToolCommand for this special situation. This is the behaviour
     *  of the old treatment of "inline scripts", but with the Singularity/Apptainer feature added.
     */
    private Optional<ToolCommand> getEffectiveToolCommandImpl(@NotNull ToolCommand toolCommand,
                                                              @NotNull Code command) {
        if ([JobExecutionEnvironment.apptainer, JobExecutionEnvironment.simpleName].
                contains(context.jobExecutionEnvironment)) {
            context.addError(ExecutionContextError.CANNOT_WRAP_INLINE_SCRIPT_IN_CONTAINER_CALL.expand(
                    "Tried to submit inline script ${toolCommand.toolId} to execute " +
                    "with $context.jobExecutionEnvironment",
                    Level.SEVERE))
            // Note: This could be implemented, e.g. dealing with code via local files. But I'm not doing it, because
            //       the feature anyway is not used by anyone.
            Optional.empty()
        } else {
            Optional.of(new ToolCommand(toolCommand.toolId,
                                        wrapInCommand,
                                        toolCommand.localPath))
        }
    }

    /** All executables will be executed via the `wrapInScript.sh` in such a way that the command is wrapped by the
     *  wrapInScript.sh, and the information about which executable to execute is communicated to that script using
     *  the configuration file that is provided via the PARAMETER_FILE environment variable, which is read by the
     *  `wrapInScript.sh`.
     */
    private Optional<ToolCommand> getEffectiveToolCommandImpl(@NotNull ToolCommand toolCommand,
                                                              @NotNull Executable executable) {
        // Note that the actual executable is replaced by the wrapInScript. We do not cliAppend it, because
        // it is not necessary and would actually be misleading, because it wouldn't be used anyway.
        Optional.of(maybeWrappedInContainerCommand(toolCommand, wrapInCommand))
    }

    private @NotNull ToolCommand maybeWrappedInContainerCommand(@NotNull ToolCommand toolCommand,
                                                                @NotNull CommandReferenceI command) {
        if ([JobExecutionEnvironment.apptainer, JobExecutionEnvironment.simpleName].
                contains(context.jobExecutionEnvironment)) {
            // The resulting wrapping will be
            //
            //   bsub ... apptainer ... wrapInScript.sh
            //
            // Thus only few parameters like PARAMETER_FILE, LSB_JOBID, LSB_JOBNAME, etc. need to be forwarded
            // into the apptainer/singularity container. The wrapper then sets up the environment with from
            // the baseEnvironmentScript, jobEnvironmentScript, and PARAMETER_FILE and runs the wrapped script.
            //
            // Note that the working directory is set to the output directory, which is different from the
            // non-wrapped situation, where it is (and as of now) remains the $HOME directory. The reasons are
            //
            //    (1) usually $HOME won't be in the container and
            //    (2) what is the sense of using $HOME anyway? For better isolation we don't want to mess with home.
            //
            ApptainerCommandBuilder builder = ApptainerCommandBuilder
                    .create()
                    .withAddedBindingSpecs(context.userContainerMounts + context.roddyMounts)
                    .withAddedEngineArgs(context.userContainerEngineArguments)
                    .withApptainerExecutable(context.containerEnginePath)
                    .withWorkingDir(context.outputDirectory.toPath())
                    .withAddedExportedEnvironmentVariables(context.roddyContainerExportedVariables)
            new ToolCommand(
                    toolCommand.toolId,    // keep the toolId
                    builder.build(context.containerImage).   // call command in container context, ...
                            cliAppend(command, false, true), // ... and quote
                    toolCommand.localPath)
        } else {
            toolCommand
        }
    }

    /** This situation should not occur. Currently, Roddy and BE only executed tool scripts, identified by
     *  path or toolID, but it is not possible to run commands with parameters. */
    private Optional<ToolCommand> getEffectiveToolCommandImpl(@NotNull ToolCommand toolCommand,
                                                              @NotNull Command command) {
        throw new NotImplementedException()
    }

}
