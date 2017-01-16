/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.knowledge.files.BaseFile;
import de.dkfz.roddy.knowledge.files.FileGroup;
import de.dkfz.roddy.tools.BufferValue;
import de.dkfz.roddy.tools.RoddyConversionHelperMethods;
import de.dkfz.roddy.tools.TimeUnit;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import static de.dkfz.roddy.Constants.NO_VALUE;

/**
 * @author michael
 */
public class ToolEntry implements RecursiveOverridableMapContainer.Identifiable {

    /**
     * A resource set describes a list of resource values which are used by a script.
     * The active set is defined in a project configuration with m as the default.
     * The resources don't neccessarily have to be set, all values are objects and not primitives.
     * There are also max values defined but not used or filled yet.
     */
    public static class ResourceSet {
        private final String queue;
        private ResourceSetSize size;
        /**
         * The target memory value.
         */
        private BufferValue mem;
        private BufferValue memMax;

        private Integer cores;
        private Integer coresMax;
        private Integer nodes;
        private Integer nodesMax;
        private TimeUnit walltime;

        /**
         * Hard disk storage used.
         */
        private BufferValue storage;
        private BufferValue storageMax;
        private String additionalNodeFlag;

        public ResourceSet(ResourceSetSize size, BufferValue mem, Integer cores, Integer nodes, TimeUnit walltime, BufferValue storage, String queue, String additionalNodeFlag) {
            this.size = size;
            this.mem = mem;
            this.cores = cores;
            this.nodes = nodes;
            this.walltime = walltime;
            this.storage = storage;
            this.queue = queue;
            this.additionalNodeFlag = additionalNodeFlag;
        }

        public ResourceSetSize getSize() {
            return size;
        }

        public ResourceSet clone() {
            return new ResourceSet(size, mem, cores, nodes, walltime, storage, queue, additionalNodeFlag);
        }

        public BufferValue getMem() {
            return mem;
        }

        public Integer getCores() {
            return cores;
        }

        public Integer getNodes() {
            return nodes;
        }

        public BufferValue getStorage() {
            return storage;
        }

        public boolean isMemSet() {
            return mem != null;
        }

        public boolean isCoresSet() {
            return cores != null;
        }

        public boolean isNodesSet() {
            return nodes != null;
        }

        public boolean isStorageSet() {
            return storage != null;
        }

        public TimeUnit getWalltime() {
            return walltime;
        }

        public boolean isWalltimeSet() {
            return walltime != null;
        }

        public boolean isQueueSet() {
            return !RoddyConversionHelperMethods.isNullOrEmpty(queue);
        }

        public String getQueue() {
            return queue;
        }

        public boolean isAdditionalNodeFlagSet() {
            return !RoddyConversionHelperMethods.isNullOrEmpty(additionalNodeFlag);
        }

        public String getAdditionalNodeFlag() {
            return additionalNodeFlag;
        }
    }

    /**
     * A constraint for a parameter.
     * If the constraints checkMethod fails onFailMethod is called on the
     */
    public static class ToolConstraint {
        public final Method onFailMethod;
        public final Method checkMethod;

        private ToolConstraint(Method onFailMethod, Method checkMethod) {
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
    }

    public static abstract class ToolParameter<T extends ToolParameter> {
        public final String scriptParameterName;

        public ToolParameter(String scriptParameterName) {
            this.scriptParameterName = scriptParameterName;
        }

        public abstract T clone();
    }

    public static class ToolStringParameter extends ToolParameter<ToolStringParameter> {

        private final String cValueID;
        private final ParameterSetbyOptions setby;

        /**
         * How this parameter is filled.
         * Can be set by the calling code, it must really be done there!
         * Can also be set by a configuration value, this is done during the call.
         */
        public enum ParameterSetbyOptions {
            callingCode,
            configurationValue
        }

        /**
         * Call this, if the value is set in code.
         *
         * @param scriptParameterName
         */
        public ToolStringParameter(String scriptParameterName) {
            super(scriptParameterName);
            cValueID = null;
            setby = ParameterSetbyOptions.callingCode;
        }

        /**
         * Call this, if you have the id of a configuration value.
         *
         * @param scriptParameterName
         * @param cValueID
         */
        public ToolStringParameter(String scriptParameterName, String cValueID) {
            super(scriptParameterName);
            this.cValueID = cValueID;
            setby = ParameterSetbyOptions.configurationValue;
        }

        private ToolStringParameter(String scriptParameterName, String cValueID, ParameterSetbyOptions setby) {
            super(scriptParameterName);
            this.cValueID = cValueID;
            this.setby = setby;
        }

        @Override
        public ToolStringParameter clone() {
            return new ToolStringParameter(scriptParameterName, cValueID, setby);
        }
    }

    /**
     * The class defines a state for the check tag of tool output parameters
     * check can hold:
     *  true                   - The value will be checked on rerun
     *  false                  - The value will NOT be checked
     *  conditionial:somevalue - The check is true or false depending on the somevalue named variable
     */
    public static class ToolFileParameterCheckCondition {

        private String condition;

        private Boolean booleanValue;

