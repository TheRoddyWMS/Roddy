/*
 * Copyright (c) 2017 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config

import com.google.common.base.Preconditions
import groovy.transform.CompileStatic

import javax.annotation.Nonnull
import javax.annotation.Nullable

/**
 */
@CompileStatic
class ConfigurationError extends Exception {

    @Nullable Configuration configuration
    @Nullable String id
    @Nonnull String description
    @Nullable Exception exception

    ConfigurationError(@Nonnull String description,
                       @Nullable Configuration configuration,
                       @Nullable String id,
                       @Nullable Exception exception) {
        super(description, exception)
        Preconditions.checkNotNull(description, "Description must not be null")
        this.description = description
        this.configuration = configuration
        this.id = id
        this.exception = exception
    }

    ConfigurationError(String description,
                       Configuration configuration,
                       Exception ex = null) {
        this(description, configuration, null, ex)
    }

    ConfigurationError(String description,
                       String id = null,
                       Exception ex = null) {
        this(description, null, id, ex)
    }
}
