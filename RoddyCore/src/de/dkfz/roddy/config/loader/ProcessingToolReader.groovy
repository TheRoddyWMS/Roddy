/*
 * Copyright (c) 2017 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.loader

import de.dkfz.eilslabs.batcheuphoria.config.ResourceSet
import de.dkfz.eilslabs.batcheuphoria.config.ResourceSetSize
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.FilenamePattern
import de.dkfz.roddy.config.ToolEntry
import de.dkfz.roddy.config.ToolFileGroupParameter
import de.dkfz.roddy.config.ToolFileParameter
import de.dkfz.roddy.config.ToolFileParameterCheckCondition
import de.dkfz.roddy.config.ToolStringParameter
import de.dkfz.roddy.config.ToolTupleParameter
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.knowledge.files.FileGroup
import de.dkfz.roddy.knowledge.files.FileObjectTupleFactory
import de.dkfz.roddy.knowledge.files.GenericFileGroup
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.tools.BufferValue
import de.dkfz.roddy.tools.LoggerWrapper
import de.dkfz.roddy.tools.TimeUnit
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.slurpersupport.NodeChild
import groovy.util.slurpersupport.NodeChildren

import static de.dkfz.roddy.config.loader.ConfigurationFactory.extractAttributeText

/**
 * Class to load a processing tool entry from a node slurper
 * Created by heinold on 02.05.17.
 */
@CompileStatic
class ProcessingToolReader {

    static LoggerWrapper logger = LoggerWrapper.getLogger(de.dkfz.roddy.config.loader.ProcessingToolReader)

    NodeChild tool

    Configuration config

    List<String> loadErrors = []

    ProcessingToolReader(NodeChild tool, Configuration config) {
        this.tool = tool
        this.config = config
    }

    boolean hasErrors() {
        return loadErrors // Auto boolean to true or false
    }

    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    static String readTag(NodeChild node, String tag) {
        return node.@"$tag".text()
    }

    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    static Collection<NodeChild> readCollection(NodeChild node, String id) {
        return node."$id" as Collection<NodeChild>
    }

    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    static Collection<NodeChild> readCollection(NodeChildren node, String id) {
        return node."$id" as Collection<NodeChild>
    }

    /**
     * Read a tool node
     * @return
     */
    ToolEntry readProcessingTool() {
        try {
            String toolID = readTag(tool, "name") //tool.@name.text()
            String path = readTag(tool, "value")
            String basePathId = readTag(tool, "basepath")
            boolean overrideresourcesets = extractAttributeText(tool, "overrideresourcesets", "false").toBoolean()
            ToolEntry currentEntry = new ToolEntry(toolID, basePathId, path)
            if (overrideresourcesets)
                currentEntry.setOverridesResourceSets()
            int noOfChildren = tool.children().size()
            if (noOfChildren > 0) {
                List<ToolEntry.ToolParameter> inputParameters = []
                List<ToolEntry.ToolParameter> outputParameters = []
                List<ResourceSet> resourceSets = new LinkedList<>()
                for (NodeChild child in readCollection(tool, "children")) {
                    String cName = child.name()

                    if (cName == "resourcesets") {
                        for (NodeChild rset in readCollection(child, "rset")) {
                            ResourceSet tempSet = parseToolResourceSet(rset, config)
                            if (tempSet)
                                resourceSets << tempSet
                        }
                    } else if (cName == "input") {
                        inputParameters << parseToolParameter(toolID, child)
                    } else if (cName == "output") {
                        outputParameters << parseToolParameter(toolID, child)
                    } else if (cName == "script") {
                        if (readTag(child, "value") != "") {
                            currentEntry.setInlineScript(child.text().trim().replaceAll('<!\\[CDATA\\[', "").replaceAll(']]>', ""))
                            currentEntry.setInlineScriptName(readTag(child, "value"))
                        }
                    }
                }
                def allparameters = (inputParameters + outputParameters).collect {
                    if (it instanceof ToolEntry.ToolParameterOfFiles) {
                        ((ToolEntry.ToolParameterOfFiles) it).allFiles.collect { it.scriptParameterName }
                    } else {
                        it.scriptParameterName
                    }
                }.flatten()

                if (allparameters.size() != allparameters.unique(false).size())
                    throw new Exception("There were duplicate i/o script parameter names.")
                currentEntry.setGenericOptions(inputParameters, outputParameters, resourceSets)
            }
            return currentEntry
        } catch (Exception ex) {
            config.addLoadError(new ConfigurationLoadError(config, "ToolEntry could not be read", ex.getMessage(), ex))
            return null
        }
    }

