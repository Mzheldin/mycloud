package geekbrains.java.cloud.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginController {
    @FXML
    private TextField login;

    @FXML
    private PasswordField password;

    @FXML
    private VBox globParent;

    public Controller backController;

    public void auth(ActionEvent actionEvent) {
        if (!login.getText().trim().equals("") && !password.getText().trim().equals("")){
            backController.getClientFileMethods().sendData("AUTH " + (login.getText().trim() + " " + password.getText().trim()));
            backController.refreshLocalFilesList();
        }
    }

    public void reg(ActionEvent actionEvent) {
        if (!login.getText().trim().equals("") && !password.getText().trim().equals("")){
            backController.getClientFileMethods().sendData("REG " + (login.getText().trim() + " " + password.getText().trim()));
        }
    }

    public void close(){
        globParent.getScene().getWindow().hide();
    }
}
