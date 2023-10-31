package de.dkfz.roddy.execution.jobs

import com.google.common.base.Preconditions
import de.dkfz.roddy.execution.CommandI
import org.jetbrains.annotations.NotNull

import java.nio.file.Path

abstract class AnyToolCommand {

    private String toolId

    AnyToolCommand(@NotNull String toolId) {
        Preconditions.checkArgument(toolId != null)
        this.toolId = toolId
    }

    @NotNull String getToolId() {
        toolId
    }

    abstract Optional<CommandI> getCommand()

}

/** A ToolCommand is similar to a CommandI, but has a toolId.
 */
class ToolCommand extends AnyToolCommand {

    private CommandI command

    private Path localPath

    ToolCommand(@NotNull String toolId,
                @NotNull CommandI command,
                @NotNull Path localPath) {
        super(toolId)
        this.command = command
        this.localPath = localPath
    }

    Path getLocalPath() {
        localPath
    }

    /** We don't want to have instanceof or dynamic dispatch in using classes, but we still want type safety.
     *  Therefore, let's allow for Optional as return type.
     * @return
     */
    Optional<CommandI> getCommand() {
        Optional.of(command)
    }

}


/** If the information about formerly run jobs was read from an execution store, but the corresponding tool
 *  could not be found in the configuration, an UnknownCommand marks this fact.
 */
class UnknownToolCommand extends AnyToolCommand {

    UnknownToolCommand(@NotNull String toolId) {
        super(toolId)
    }

    Optional<CommandI> getCommand() {
        Optional.empty()
    }

}
