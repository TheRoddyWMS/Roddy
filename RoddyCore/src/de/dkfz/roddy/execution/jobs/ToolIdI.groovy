package de.dkfz.roddy.execution.jobs

import com.google.common.base.Preconditions
import de.dkfz.roddy.Constants
import groovy.transform.CompileStatic
import org.jetbrains.annotations.NotNull


@CompileStatic
interface ToolIdI {

    String getIdString()

}


@CompileStatic
class UnknownToolId implements ToolIdI {

    UnknownToolId() {}

    @Override
    String getIdString() {
        Constants.UNKNOWN
    }

}


@CompileStatic
class ToolId implements ToolIdI {

    private String id

    ToolId(@NotNull String id) {
        Preconditions.checkArgument(id != null)
        this.id = id
    }

    @Override
    String getIdString() {
        id
    }

}
