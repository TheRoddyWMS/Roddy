/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.knowledge.files.BaseFile;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * @author michael
 */
public class ToolEntry implements RecursiveOverridableMapContainer.Identifiable {

    /**
     * A constraint for a parameter.
     * If the constraints checkMethod fails onFailMethod is called on the
     */
    public static class ToolConstraint {
        public final Method onFailMethod;
        public final Method checkMethod;

        public ToolConstraint(Method onFailMethod, Method checkMethod) {
            this.onFailMethod = onFailMethod;
            this.checkMethod = checkMethod;
        }

        public void apply(BaseFile p) {
            try {
                boolean valid = (Boolean) checkMethod.invoke(p);
                if (!valid)
                    onFailMethod.invoke(p);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public ToolConstraint clone() {
            return new ToolConstraint(onFailMethod, checkMethod);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ToolConstraint that = (ToolConstraint) o;

            if (onFailMethod != null ? !onFailMethod.equals(that.onFailMethod) : that.onFailMethod != null) return false;
            return checkMethod != null ? checkMethod.equals(that.checkMethod) : that.checkMethod == null;
        }
    }

    public static abstract class ToolParameter<T extends ToolParameter> {
        public final String scriptParameterName;

        public ToolParameter(String scriptParameterName) {
            this.scriptParameterName = scriptParameterName;
        }

        public abstract T clone();
    }

    public final String id;
    public final String basePathId;
    public final String path;
    public final String computationResourcesFlags;
    private String inlineScript;
    private String inlineScriptName;
    private final List<ToolParameter> inputParameters = new LinkedList<>();
    private final List<ToolParameter> outputParameters = new LinkedList<>();
    private final List<ResourceSet> resourceSets = new LinkedList<>();
    private boolean overridesResourceSets;

    public ToolEntry(String id, String basePathId, String path) {
        this.id = id;
        this.basePathId = basePathId;
        this.path = path;
        this.computationResourcesFlags = "";
    }

    public ToolEntry(String id, String basePathId, String path, String computationResourcesFlags) {
        this.id = id;
        this.basePathId = basePathId;
        this.path = path;
        this.computationResourcesFlags = computationResourcesFlags != null ? computationResourcesFlags : "";
    }

    public boolean isToolGeneric() {
        return overridesResourceSets || (inputParameters.size() > 0 || outputParameters.size() > 0);
    }

    public void setGenericOptions(List<ToolParameter> input, List<ToolParameter> output, List<ResourceSet> resourceSets) {
        inputParameters.addAll(input);
        outputParameters.addAll(output);
        this.resourceSets.addAll(resourceSets);
    }

    public ToolEntry clone() {
        List<ToolParameter> _inp = new LinkedList<ToolParameter>();
        List<ToolParameter> _outp = new LinkedList<ToolParameter>();
        List<ResourceSet> _rsets = new LinkedList<>();
        for (ToolParameter tp : inputParameters) {
            _inp.add(tp.clone());
        }
        for (ToolParameter tp : outputParameters) {
            _outp.add(tp.clone());
        }
        for (ResourceSet rs : resourceSets) {
            _rsets.add(rs.clone());
        }
        ToolEntry te = new ToolEntry(id, basePathId, path, computationResourcesFlags);
        te.setGenericOptions(_inp, _outp, _rsets);
        if (overridesResourceSets) te.setOverridesResourceSets();
        return te;
    }

    public String getLocalPath() {
        return path;
    }

    public void setInlineScript(String script) {
        this.inlineScript = script;
    }

    public String getInlineScript() {
        return this.inlineScript;
    }

    public void setInlineScriptName(String scriptName) {
        this.inlineScriptName = scriptName;
    }

    public String getInlineScriptName() {
        return this.inlineScriptName;
    }

    public boolean hasInlineScript() {
        if (inlineScript != null)
            return true;
        else
            return false;
    }

    public boolean hasResourceSets() {
        return resourceSets.size() > 0;
    }

    public ResourceSet getResourceSet(Configuration configuration) {
        ResourceSetSize key = configuration.getResourcesSize();
        int size = key.ordinal();

        ResourceSet first = resourceSets.get(0);
        if (resourceSets.size() == 1) { // Only one set exists.
            return resourceSets.get(0);
        }

        ResourceSet last = resourceSets.get(resourceSets.size() - 1);
        if (size <= first.getSize().ordinal()) {  // The given key is smaller than the available keys. Return the first set.
            return first;
        }
        if (size >= last.getSize().ordinal()) {  // The given key is larger than the available keys. Return the last set.
            return last;
        }
        for (ResourceSet resourceSet : resourceSets) {  // Select the appropriate set
            if (resourceSet.getSize() == key)
                return resourceSet;
        }
        //Still no set, take the largest set, which comes after the given ordinal.
        for (ResourceSet resourceSet : resourceSets) {
            if (resourceSet.getSize().ordinal() > size)
                return resourceSet;
        }

        return null;
    }

    public void setOverridesResourceSets() {
        overridesResourceSets = true;
    }

    public boolean doesOverrideResourceSets() {
        return overridesResourceSets;
    }

    public List<ToolParameter> getInputParameters(ExecutionContext context) {
        return getInputParameters(context.getConfiguration());
    }

    public List<ToolParameter> getInputParameters(Configuration configuration) {
        if (overridesResourceSets) {
            List<ToolEntry> containerParents = configuration.getTools().getInheritanceList(this.id);
            if (containerParents.size() == 1)
                return inputParameters;
            for (int i = containerParents.size() - 2; i >= 0; i--) {
                if (!containerParents.get(i).overridesResourceSets)
                    return containerParents.get(i).inputParameters;
            }
        }
        return inputParameters;
    }

    public List<ToolParameter> getOutputParameters(Configuration configuration) {
        if (overridesResourceSets) {
            List<ToolEntry> containerParents = configuration.getTools().getInheritanceList(this.id);
            if (containerParents.size() == 1)
                return outputParameters;
            for (int i = containerParents.size() - 2; i >= 0; i--) {
                if (!containerParents.get(i).overridesResourceSets)
                    return containerParents.get(i).outputParameters;
            }
        }
        return outputParameters;
    }

    public List<ResourceSet> getResourceSets() {
        return resourceSets;
    }

    @Override
    public String getID() {
        return id;
    }
}