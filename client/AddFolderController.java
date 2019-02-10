package geekbrains.java.cloud.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class AddFolderController {

    @FXML
    TextField folder;

    @FXML
    VBox globParent;

    public Controller backController;

    public void create(ActionEvent actionEvent) {
        if (!folder.getText().trim().equals("")){
            backController.getClientFileMethods().sendData("CREATE " + folder.getText() +"/");
            backController.refreshLocalFilesList();
        }
        globParent.getScene().getWindow().hide();
    }
}