    ResourceSet parseToolResourceSet(NodeChild rset, Configuration config) {
        ResourceSet tempSet = null
        try {
            ResourceSetSize rsetSize = readTag(rset, "size")
            //Is it short defined or long defined?
            String valueList = extractAttributeText(rset, "values", "")
            if (!valueList) { //Must be fully specified.
                // Only parse the memory value, if it is set.
                BufferValue rsetUsedMemory
                String _rsetUsedMemory = extractAttributeText(rset, "memory", null)
                if (_rsetUsedMemory != null) rsetUsedMemory = new BufferValue(_rsetUsedMemory)

                Integer rsetUsedCores = extractAttributeText(rset, "cores", null)?.toInteger()
                Integer rsetUsedNodes = extractAttributeText(rset, "nodes", null)?.toInteger()

                TimeUnit rsetUsedWalltime
                String _rsetUsedWalltime = extractAttributeText(rset, "walltime", null)
                if (_rsetUsedWalltime != null) rsetUsedWalltime = new TimeUnit(_rsetUsedWalltime)

                String rsetUsedQueue = extractAttributeText(rset, "queue", null)
                String rsetUsedNodeFlag = extractAttributeText(rset, "nodeflag", null)
                tempSet = new ResourceSet(rsetSize, rsetUsedMemory, rsetUsedCores, rsetUsedNodes, rsetUsedWalltime, null, rsetUsedQueue, rsetUsedNodeFlag)
            }
        } catch (Exception ex) {
            if (config != null) config.addLoadError(new ConfigurationLoadError(config, "Resource set could not be read", "", ex))
        }
        return tempSet
    }

    /**
     * Load tool parameters like group, files and constraints...
     * @param child
     * @return
     */
    ToolEntry.ToolParameter parseToolParameter(String toolID, NodeChild child) {
        String type = readTag(child, "type")
        if (type == "file") { //Load a file
            return parseFile(child, toolID)
        } else if (type == "tuple") {
            return parseTuple(child, toolID)
        } else if (type == "filegroup") {
            return parseFileGroup(child, toolID)
        } else if (type == "string") {
            ToolStringParameter.ParameterSetbyOptions setby = Enum.valueOf(ToolStringParameter.ParameterSetbyOptions.class, extractAttributeText(child, "setby", ToolStringParameter.ParameterSetbyOptions.callingCode.name()))
            String pName = readTag(child, "scriptparameter")
            ToolStringParameter tsp = null
            if (setby == ToolStringParameter.ParameterSetbyOptions.callingCode) {
                tsp = new ToolStringParameter(pName)
            } else {
                tsp = new ToolStringParameter(pName, extractAttributeText(child, "cValueID"))
                //TODO Validate if cValueID == null!
            }
            return tsp
        }
    }

    ToolFileParameter parseFile(NodeChild child, String toolID) {
        String cls = readTag(child, "typeof")
        Class _cls = LibrariesFactory.getInstance().loadRealOrSyntheticClass(cls, BaseFile.class.name)

        String pName = readTag(child, "scriptparameter")
        String fnpSelTag = extractAttributeText(child, "selectiontag", extractAttributeText(child, "fnpatternselectiontag", FilenamePattern.DEFAULT_SELECTION_TAG))
        String parentFileVariable = extractAttributeText(child, "variable", null) //This is only the case for child files.
        ToolFileParameterCheckCondition check = new ToolFileParameterCheckCondition(extractAttributeText(child, "check", "true"))

        List<ToolEntry.ToolConstraint> constraints = new LinkedList<ToolEntry.ToolConstraint>()
        for (constraint in readCollection(child, "constraint")) {
            String method = readTag(constraint, "method")
            String methodonfail = readTag(constraint, "methodonfail")
            constraints << new ToolEntry.ToolConstraint(_cls.getMethod(methodonfail), _cls.getMethod(method))
        }

        // A file can have several defined child files
        List<ToolFileParameter> subParameters = new LinkedList<ToolFileParameter>()
        for (NodeChild fileChild in readCollection(child, "children")) {
            subParameters << (ToolFileParameter) parseToolParameter(toolID, fileChild)
        }
        ToolFileParameter tp = new ToolFileParameter(_cls, constraints, pName, check, fnpSelTag, subParameters, parentFileVariable)

        return tp
    }

