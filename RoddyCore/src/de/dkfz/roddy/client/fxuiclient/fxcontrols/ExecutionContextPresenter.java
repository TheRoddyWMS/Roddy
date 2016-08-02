/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient.fxcontrols;

import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.CustomCellItemsHelper;
import de.dkfz.roddy.core.*;
import de.dkfz.roddy.execution.jobs.Job;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 */
public abstract class ExecutionContextPresenter<T> extends CustomCellItemsHelper.CustomCellItemController<T> implements DataSetListener, ExecutionContextListener {

    private static final String ICO_FILE_MISSING = "/imgs/icon_status_logfile_missing.png";
    private static final String ICO_EXECUTION_ERROR = "/imgs/icon_status_error.png";
    private static final String ICO_WARNING= "/imgs/icon_status_warning.png";

    private static boolean iconsAreLoaded = false;

    private static Map<ExecutionContextError, Image> availableIcons = new LinkedHashMap<>();

    private static Image loadImage(String image) {
        return new Image(ExecutionContextPresenter.class.getResourceAsStream(image));
    }

    protected static synchronized void loadIcons() {
        if (iconsAreLoaded) return;

        availableIcons.put(ExecutionContextError.READBACK_NOBINARYSERIALIZEDJOBS, loadImage(ICO_FILE_MISSING));
        availableIcons.put(ExecutionContextError.READBACK_NOJOBSTATESFILE, availableIcons.get(ExecutionContextError.READBACK_NOBINARYSERIALIZEDJOBS));
        availableIcons.put(ExecutionContextError.READBACK_NOREALJOBCALLSFILE, availableIcons.get(ExecutionContextError.READBACK_NOBINARYSERIALIZEDJOBS));
        availableIcons.put(ExecutionContextError.READBACK_NOEXECUTEDJOBSFILE, loadImage(ICO_WARNING));
        availableIcons.put(ExecutionContextError.EXECUTION_JOBFAILED, loadImage(ICO_EXECUTION_ERROR));
        availableIcons.put(ExecutionContextError.EXECUTION_NOINPUTDATA, availableIcons.get(ExecutionContextError.EXECUTION_JOBFAILED));
        availableIcons.put(ExecutionContextError.EXECUTION_UNCATCHEDERROR, availableIcons.get(ExecutionContextError.EXECUTION_JOBFAILED));

        iconsAreLoaded = true;
    }

    protected Label createErrorIcon(ExecutionContextError ece) {
        loadIcons();

        Label l = new Label();
        Image image = availableIcons.get(ece.getBase());

        ImageView iv = new ImageView(image);

        l.setGraphic(iv);
        l.setTooltip(new Tooltip(ece.getDescription()));
        return l;
    }

    @Override
    public void processingInfoAddedEvent(DataSet dataSet, AnalysisProcessingInformation pi) {
    }

    @Override
    public void processingInfoRemovedEvent(DataSet dataSet, AnalysisProcessingInformation pi) {

    }

    @Override
    public void newExecutionContextEvent(ExecutionContext context) {
    }

    @Override
    public void jobStateChangedEvent(Job job) {
    }

    @Override
    public void jobAddedEvent(Job job) {
    }

    @Override
    public void fileAddedEvent(File file) {
    }

    @Override
    public void detailedExecutionContextLevelChanged(ExecutionContext context) {
    }
}
