package de.dkfz.roddy.knowledge.brawlworkflows

import de.dkfz.roddy.config.OnScriptParameterFilenamePattern
import de.dkfz.roddy.config.ResourceSet
import de.dkfz.roddy.config.ResourceSetSize
import de.dkfz.roddy.config.ToolEntry
import de.dkfz.roddy.config.ToolFileParameter
import de.dkfz.roddy.config.ToolFileParameterCheckCondition
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.knowledge.files.FileObject
import de.dkfz.roddy.plugins.LibrariesFactory
import de.dkfz.roddy.tools.BufferValue
import de.dkfz.roddy.tools.TimeUnit
import groovy.transform.CompileStatic

@CompileStatic
class BrawlWorkflowToolCall {

    private BrawlWorkflow workflow
    private TimeUnit timeUnit
    private String script
    private int memory
    private int threads
    private String toolID
    private List<ToolFileParameter> inputParameters = []
    private List<ToolFileParameter> outputParameters = []

    BrawlWorkflowToolCall(BrawlWorkflow wf, String toolID) {
        workflow = wf
        this.toolID = toolID
    }

    ToolFileParameter getToolFileParameter(String fileclass, String parameter) {
        Class loadedFileClass = LibrariesFactory.instance.loadRealOrSyntheticClass(fileclass, BaseFile.class as Class<FileObject>)
        new ToolFileParameter(loadedFileClass as Class<BaseFile>, [], parameter, new ToolFileParameterCheckCondition(true))
    }

    void input(String fileclass, String parameter, String pattern = "") {
        getToolFileParameter(fileclass, parameter)
        // For later...
        //        new OnScriptParameterFilenamePattern(loadedFileClass, toolID, parameter, pattern)
    }

    void output(String fileclass, String parameter, String pattern) {
        def fileParameter = getToolFileParameter(fileclass, parameter)
        new OnScriptParameterFilenamePattern(fileParameter.fileClass as Class<BaseFile>, toolID, parameter, pattern)
    }

    void threads(int threads) {

        this.threads = threads
    }

    void memory(int memory) {

        this.memory = memory
    }

    void walltime(String walltime) {
        timeUnit = new TimeUnit(walltime)
    }

    void shell(String script) {
        this.script = script
    }

    ToolEntry toToolEntry() {
        ToolEntry entry = new ToolEntry(toolID, "brawlTools", "")
        entry.setInlineScript(script)
        entry.getResourceSets().add(new ResourceSet(ResourceSetSize.l, new BufferValue(memory), threads, 1, timeUnit, null, null, null))
        entry.inputParameters.addAll(inputParameters)
        entry.outputParameters.addAll(outputParameters)
        return entry
    }
}