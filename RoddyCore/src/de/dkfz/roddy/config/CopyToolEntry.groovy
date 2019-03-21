/*
 * Copyright (c) 2019 German Cancer Research Center (DKFZ).
 *
 * Distributed under the MIT License (https://opensource.org/licenses/MIT).
 */
package de.dkfz.roddy.config

class CopyToolEntry extends BaseToolEntry {

    final String copyOfName

    CopyToolEntry(String name, String copyOfName) {
        this.id = name
        this.copyOfName = copyOfName
    }

}
