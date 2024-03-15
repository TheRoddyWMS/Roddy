package de.dkfz.roddy.core

import groovy.transform.CompileStatic

import java.nio.file.Path
import java.nio.file.Paths


@CompileStatic
enum JobExecutionEnvironment {
    bash,
    singularity,
    apptainer;

    static JobExecutionEnvironment from(String value) {
        value.toLowerCase() as JobExecutionEnvironment
    }

    Path toPath() {
        Paths.get(this.toString())
    }
}
