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

import java.nio.file.Paths
import java.util.logging.Level

/**
 *  The effective command, after the possible application of wrapping into a container and changing the primary
 *  group.
 *
 *  As of now, the information for the binding/mounts and the container engine and arguments are
 *  taken from the global configuration (i.e. one container image far all jobs). This could be moved
 *  into the tool-declarations (maybe as resources?).
 *
 **/
@CompileDynamic
class EffectiveToolCommandBuilder {

    private final ExecutionContext context

    @CompileStatic
    private EffectiveToolCommandBuilder(@NotNull ExecutionContext context) {
        this.context = context
    }

    @CompileStatic
    @NotNull static EffectiveToolCommandBuilder from(@NotNull ExecutionContext context) {
        new EffectiveToolCommandBuilder(context)
    }

    // The following functions unroll/flatten a decision tree on the types of parameters using dynamic dispatch.

    /** This function keeps the CompileDynamic to the part of the code that needs it. Thus we keep the benefits of
     *  static type checking in other complex code. For this function here, no dynamic dispatch is needed
     *  to find this function, in contrast to the ...Impl functions below.
     *
     *  Important: This takes any command object. If you want to run a plain, unwrapped command us an Executable
     *             or Command instance. If you want to wrap the command into a wrapInScript.sh call, which then
     *             selects the actual (wrapped) command via the toolID, then use a ToolIdCommand.
     *
     *  */
    Optional<ToolCommand> build(@NotNull AnyToolCommand toolCommand) {
        Preconditions.checkArgument(toolCommand != null)
        getEffectiveToolCommandImpl(toolCommand)
    }

    /** If there is no toolId, command, or code object to execute provided, then we basically can't execute anything.
     *  This only happens, if we are in "simulation" mode. We continue by returning just the (empty) original
     *  toolCommand. It does not really make sense to add any Apptainer/Singularity code here, because also that
     *  would not be executed and could even not get executed, because there simply is no final command to execute.
     */
    private Optional<ToolCommand> getEffectiveToolCommandImpl(@NotNull UnknownToolCommand toolCommand) {
        Preconditions.checkArgument(
                !context.executionContextLevel.allowedToSubmitJobs,
                "UnkwownToolCommand should not occur for ${context.executionContextLevel}")
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
        Optional.of(scriptWrappedCommand(context.getToolCommand(toolCommand.toolId),
                                         context.wrapInCommand))
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
            throw new UnsupportedOperationException("Feeding Code inline scipts into containers is not supported.")
            // Note: This could be implemented, e.g. dealing with code via local files. But I'm not doing it, because
            //       the feature anyway is not used by anyone.
            Optional.empty()
        } else {
            // If we are dealing with code, Roddy's (current) way is to communicate the code to the submission command
            // via its standard input. The submission code in BatchEuphoria therefore decides what to do. In any case,
            // we must not add a wrapInScript.sh call here, because something like `cat command | bsub anotherCommand`
            // will execute `anotherCommand`. The Code class exists exactly for this communication into BE. Therefore:
            Optional.of(toolCommand)
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
        Optional.of(scriptWrappedCommand(toolCommand,
                                         context.wrapInCommand))
    }


