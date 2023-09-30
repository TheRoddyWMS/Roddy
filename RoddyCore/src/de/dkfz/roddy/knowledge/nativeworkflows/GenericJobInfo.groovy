/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.nativeworkflows

import de.dkfz.roddy.config.ResourceSet
import de.dkfz.roddy.config.ResourceSetSize
import de.dkfz.roddy.config.ToolEntry
import de.dkfz.roddy.execution.Code
import de.dkfz.roddy.execution.CommandI
import de.dkfz.roddy.execution.Executable
import de.dkfz.roddy.execution.jobs.Job
import de.dkfz.roddy.execution.jobs.ToolCommand
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.tools.BufferUnit
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.execution.jobs.GenericJobInfo as BEGenJI
import de.dkfz.roddy.tools.BufferValue
import de.dkfz.roddy.tools.TimeUnit
import groovy.transform.CompileStatic

import java.nio.file.Paths

/**
 * Created by michael on 06.02.15.
 */
@CompileStatic
class GenericJobInfo {

    final ExecutionContext executionContext
    final String jobName
    final String toolID
    final ToolEntry toolEntry
    final String id
    final Map<String, String> parameters
    final List<String> parentJobIDs
    final TimeUnit walltime
    final Integer memory
    final BufferUnit memoryBufferUnit
    final Integer cpus
    final Integer nodes
    final String queue
    final String otherSettings
    final String inlineScript

    GenericJobInfo(ExecutionContext context, BEGenJI jInfo, String inlineScript = null, BufferUnit bufferUnit = BufferUnit.G) {
        executionContext = context
        jobName = jInfo.jobName
        id = jInfo.jobID.toString()
        parameters = jInfo.parameters
        parentJobIDs = jInfo.parentJobIDs
        walltime = TimeUnit.fromDuration(jInfo.usedResources.walltime)
        assert(jInfo.usedResources.mem.toLong(bufferUnit) <= Integer.MAX_VALUE)
        memory = jInfo.usedResources.mem.toLong(bufferUnit) as Integer
        memoryBufferUnit = bufferUnit
        cpus = jInfo.usedResources.cores
        nodes = jInfo.usedResources.nodes
        queue = jInfo.usedResources.queue
        otherSettings = jInfo.otherSettings

        if (!inlineScript) {
            toolEntry = context.configuration.tools.allValuesAsList.find { ToolEntry te -> new File(te.localPath).name == jInfo.tool.name }
            toolID = toolEntry.id
            if (!toolEntry.resourceSets) {
                toolEntry.resourceSets << new ResourceSet(ResourceSetSize.l, new BufferValue(memory, memoryBufferUnit), cpus, nodes, walltime, null, queue, otherSettings);
            }
        } else {
            this.inlineScript = inlineScript
        }
    }

    ExecutionContext getExecutionContext() {
        return executionContext
    }

    Job toJob() {
        ToolCommand command
        if (inlineScript == null) {
            command = executionContext.getToolCommand(toolID)
        } else {
            command = new ToolCommand(toolID,
                                      new Code(inlineScript),
                                      executionContext.configuration.getSourceToolPath(toolID).toPath())
        }
        return new Job(
                executionContext,
                jobName,
                command,
                parameters as Map<String, Object>,
                new LinkedList<BaseFile>(),
                new LinkedList<BaseFile>())
    }

    @Override
    String toString() {
        return "GenericJobInfo{" +
                "executionContext=" + executionContext +
                ", jobName='" + jobName + '\'' +
                ", toolID='" + toolID + '\'' +
                ", id='" + id + '\'' +
                ", parameters=" + parameters +
                ", parentJobIDs=" + parentJobIDs +
                '}'
    }

}
