/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.knowledge.nativeworkflows

import de.dkfz.eilslabs.batcheuphoria.config.ResourceSet
import de.dkfz.eilslabs.batcheuphoria.config.ResourceSetSize
import de.dkfz.roddy.config.ToolEntry
import de.dkfz.roddy.execution.jobs.Job
import de.dkfz.roddy.knowledge.files.BaseFile
import de.dkfz.roddy.tools.BufferUnit
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.eilslabs.batcheuphoria.jobs.GenericJobInfo as BEGenJI
import de.dkfz.roddy.tools.BufferValue
import de.dkfz.roddy.tools.TimeUnit
import groovy.transform.CompileStatic

import java.util.List
import java.util.Map

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

    GenericJobInfo(ExecutionContext context, BEGenJI jInfo) {
        executionContext = context
        jobName = jInfo.jobName
        id = jInfo.id
        parameters = jInfo.parameters
        parentJobIDs = jInfo.parentJobIDs
        walltime = jInfo.walltime
        memory = jInfo.maxMemory
        memoryBufferUnit = jInfo.memoryBufferUnit
        cpus = jInfo.maxCpus
        nodes = jInfo.maxNodes
        queue = jInfo.queue
        otherSettings = jInfo.otherSettings

        toolEntry = context.configuration.tools.allValuesAsList.find { ToolEntry te -> new File(te.localPath).name == jInfo.tool.name }
        toolID = toolEntry.id

        if(!toolEntry.resourceSets) {
            toolEntry.resourceSets << new ResourceSet(ResourceSetSize.l, new BufferValue(memory, memoryBufferUnit), cpus, nodes, walltime, null, queue, otherSettings);
        }
    }

//    GenericJobInfo(ExecutionContext executionContext, String jobName, String toolID, String id, Map<String, String> parameters, List<String> parentJobIDs) {
//
//        this.executionContext = executionContext
//        this.jobName = jobName
//        this.toolID = toolID
//        this.id = id
//        this.parameters = parameters
//        this.parentJobIDs = parentJobIDs
//    }

    ExecutionContext getExecutionContext() {
        return executionContext
    }

    Job toJob() {
        return new Job(executionContext, jobName, toolID, parameters as Map<String, Object>, null, new LinkedList<BaseFile>())
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
