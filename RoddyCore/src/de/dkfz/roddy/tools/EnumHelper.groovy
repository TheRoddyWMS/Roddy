/*
 * Copyright (c) 2019 German Cancer Research Center (DKFZ).
 *
 * Distributed under the MIT License (https://opensource.org/licenses/MIT).
 */
package de.dkfz.roddy.tools

import groovy.transform.CompileStatic

@CompileStatic
class EnumHelper {

    static <T> Optional<T> castFromString(String name) {
        T mode
        try {
            mode = name as T
        } catch (Exception e) {
            return Optional.empty()
        }
        return Optional.of(mode)
    }


}
