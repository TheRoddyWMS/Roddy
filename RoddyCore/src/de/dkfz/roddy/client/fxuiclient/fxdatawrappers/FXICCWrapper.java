package de.dkfz.roddy.client.fxuiclient.fxdatawrappers;

import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.InformationalConfigurationContent;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * UI Wrapper for InformationalConfigurationContent objects
 * JavaFX says it is good style to have a wrapper class for objects so this is it. Not more not less.
 */
public class FXICCWrapper {
    private final String analysisID;
    private InformationalConfigurationContent icc;
    private final int uiPosition;

    private String currentlySelectedAnalysisID;

    public FXICCWrapper(InformationalConfigurationContent icc, int uiPosition) {
        this.icc = icc;
        this.analysisID = null;
        this.uiPosition = uiPosition;
    }

    public FXICCWrapper(InformationalConfigurationContent icc, String analysisID, int uiPosition) {
        this.icc = icc;
        this.analysisID = analysisID;
        this.uiPosition = uiPosition;
    }

    public String getAnalysisID() {
        return analysisID;
    }

    public boolean isAnalysisWrapper() {
        return analysisID != null;
    }

    public String getName() {
        return icc.name;
    }

    public String getID() {
        return icc.id;
    }

    public String getImports() {
        return icc.imports;
    }

    public boolean isSubProject() {
        return icc.parent != null;
    }

    /**
     * @return Returns a sorted list of the ids of the available analyses for the wrapped project.
     */
    public List<String> getAnalyses() {
        List<String> analyses = new LinkedList<>();
        for (String _analysis : icc.getListOfAnalyses()) {
            String[] analysis = _analysis.split("::");
            analyses.add(analysis[analysis.length - 2]);
        }
        Collections.sort(analyses);
        return analyses;
    }

    public String getCurrentlySelectedAnalysisID() {
        return currentlySelectedAnalysisID;
    }

    public void setCurrentlySelectedAnalysisID(String currentlySelectedAnalysisID) {
        this.currentlySelectedAnalysisID = currentlySelectedAnalysisID;
    }

    public Configuration.ConfigurationType getType() {
        return icc.type;
    }

    public InformationalConfigurationContent getICC() {
        return icc;
    }

    /**
     * Returns the level of the item in the hierarchy.
     *
     * @return
     */
    public int getDepth() {
        int level = 0;
        for (InformationalConfigurationContent icc = this.icc; icc.getParent() != null; icc = icc.getParent()) {
            level++;
        }
        return level;
    }

    public int getUIPosition() {
        return uiPosition;
    }

    public boolean hasAnalyses() {
        return getAnalyses().size() > 0;
    }

    public boolean hasSubConfigurations() {
        return icc.getSubContent().size() > 0;
    }
}
