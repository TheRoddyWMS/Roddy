package de.dkfz.roddy.execution.jobs

import de.dkfz.roddy.execution.CommandI

import javax.annotation.Nonnull
import java.nio.file.Path

abstract class AnyToolCommand {

    private String toolId

    AnyToolCommand(@Nonnull String toolId) {
        assert toolId != null
        this.toolId = toolId
    }

    @Nonnull String getToolId() {
        toolId
    }

    @Nonnull Optional<CommandI> getCommand() {
        Optional.empty()
    }

}

/** A ToolCommand is similar to a CommandI, but has a toolId.
 */
class ToolCommand extends AnyToolCommand {

    private CommandI command

    private Path localPath

    ToolCommand(@Nonnull String toolId,
                @Nonnull CommandI command,
                @Nonnull Path localPath) {
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

    UnknownToolCommand(@Nonnull String toolId) {
        super(toolId)
    }

}
