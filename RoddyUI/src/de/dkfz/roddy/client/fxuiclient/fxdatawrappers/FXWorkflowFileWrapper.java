/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient.fxdatawrappers;

import de.dkfz.roddy.execution.jobs.BEJob;
import de.dkfz.roddy.knowledge.files.BaseFile;
import javafx.beans.property.*;

/**
 */
public class FXWorkflowFileWrapper extends FXFileWrapper {

    private BEJob job;
    private StringProperty _jobID = new SimpleStringProperty();
    private BooleanProperty _isFileMissing = new SimpleBooleanProperty();
    private BooleanProperty _isTemporaryFile = new SimpleBooleanProperty();
    private ObjectProperty<BaseFile> _baseFile= new SimpleObjectProperty<>();
    private BaseFile baseFile;

    public FXWorkflowFileWrapper(BaseFile file, BEJob job) {
        super(file.getPath());
        this.job = job;
        this.baseFile = file;
        this._baseFile.setValue(baseFile);
        this._jobID.setValue(job.getJobID());
        this._isFileMissing.setValue(!file.isFileValid());
        this._isTemporaryFile.setValue(file.isTemporaryFile());
    }

    public BEJob getJob() {
        return job;
    }

    public StringProperty jobIDProperty() {
        return _jobID;
    }

    public BooleanProperty isTemporaryFileProperty() {
        return _isTemporaryFile;
    }

    public BooleanProperty isFileMissingProperty() {
        return _isFileMissing;
    }

    public ObjectProperty<BaseFile> baseFileProperty() {
        return _baseFile;
    }
}
