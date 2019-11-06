/*
 * Copyright (c) 2019 German Cancer Research Center (DKFZ).
 *
 * Distributed under the MIT License (https://opensource.org/licenses/MIT).
 */
package de.dkfz.roddy.tools

import groovy.transform.CompileStatic

@CompileStatic
class EnumHelper {

    private static final LoggerWrapper logger = LoggerWrapper.getLogger(EnumHelper.simpleName)

    static <T> Optional<T> castFromString(String name) {
        T mode
        try {
            mode = name as T
        } catch (Exception e) {
            logger.warning("Caught exception during ${mode.class.simpleName} enum cast of '${name}': " + e.message)
            return Optional.empty()
        }
        return Optional.of(mode)
    }


}
