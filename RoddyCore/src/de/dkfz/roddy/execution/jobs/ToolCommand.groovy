package de.dkfz.roddy.execution.jobs

import com.google.common.base.Preconditions
import de.dkfz.roddy.execution.CommandI
import org.jetbrains.annotations.NotNull

import javax.annotation.Nullable
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
}

/** A ToolCommand is similar to a CommandI, but has a `toolId` and a `localPath`.
 *
 *  The toolId is used to track the actual tool-command name. Roddy will use the toolId to lift the path to the tool
 *  from the configurations, and invoke the tool with this path via the wrapInScript.sh. The command itself,
 *  can be more complex, but obviously should be related to the toolId.
 *
 *  The `localPath` is needed if the script with the code for the command is to be uploaded to a remote site.
 *  The target path will be taken from the executable path in the CommandI object.
 */
class ToolCommand extends AnyToolCommand {

    private CommandI command

    private Path localPath

    ToolCommand(@NotNull String toolId,
                @NotNull CommandI command,
                @NotNull Path localPath) {
        super(toolId)
        Preconditions.checkArgument(command != null)
        this.command = command
        Preconditions.checkArgument(localPath != null)
        this.localPath = localPath
    }

    Path getLocalPath() {
        localPath
    }

    CommandI getCommand() {
        command
    }

}

/** If the information about formerly run jobs was read from an execution store, but the corresponding tool
 *  could not be found in the configuration, an ToolIdCommand marks this fact.
 *
 *  The object might be cast into a normal ToolCommand, using an execution context that contains
 *  these information with `context.getToolCommand(this.toolId)`. Or not ...
 */
class ToolIdCommand extends AnyToolCommand {

    @Nullable final private String md5

    ToolIdCommand(@NotNull String toolId,
                  @Nullable String md5 = null) {
        super(toolId)
        Preconditions.checkArgument(md5 == null || md5.length() == 32 && !md5.toLowerCase().any {
            !"0123457689abcdef".contains(it)
        }, "Not a valid MD5: '$md5'")
        this.md5 = md5
    }

    Optional<String> getMd5() {
        Optional.ofNullable(md5)
    }

}

/** This is used an Null object. It tracks the id of the requested tool. It should NOT be just used as a tool
 *  for which no MD5 is known. For that, the MD5 in ToolIdCommand is optional.
 */
class UnknownToolCommand extends AnyToolCommand {

    UnknownToolCommand(@NotNull toolId) {
        super(toolId)
    }

}
