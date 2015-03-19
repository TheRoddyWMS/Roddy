package de.dkfz.roddy.client.fxuiclient.settingsviewer;

import de.dkfz.roddy.Constants;
import de.dkfz.roddy.Roddy;
import de.dkfz.roddy.tools.RoddyConversionHelperMethods;
import de.dkfz.roddy.client.fxuiclient.fxwrappercontrols.CustomControlOnGridPane;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

/**
 */
public class SSHExecutionServiceSettingsPanelControl extends CustomControlOnGridPane implements Initializable {

    @FXML
    private TextField txtRemoteHost;

    @FXML
    private TextField txtRemoteUser;

    @FXML
    private PasswordField txtPwdRemotePassword;

    @FXML
    private CheckBox chkUseKeyfileForRemoteConnection;

    @FXML
    private CheckBox chkStorePassword;

    @FXML
    private CheckBox chkUseCompression;

    @FXML
    private Label lblHeaderRunmode;

    private Roddy.RunMode runMode;

    public SSHExecutionServiceSettingsPanelControl(Roddy.RunMode mode) {
        super();
        this.runMode = mode;
        this.lblHeaderRunmode.setText(mode.name());
        readFromApplicationSetings();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    }

    private void readFromApplicationSetings() {
        txtRemoteHost.setText(Roddy.getApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_HOSTS, ""));
        txtRemoteUser.setText(Roddy.getApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_USER, System.getProperty("user.name")));
        txtPwdRemotePassword.setText(Roddy.getApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_PWD, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_PWD));
        if(RoddyConversionHelperMethods.toBoolean(Roddy.getApplicationProperty(Roddy.getRunMode(), Constants.APP_PROPERTY_EXECUTION_SERVICE_USE_COMPRESSION, Boolean.FALSE.toString()), false))
            chkUseCompression.setSelected(true);
        if (Roddy.getApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD).equals(Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_KEYFILE))
            chkUseKeyfileForRemoteConnection.setSelected(true);
        chkStorePassword.setSelected(Roddy.getApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_STORE_PWD, "true").equals("true"));

    }

    public void writeToApplicationSettings() {
        Roddy.setApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_HOSTS, txtRemoteHost.getText());
        Roddy.setApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_USER, txtRemoteUser.getText());
        Roddy.setApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_PWD, txtPwdRemotePassword.getText());
        Roddy.setApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_STORE_PWD, "" + chkStorePassword.isSelected());
        Roddy.setApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_USE_COMPRESSION, "" + chkUseCompression.isSelected());
        if (chkUseKeyfileForRemoteConnection.isSelected()) {
            Roddy.setApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_KEYFILE);
        } else {
            Roddy.setApplicationProperty(runMode, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD, Constants.APP_PROPERTY_EXECUTION_SERVICE_AUTH_METHOD_PWD);
        }

    }

}
