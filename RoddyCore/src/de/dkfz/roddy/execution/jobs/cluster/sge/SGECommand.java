/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.jobs.cluster.sge;

import de.dkfz.roddy.core.ExecutionContext;
import de.dkfz.roddy.execution.io.ExecutionService;
import de.dkfz.roddy.execution.jobs.Job;
import de.dkfz.roddy.execution.jobs.ProcessingCommands;
import de.dkfz.roddy.execution.jobs.cluster.pbs.PBSCommand;

import java.util.List;
import java.util.Map;

/**
 * Created by michael on 20.05.14.
 */
public class SGECommand extends PBSCommand {
    /**
     * @param job
     * @param run
     * @param executionService
     * @param id
     * @param processingCommands
     * @param parameters
     * @param arrayIndices
     * @param dependencyIDs
     * @param command
     */
    public SGECommand(Job job, ExecutionContext run, ExecutionService executionService, String id, List<ProcessingCommands> processingCommands, Map<String, String> parameters, List<String> arrayIndices, List<String> dependencyIDs, String command) {
        super(job, run, executionService, id, processingCommands, parameters, arrayIndices, dependencyIDs, command);
    }

    @Override
    public String getJoinLogParameter() {
        return " -j y";
    }

    @Override
    public String getEmailParameter(String address) {
        return " -M " + address;
    }

    @Override
    public String getGroupListString(String groupList) {
        return " ";
    }

    @Override
    public String getUmaskString(String umask) {
        return " ";
    }

    @Override
    public String getDependencyParameterName() {
        return "-hold_jid";
    }

    @Override
    public String getDependencyTypesSeparator() {
        return " ";
    }

    @Override
    public String getDependencyOptionSeparator() {
        return " ";
    }

    @Override
    public String getDependencyIDSeparator() {
        return ",";
    }

    @Override
    public String getArrayDependencyParameterName() {
        return "-hold_jid_ad";
    }

    @Override
    protected String getAdditionalCommandParameters() {
        return " -S /bin/bash ";
    }

    @Override
    protected String getDependsSuperParameter() {
        return " ";
    }
}
