/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.core;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * This class stores all process relevant information for a dataset.
 * These information contain (previous) runs
 */
public class AnalysisProcessingInformation extends InfoObject {

    private final Analysis analysis;
    private final File execPath;
    private final DataSet dataSet;
    private String executingUser;
    private ExecutionContext executionContext;
//    private final List<AnalysisProcessingInformationListener> listeners = new LinkedList<>();

    /**
     * Constructor for read out information.
     * @param analysis
     * @param dataSet
     * @param execPath
     */
    public AnalysisProcessingInformation(Analysis analysis, DataSet dataSet, File execPath) {
        this.analysis = analysis;
        this.execPath = execPath;
        this.dataSet = dataSet;
    }

    /**
     * Constructor for running processes.
     * @param ec
     */
    public AnalysisProcessingInformation(ExecutionContext ec) {
        this(ec.getAnalysis(), ec.getDataSet(), ec.getExecutionDirectory());
        this.executionContext = ec;
    }

    public void setExecutingUser(String user) {
        this.executingUser = user;
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public File getExecPath() {
        return execPath;
    }

    public DataSet getDataSet() {
        return dataSet;
    }

    public ExecutionContext getDetailedProcessingInfo() {
        if (executionContext == null) {
            executionContext = analysis.getRuntimeService().readInExecutionContext(this);
            if(executingUser != null)
                executionContext.setExecutingUser(executingUser);
        }
        return executionContext;
    }

    public Date getExecutionDate() {
        return analysis.getRuntimeService().extractDateFromExecutionDirectory(execPath);
    }

    public String getExecutionDateHumanReadable() {
        return InfoObject.formatTimestampReadable(getExecutionDate());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnalysisProcessingInformation that = (AnalysisProcessingInformation) o;

        if (analysis != null ? !analysis.equals(that.analysis) : that.analysis != null) return false;
        if (dataSet != null ? !dataSet.equals(that.dataSet) : that.dataSet != null) return false;
        if (execPath != null ? !execPath.equals(that.execPath) : that.execPath != null) return false;

        return true;
    }

    @Override
    public String toString() {
        return String.format("AnalysisProcessingInformation with timestamp %s for path %s and analysis %s", InfoObject.formatTimestamp(getTimeStamp()), execPath.getAbsolutePath(), analysis.getName());
    }

//    public void addListener(AnalysisProcessingInformationListener apil) {
//        this.listeners.add(apil);
//    }
}