    /**
     * Wrap in container call, and maybe `sg` call to change the group.
     *
     * The resulting wrapping will be a combination of commands wrapped into each other.
     *
     *   bsub ... sg ... apptainer ... wrapInScript.sh
     *
     * Only few parameters like PARAMETER_FILE, LSB_JOBID, LSB_JOBNAME, etc. need to be forwarded
     * into the apptainer/singularity container. The wrapper then sets up the environment with from
     * the baseEnvironmentScript, jobEnvironmentScript, and PARAMETER_FILE and runs the wrapped script.
     *
     * Note that the working directory is set to the output directory, which is different from the
     * non-wrapped situation, where it is (and as of now) remains the $HOME directory. The reasons are
     *
     *    (1) usually $HOME won't be in the container and
     *    (2) what is the sense of using $HOME anyway? For better isolation we don't want to mess with home.
     **/
    private @NotNull ToolCommand wrappedInContainerCommand(@NotNull ToolCommand toolCommand,
                                                           @NotNull CommandReferenceI command) {
        ApptainerCommandBuilder builder = ApptainerCommandBuilder
                .create()
                .withAddedBindingSpecs(context.userContainerMounts + context.roddyMounts)
                .withAddedEngineArgs(context.userContainerEngineArguments)
                .withApptainerExecutable(context.containerEnginePath)
                .withWorkingDir(context.outputDirectory.toPath())
                .withCopiedEnvironmentVariables(context.roddyContainerCopyVariables)

        Optional<Command> groupChangeCommand =
                Optional.ofNullable(context.outputGroupString).map { String outputFileGroup ->
                    new Command(new Executable(Paths.get("sg")), [outputFileGroup, "-c"])
                }

        // We have to prevent interference with the group-change code in different versions of the wrapInScript.sh.
        // If the outputFileGroup is changed, we need to change the group to the target group **outside** of
        // apptainer/singularity (no `sg` in the container), **and** we have to set a flag on the apptainer/
        // singularity command that indicates that the group-change was done. This depends a bit on the
        // version of the default plugin. New versions only try to change the group, if the outputFileGroup is
        // actually different from the current primary group. Older versions, however, (e.g., 1.2.2-5) us a flag
        // sgWasCalled that needs to be set to true.
        // For the apptainer/singularity use case we always change the group outside, so we just deactivate the
        // group-change code in the wrapInScrip.sh
        builder = builder.withAddedEnvironmentVariables(["sgWasCalled": "true"])

        groupChangeCommand.map { Command sg ->
            new ToolCommand(
                toolCommand.toolId,    // keep the toolId
                sg.cliAppend(          // wrap in change of primary group
                        builder.build(context.containerImage).   // call command in container context, ...
                            cliAppend(command, false, true), // ... and quote for apptainer
                        false, true),       // ... and quote for `sg $group -c`
                toolCommand.localPath)
        }.orElse(new ToolCommand(
                toolCommand.toolId,    // keep the toolId
                builder.build(context.containerImage).   // call command in container context, ...
                        cliAppend(command, false, true), // ... and quote for apptainer
                toolCommand.localPath))
    }

    /** We currently only support command calls as wrapped in wrapInScrip.sh. This is constructed here. */
    private @NotNull ToolCommand scriptWrappedCommand(@NotNull ToolCommand toolCommand,
                                                      @NotNull CommandReferenceI command) {
        Preconditions.checkArgument(toolCommand != null)
        Preconditions.checkArgument(command != null)
        if ([JobExecutionEnvironment.apptainer, JobExecutionEnvironment.singularity].
                contains(context.jobExecutionEnvironment)) {
            wrappedInContainerCommand(toolCommand, command)
        } else {
            // Here we need to use the wrapInScript.sh directly. It also handles the group change, such that no `sg`
            // call is necessary, here. We return the wrapInScript.sh directly, but leave the toolId as is.
            // The localPath is not used, because the wrapInScript.sh is not uploaded to the remote site, so we could
            // also put it to some invalid value, but (for now) we just keep it.
            new ToolCommand(
                    toolCommand.toolId,    // keep the toolId
                    command,               // this is actually the command for the wrapInScript.sh
                    toolCommand.localPath)
        }
    }

    /** This situation should not occur. Currently, Roddy and BE only executed tool scripts, identified by
     *  path or toolID, but it is not possible to run commands with parameters. */
    private Optional<ToolCommand> getEffectiveToolCommandImpl(@NotNull ToolCommand toolCommand,
                                                              @NotNull Command command) {
        throw new UnsupportedOperationException("Cannot execute commands with parameters via wrapper script.")
    }

}