        public ToolFileParameterCheckCondition(boolean value) {
            this.booleanValue = Boolean.valueOf(value);
        }

        public ToolFileParameterCheckCondition(String condition) {
            if(condition.startsWith("conditional:"))
                this.condition = condition.substring(12);
            else {
                this.booleanValue = RoddyConversionHelperMethods.toBoolean(condition, true);
            }
        }

        public boolean isBoolean() {
            return booleanValue != null;
        }

        public String getCondition() {
            return condition;
        }

        public boolean evaluate(ExecutionContext context) {
            if (booleanValue != null)
                return booleanValue;
            return context.getConfiguration().getConfigurationValues().getBoolean(condition, true);
        }
    }

    /**
     * Parameters for generic tools (tools which are not programmatically set!).
     * Parameters can be for in and for output.
     * Parameters can have constraints.
     */
    public static class ToolFileParameter extends ToolParameter<ToolFileParameter> {
        public final Class<BaseFile> fileClass;
        public final List<ToolConstraint> constraints;
        public final String scriptParameterName;
        public final String filenamePatternSelectionTag;
        public final ToolFileParameterCheckCondition checkFile;
        public final String parentVariable;
        private List<ToolFileParameter> childFiles;

        public ToolFileParameter(Class<BaseFile> fileClass, List<ToolConstraint> constraints, String scriptParameterName, ToolFileParameterCheckCondition checkFile) {
            this(fileClass, constraints, scriptParameterName, checkFile, FilenamePattern.DEFAULT_SELECTION_TAG, null, null);
        }

        public ToolFileParameter(Class<BaseFile> fileClass, List<ToolConstraint> constraints, String scriptParameterName, ToolFileParameterCheckCondition checkFile, String filenamePatternSelectionTag, List<ToolFileParameter> childFiles, String parentVariable) {
            super(scriptParameterName);
            this.fileClass = fileClass;
            this.constraints = constraints != null ? constraints : new LinkedList<>();
            this.scriptParameterName = scriptParameterName;
            this.checkFile = checkFile;
            this.filenamePatternSelectionTag = filenamePatternSelectionTag;
            this.childFiles = childFiles;
            if (this.childFiles == null) this.childFiles = new LinkedList<>();
            this.parentVariable = parentVariable;
        }

        @Override
        public ToolFileParameter clone() {
            List<ToolConstraint> _con = new LinkedList<ToolConstraint>();
            for (ToolConstraint tc : constraints) {
                _con.add(tc.clone());
            }
            return new ToolFileParameter(fileClass, _con, scriptParameterName, checkFile, filenamePatternSelectionTag, childFiles, parentVariable);
        }

        public List<ToolFileParameter> getChildFiles() {
            return childFiles;
        }

        public boolean hasSelectionTag() {
            return !filenamePatternSelectionTag.equals(FilenamePattern.DEFAULT_SELECTION_TAG);
        }
    }

    /**
     * This class is supposed to contain output objects from a method call.
     * You can create tuples of different sizes containt file groups and files.
     */
    public static class ToolTupleParameter extends ToolParameter<ToolTupleParameter> {
        public final List<ToolFileParameter> files;

        public ToolTupleParameter(List<ToolFileParameter> files) {
            super(NO_VALUE); //Tuples are not passed
            this.files = files;
        }

        @Override
        public ToolTupleParameter clone() {
            List<ToolFileParameter> _files = new LinkedList<>();
            for (ToolFileParameter tf : files) _files.add(tf.clone());
            return new ToolTupleParameter(_files);
        }
    }

    /**
     * Parameters for generic tools (tools which are not programmatically set!).
     * Parameters can be for in and for output.
     * Parameters can have constraints.
     */
    public static class ToolFileGroupParameter extends ToolParameter<ToolFileGroupParameter> {
        public final Class<FileGroup> groupClass;
        public final Class<BaseFile> genericFileClass;
        public final List<ToolFileParameter> files;
        public final PassOptions passOptions;

        public enum PassOptions {
            parameters,
            array
        }

        public ToolFileGroupParameter(Class<FileGroup> groupClass, Class<BaseFile> genericFileClass, List<ToolFileParameter> files, String scriptParameterName, PassOptions passas) {
            super(scriptParameterName);
            this.groupClass = groupClass;
            this.files = files;
            this.passOptions = passas;
            this.genericFileClass = genericFileClass;
        }

        @Override
        public ToolFileGroupParameter clone() {
            List<ToolFileParameter> _files = new LinkedList<ToolFileParameter>();
            for (ToolFileParameter tf : files) {
                _files.add(tf.clone());
            }
            return new ToolFileGroupParameter(groupClass, genericFileClass, _files, scriptParameterName, passOptions);
        }

        public boolean isGeneric() {
            return genericFileClass != null;
        }

        public String getGenericClassString() {
            if (isGeneric()) {
                return groupClass.getName() + "<" + genericFileClass.getName() + ">";
            } else {
                return groupClass.getName();
            }
        }
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

//    public List<ToolParameter> getOutputParameters(ExecutionContext context) {
//        return getOutputParameters(context.getConfiguration());
//    }

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