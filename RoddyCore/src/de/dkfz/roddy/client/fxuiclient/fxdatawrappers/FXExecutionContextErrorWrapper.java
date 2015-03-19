package de.dkfz.roddy.client.fxuiclient.fxdatawrappers;

import de.dkfz.roddy.core.ExecutionContextError;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 */
public class FXExecutionContextErrorWrapper {
    private SimpleObjectProperty<ExecutionContextError> error = new SimpleObjectProperty<>();

    private SimpleObjectProperty<Exception> exception = new SimpleObjectProperty<>();
    ;
    private StringProperty errorLevel = new SimpleStringProperty();
    private StringProperty errorType = new SimpleStringProperty();
    private StringProperty errorText = new SimpleStringProperty();

    public FXExecutionContextErrorWrapper(ExecutionContextError error) {
        this.error.setValue(error);
        this.exception.setValue(error.getException());
        this.errorText.setValue(error.getDescription());
        this.errorLevel.setValue(error.getErrorLevel().getName());
    }

    public String getErrorLevel() {
        return errorLevel.get();
    }

    public StringProperty errorLevelProperty() {
        return errorLevel;
    }

    public String getErrorType() {
        return errorType.get();
    }

    public StringProperty errorTypeProperty() {
        return errorType;
    }

    public String getErrorText() {
        return errorText.get();
    }

    public StringProperty errorTextProperty() {
        return errorText;
    }

    public ExecutionContextError getError() {
        return error.get();
    }

    public SimpleObjectProperty<ExecutionContextError> errorProperty() {
        return error;
    }

    public boolean hasException() {
        return error.getValue().getException() != null;
    }
}
