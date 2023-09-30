package de.dkfz.roddy.execution.jobs

import de.dkfz.roddy.Constants
import groovy.transform.CompileStatic

import javax.annotation.Nonnull


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

    ToolId(@Nonnull String id) {
        assert id != null
        this.id = id
    }

    @Override
    String getIdString() {
        id
    }

}
