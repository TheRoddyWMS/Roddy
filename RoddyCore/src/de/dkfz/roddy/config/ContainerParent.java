package de.dkfz.roddy.config;

import java.util.List;

/**
*/
public interface ContainerParent<P extends ContainerParent> {
    public List<P> getContainerParents();

    public RecursiveOverridableMapContainer getContainer(String id);

    public String getID();
}
