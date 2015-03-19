package de.dkfz.roddy.client.fxuiclient;

/**
 * The class contains constants for Roddys JavaFX UI
 */
public final class UIConstants {

    /////////////////////////
    // Error messages
    /////////////////////////

    public static final String ERRSTR_PATHNOTFOUND = "<Path could not be loaded.>";
    public static final String ERRTXT_WORKFLOWNOTEXECUTED = "Could not execute workflow for pid ";
    public static final String ERRTXT_PROJECTSNOTLOADED = "Projects could not be loaded in Java FX Task.";

    /////////////////////////
    // Task names and description strings along task specific variables
    /////////////////////////

    public static final String UITASK_ANALYSIS_SELECTED = "change selected analysis w/o loaddatasets";
    public static final String UITASK_LOAD_PROJECTS = "load projects";
    public static final String UITASK_CHANGE_PROJECT = "change project";
    public static final String UITASK_MP_LOADCONFIGURATION = "load configuration";
    public static final String UITASK_MP_LOAD_ANALYSIS_LIST = "load analysis list";
    public static final String UITASK_ANALYSIS_SELECTED_UPD_CFGVIEWER = "set configuration viewer stuff";
    public static final String UITASK_LOAD_PROJECTS_DATASETS = "load datasets for project";
    public static final String UITASK_APPINFO_UPDATE_DAEMON = "FXUI application info update task";
    public static final String UITASK_RERUN_DATASETS = "Rerun selected datasets";
    public static final String UITASK_CHECKCONN = "check";
    public static final int UITASK_CHECKCONN_WAIT = 500;

    public static final String UIINVOKE_ADD_DATA_SETS_TO_LISTVIEW = "Add data sets to listview";
    public static final String UIINVOKE_CHANGE_STATE_IMAGES = "change state images";
    public static final String UIINVOKE_SET_APPINFO = "Set ui info updates";
    public static final String UIINVOKE_APPEND_PROCESSINGINFO = "Append a processing info object to the ui";

    private UIConstants() {}
}
