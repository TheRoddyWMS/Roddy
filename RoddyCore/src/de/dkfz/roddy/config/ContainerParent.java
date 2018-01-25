/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config;

import java.util.List;

/**
*/
public interface ContainerParent<P extends ContainerParent> {
    public List<P> getParents();

    public RecursiveOverridableMapContainer getContainer(String id);

    public String getID();
}
