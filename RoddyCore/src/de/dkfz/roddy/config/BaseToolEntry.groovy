/*
 * Copyright (c) 2019 German Cancer Research Center (DKFZ).
 *
 * Distributed under the MIT License (https://opensource.org/licenses/MIT).
 */
package de.dkfz.roddy.config

import groovy.transform.CompileStatic

@CompileStatic
abstract class BaseToolEntry implements RecursiveOverridableMapContainer.Identifiable {

    protected String id

    @Override
    String getID() {
        id
    }

    String getId() {
        id
    }

}