    ToolTupleParameter parseTuple(NodeChild child, String toolID) {
        int tupleSize = child.children().size()
        if (!FileObjectTupleFactory.isValidSize(tupleSize)) {
            logger.severe("Tuple is of wrong size for tool ${toolID}.")
        }
        List<ToolFileParameter> subParameters = new LinkedList<ToolFileParameter>()
        for (NodeChild fileChild in readCollection(child, "children")) {
            subParameters << (ToolFileParameter) parseToolParameter(toolID, fileChild)
        }
        return new ToolTupleParameter(subParameters)
    }

    @Deprecated
    boolean isInputFileGroup(NodeChild groupNode) {
        return groupNode.name() == "input"
    }

    ToolFileGroupParameter parseFileGroup(NodeChild groupNode, String toolID) {
        String cls = extractAttributeText(groupNode, "typeof", GenericFileGroup.name)
        Class<FileGroup> filegroupClass = LibrariesFactory.getInstance().loadRealOrSyntheticClass(cls, FileGroup.class.name)
        if (!filegroupClass)
            filegroupClass = GenericFileGroup

        ToolFileGroupParameter.PassOptions passas = Enum.valueOf(ToolFileGroupParameter.PassOptions.class, extractAttributeText(groupNode, "passas", ToolFileGroupParameter.PassOptions.parameters.name()))
        ToolFileGroupParameter.IndexOptions indexOptions = Enum.valueOf(ToolFileGroupParameter.IndexOptions.class, extractAttributeText(groupNode, "indices", ToolFileGroupParameter.IndexOptions.numeric.name()))

        String fileclass = extractAttributeText(groupNode, "fileclass", null)
        int childCount = groupNode.children().size()
        String pName = readTag(groupNode, "scriptparameter")

        if (fileclass) {
            if (!pName)
                throw new RuntimeException("You have to set both the parametername and the fileclass attribute for filegroup i/o parameter in ${toolID}")

            Class<BaseFile> genericFileClass = LibrariesFactory.getInstance().loadRealOrSyntheticClass(fileclass, BaseFile.class.name)
            ToolFileGroupParameter tpg = new ToolFileGroupParameter(filegroupClass, genericFileClass, pName, passas, indexOptions)
            return tpg
        } else if (childCount) {
            return parseChildFilesForFileGroup(groupNode, passas, toolID, pName, filegroupClass, indexOptions)
        } else {
            if (!isInputFileGroup(groupNode)) { // TODO: Enforce fileclass attributes for output filegroup <https://eilslabs-phabricator.dkfz.de/T2015>
                throw new RuntimeException("Either the fileclass or a list of child files need to be set for a filegroup in ${toolID}")
            } else {
                return new ToolFileGroupParameter(filegroupClass, BaseFile.class, pName, passas, indexOptions)
            }
        }
    }

    @Deprecated
    ToolFileGroupParameter parseChildFilesForFileGroup(NodeChild groupNode, ToolFileGroupParameter.PassOptions passas, String toolID, String pName, Class filegroupClass, ToolFileGroupParameter.IndexOptions indexOptions) {
        int childCount = groupNode.children().size()
        List<ToolFileParameter> children = new LinkedList<ToolFileParameter>()
        if (childCount == 0 && passas != ToolFileGroupParameter.PassOptions.array)
            logger.severe("No files in the file group. Configuration is not valid.")
        for (Object fileChild in groupNode.children()) {
            children << (parseToolParameter(toolID, fileChild as NodeChild) as ToolFileParameter)
        }
        return new ToolFileGroupParameter(filegroupClass, children, pName, passas, indexOptions)
    }
}
