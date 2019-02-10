package geekbrains.java.cloud.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    private Network network = new Network(this);
    private ClientFileMethods clientFileMethods = new ClientFileMethods(network);
    private LoginController lc;

    @FXML
    private ListView<String> filesList;

    @FXML
    private ListView<String> serverFilesList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Thread t = new Thread(() -> {
            try {
                network.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
        showAuth();
    }

    public ClientFileMethods getClientFileMethods() {
        return clientFileMethods;
    }

    public void pressOnDownloadBtn(ActionEvent actionEvent) {
        String serverFileName = serverFilesList.getSelectionModel().getSelectedItem();
        if (serverFileName != null && !serverFileName.equals("") ){
            clientFileMethods.sendData("DOWNLOAD " + serverFileName);
        }
    }

    public void pressOnUploadBtn(ActionEvent actionEvent){
        String clientFileName = filesList.getSelectionModel().getSelectedItem();
        if (clientFileName != null && !clientFileName.equals("")){
            clientFileMethods.sendData("UPLOAD " + clientFileName);
            clientFileMethods.writeFile(clientFileName);
            refreshLocalFilesList();
            clientFileMethods.sendData("LIST");
        }
    }

    public void pressOnRfrshBtn(ActionEvent actionEvent) {
        refreshLocalFilesList();
        clientFileMethods.sendData("LIST");
    }

    public void pressOnDltBtn(ActionEvent actionEvent) {
        String serverFileName = serverFilesList.getSelectionModel().getSelectedItem();
        if (serverFileName != null && !serverFileName.equals("")) clientFileMethods.sendData("DELETE " + serverFileName);

        String clientFileName = filesList.getSelectionModel().getSelectedItem();
        if (clientFileName != null && !clientFileName.equals("")){
            try {
                Files.deleteIfExists(Paths.get(clientFileMethods.CLIENT_PATH + clientFileName));
                refreshLocalFilesList();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    void refreshLocalFilesList() {
        if (Platform.isFxApplicationThread()) {
            try {
                filesList.getItems().clear();
                Files.list(Paths.get(clientFileMethods.CLIENT_PATH)).map(p -> p.getFileName().toString()).forEach(o -> filesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Platform.runLater(() -> {
                try {
                    filesList.getItems().clear();
                    Files.list(Paths.get(clientFileMethods.CLIENT_PATH)).map(p -> p.getFileName().toString()).forEach(o -> filesList.getItems().add(o));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    void refreshServerFilesList(String[] serverList) {

        if (Platform.isFxApplicationThread()) {
            serverFilesList.getItems().clear();
            for (String o : serverList) serverFilesList.getItems().add(o);
        } else {
            Platform.runLater(() -> {
                serverFilesList.getItems().clear();
                for (String o : serverList) serverFilesList.getItems().add(o);
            });
        }
    }

    private void showAuth(){
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = loader.load();
            lc = (LoginController) loader.getController();
            lc.backController = this;
            stage.setTitle("JavaFX Autorization");
            stage.setScene(new Scene(root, 400, 400));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeAuth(){
        lc.close();
    }

    public void pressOnAddBtn(ActionEvent actionEvent) {
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddFolder.fxml"));
            Parent root = loader.load();
            AddFolderController afc = (AddFolderController) loader.getController();
            afc.backController = this;
            stage.setTitle("JavaFX Adding Folder");
            stage.setScene(new Scene(root, 400, 400));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pressOnFrwrdBtn(ActionEvent actionEvent) {
        String serverFileName = serverFilesList.getSelectionModel().getSelectedItem();
        if (serverFileName != null && !serverFileName.equals("")) clientFileMethods.sendData("FORWARD " + serverFileName + "/");
    }

    public void pressOnBckBtn(ActionEvent actionEvent) {
        clientFileMethods.sendData("BACK");
    }

//    public void pressOnAuthBtn(ActionEvent actionEvent) {
//        clientFileMethods.sendData("RELOG");
//        showAuth();
//    }
}