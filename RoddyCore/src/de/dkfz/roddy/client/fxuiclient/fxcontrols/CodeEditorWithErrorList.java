/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.client.fxuiclient.fxcontrols;

import de.dkfz.roddy.tools.RoddyIOHelperMethods;
import de.dkfz.roddy.StringConstants;
import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.validation.ScriptValidator;
import de.dkfz.roddy.config.validation.ValidationError;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.CustomControlOnBorderPane;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A syntax highlighting code editor for JavaFX created by wrapping a
 * CodeMirror code editor in a WebView.
 * <p/>
 * See http://codemirror.net for more information on using the codemirror editor.
 */
public class CodeEditorWithErrorList extends CustomControlOnBorderPane {
    /**
     * a webview used to encapsulate the CodeMirror JavaScript.
     */
    @FXML
    private WebView webview;

    @FXML
    private Label lblWebviewLoaderState;

    @FXML
    private Label lblFilePath;

    @FXML
    private ProgressBar lblWebviewProgress;

    @FXML
    private VBox bottomVBox;

    private ListView lstFileValidationResult;

    /**
     * a snapshot of the code to be edited kept for easy initilization and reversion of editable code.
     */
    private String editingCode;

    private Map<String, String> editModeByFileSuffix = new LinkedHashMap<>();

    private String fileSuffix = "shell";

    /**
     * The file which is about to be edited.
     */
    private File editedFile;
    private Configuration configuration;

    private String toolID;

    /**
     * applies the editing template to the editing code to create the html+javascript source for a code editor.
     */
    private String applyEditingTemplate() {
        File path = new File("./dist/dependencies/codemirror");

        String editingTemplate = RoddyIOHelperMethods.loadTextFileEnblock(new File(path.getAbsolutePath() + "/editingTemplate.html"));
        String code = editingTemplate.replace("${code}", "" + editingCode);
        code = code.replace("${mode}", editModeByFileSuffix.get(fileSuffix));
        code = code.replace("${path}", path.getAbsolutePath());
//        System.out.println(code);
//        RoddyIOHelperMethods.writeTextFile("./test.html", code);
        return code;
    }

    /**
     * sets the current code in the editor and creates an editing snapshot of the code which can be reverted to.
     */
    public void setCode(String newCode) {
        this.editingCode = newCode;
        webview.getEngine().loadContent(applyEditingTemplate());
    }

    /**
     * returns the current code in the editor and updates an editing snapshot of the code which can be reverted to.
     */
    public String getCodeAndSnapshot() {
        this.editingCode = (String) webview.getEngine().executeScript("editor.getValue();");
        return editingCode;
    }

    /**
     * revert edits of the code to the last edit snapshot taken.
     */
    public void revertEdits() {
        setCode(editingCode);
    }

    public CodeEditorWithErrorList(Configuration cfg, String toolID, File file) {
        init();
        this.toolID = toolID;
        String fileContent = loadFile(cfg, file);
        loadCode(fileContent);
    }

    public CodeEditorWithErrorList(Configuration cfg, File file) {
        init();
        String fileContent = loadFile(cfg, file);
        loadCode(fileContent);
    }

    public CodeEditorWithErrorList(File file) {
//        this(loadFile(file));
        init();
        String fileContent = loadFile(file);
        loadCode(fileContent);
    }

    /**
     * Create a new code editor.
     *
     * @param editingCode the initial code to be edited in the code editor.
     */
    private CodeEditorWithErrorList(String editingCode) {
        init();
        loadCode(editingCode);
    }

    private String loadFile(Configuration cfg, File file) {
        lstFileValidationResult = new ListView<>();
        lstFileValidationResult.setMaxHeight(200);
        bottomVBox.getChildren().add(0, lstFileValidationResult);
        validateScriptFile(cfg, toolID, file);
        return loadFile(file);
    }

    private void validateScriptFile(Configuration cfg, String toolID, File file) {
        try {
            this.configuration = cfg;
            ScriptValidator sv = new ScriptValidator(cfg);
            sv.validateVariableUsageInFile(file, toolID);
            lstFileValidationResult.getItems().clear();
            for (ValidationError validationError : sv.getValidationErrors()) {
                lstFileValidationResult.getItems().add(validationError);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String loadFile(File file) {
        this.editedFile = file;
        lblFilePath.setText(file.getAbsolutePath());


        String fileContent = StringConstants.EMPTY;
        if (file != null) {
            if (file.exists()) {
                String name = file.getName();
                String[] split = name.split(StringConstants.SPLIT_STOP);
                fileSuffix = split[split.length - 1];
                //TODO Make configurable.
                if (editModeByFileSuffix.containsKey(fileSuffix))
                    fileContent = RoddyIOHelperMethods.loadTextFileEnblock(file);
            }
        }
        return fileContent;
    }

    private void init() {

        //TODO Fill or find it somewhere...
        editModeByFileSuffix.put("sh", "shell");
        editModeByFileSuffix.put("py", "python");
        editModeByFileSuffix.put("pl", "perl");
        editModeByFileSuffix.put("xml", "xml");

//        codeFileFormatterByFileSuffix.put("sh", "shell/shell.js");
//        codeFileFormatterByFileSuffix.put("pl", "perl/perl.js");
//        codeFileFormatterByFileSuffix.put("py", "python/python.js");
        Worker<Void> loadWorker = webview.getEngine().getLoadWorker();
//        webview.getEngine().getDocument().
        loadWorker.messageProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String s2) {
//                System.out.println("New webview message: " + s2);
            }
        });
        loadWorker.stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue<? extends Worker.State> observableValue, Worker.State state, Worker.State state2) {
//                System.out.println("New webview state: " + state2);
                lblWebviewLoaderState.setText(state2.name());
//                if (state2 == Worker.State.SUCCEEDED) {
//                    System.out.println(getCodeAndSnapshot());
//                }
            }
        });
        loadWorker.exceptionProperty().addListener(new ChangeListener<Throwable>() {
            @Override
            public void changed(ObservableValue<? extends Throwable> ov, Throwable t, Throwable t1) {
//                System.out.println("Received exception: " + t1.getMessage());
            }
        });
        loadWorker.progressProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                lblWebviewProgress.setProgress(number2.doubleValue());
//                System.out.println("New progress number: " + number2);
            }
        });

        loadWorker.titleProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String s2) {
//                System.out.println("New title: " + s2);
            }
        });
    }

    private void loadCode(String editingCode) {
        this.editingCode = editingCode;
        String code = applyEditingTemplate();
        webview.getEngine().loadContent(code);
    }

    public void saveFile(ActionEvent actionEvent) {
        RoddyIOHelperMethods.writeTextFile(editedFile, getCodeAndSnapshot());
        validateScriptFile(configuration, toolID, editedFile);
    }
}
