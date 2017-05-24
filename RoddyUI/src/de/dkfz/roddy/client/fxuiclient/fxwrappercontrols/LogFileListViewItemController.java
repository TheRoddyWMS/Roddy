/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient.fxwrappercontrols;

import de.dkfz.eilslabs.batcheuphoria.jobs.JobState;
import de.dkfz.roddy.client.fxuiclient.fxdatawrappers.FXLogFileWrapper;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

/**
 */
public class LogFileListViewItemController extends CustomCellItemsHelper.CustomCellItemController<FXLogFileWrapper> {

    public Label lblJobID;
    public Label lblFilePath;
    public ImageView indicatorOK;
    public ImageView indicatorError;
    public ImageView indicatorUnknown;
    public ImageView indicatorLogFileIsMissing;
    public GridPane iconsPane;
    private FXLogFileWrapper wrapper;

    @Override
    public void initialize() {
    }

    @Override
    public void itemSet(FXLogFileWrapper item) {
        this.wrapper = wrapper;
        indicatorUnknown.setVisible(item.getJobState() == JobState.UNKNOWN);
        indicatorError.setVisible(item.getJobState() == JobState.FAILED);
        indicatorOK.setVisible(item.getJobState() == JobState.OK);
        if(item.getJob().hasLogFile()) {
           iconsPane.getChildren().remove(indicatorLogFileIsMissing);
        }
    }
}
