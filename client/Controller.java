package geekbrains.java.cloud.client;

import geekbrains.java.cloud.common.CommandType;
import geekbrains.java.cloud.common.DataType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    private ClientController clientController = new ClientController();

    @FXML
    TextField tfFileName;

    @FXML
    ListView<String> filesList;

    @FXML
    ListView<String> serverFilesList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clientController.initConnection(); //входящий поток принимается Сканнером

        Thread t = new Thread(() -> {
            //исходные параметры приема данных
            int stage = -1; //этап
            DataType type = DataType.EMPTY; //тип сообщения
            Path pathToLoad = null; //путь к файлу на стороне клиента
            long fileReqLength = 0; // объем получаемого файла
            long loadedLength = 0; // полученные байты файла
            ByteBuffer fNameLenBuf = null;//байты длины имени принимаемого файла
            ByteBuffer fNameBuf = null;//байты имени приниаемого файла
            ByteBuffer fLen = null;//длина принимаемого файла
            try {
                while (true) {
                    if (clientController.hasNext()){ //если есть байт на входе
                        // определяем типа сообщения и буфер для приема длины имени
                        if (stage == -1){
                            byte firstByte = clientController.readByte();
                            type = DataType.getDataTypeFromByte(firstByte);
                            stage = 0;
                            fNameLenBuf = ByteBuffer.allocate(4);
                        }
                        // опринимаем и считываем длину имени, определяем буфер для имени файла
                        if (stage == 0){
                            if (fNameLenBuf.remaining() > 0) {
                                fNameLenBuf.put(clientController.readByte());
                                return;
                            }
                            int reqLength = fNameLenBuf.getInt();
                            fNameBuf = ByteBuffer.allocate(reqLength);
                            stage = 1;
                            System.out.println("text size: " + reqLength);
                        }
                        // принимаем имя файла и создаем путь и пустой файл для этого имени, задаем длину объема файла
                        if (stage == 1) {
                            if (fNameBuf.remaining() > 0) {
                                fNameBuf.put(clientController.readByte());
                                return;
                            }
                            if (type == DataType.FILE){
                                pathToLoad = Paths.get("client_storage/" + new String(fNameBuf.array()));
                                createFile(pathToLoad);
                                fLen = ByteBuffer.allocate(8);
                                stage = 2;
                            }
                        }
                        // принимаем и считываем объем файла
                        if (stage == 2){
                            if (fLen.remaining() > 0) {
                                fLen.put(clientController.readByte());
                                return;
                            }
                            fileReqLength = fLen.getLong();
                            loadedLength = 0;
                            stage = 3;
                        }
                        // пока счетчик полученных байт меньше объема файла, принимаем и записываем по байту
                        if (stage == 3){
                            if (loadedLength < fileReqLength){
                                byte[] data = new byte[1];
                                data[0] = clientController.readByte();
                                downloadFile(data, pathToLoad);
                                loadedLength++;
                                return;
                            }
                            // по окончании сбрасываем параметры приема
                            fNameLenBuf = null;
                            fNameBuf = null;
                            fLen = null;
                            stage = -1;
                            refreshLocalFilesList();
                        }

                    }
                    refreshLocalFilesList();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                clientController.closeConnection();
            }
        });
        t.setDaemon(true);
        t.start();
        filesList.setItems(FXCollections.observableArrayList());
        refreshLocalFilesList();
        serverFilesList.setItems(FXCollections.observableArrayList());
        refreshServerFilesList();
    }
    //создаем пустой файл для загрузки в него данных
    private void createFile(Path path){
        try{
            if (Files.notExists(path)) Files.createFile(path);
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    //запись байт в файл
    private void downloadFile(byte[] data, Path pathToLoad){
        try {
            Files.write(pathToLoad, data, StandardOpenOption.APPEND);
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    //обработка загрузки с сервера
    public void pressOnDownloadBtn(ActionEvent actionEvent) {
        //считываем имя файла с выделенного элемента списка сервера
        String serverFileName = checkServerFileListElement();
        if (serverFileName != null && !serverFileName.equals("") ){
            //формируем команду - тип, длину и содержимое и отправляем ее
            byte[] dataType = new byte[1];
            dataType[0] = 16;
            byte[] command = ("DOWNLOAD " + serverFileName).getBytes();
            int commandLength = command.length;
            ByteBuffer buf = ByteBuffer.allocate(4);
            buf.putInt(commandLength);
            try {
                clientController.sendMessage(dataType);
                clientController.sendMessage(buf.array());
                clientController.sendMessage(command);
            } catch (IOException e) {

            }
        }
    }
    //обработка загрузки на сервер
    public void pressOnUploadBtn(ActionEvent actionEvent){
        //считываем имя файла с выделенного элемента списка клиента
        String clientFileName = checkClientFileListElement();
        if (clientFileName != null && !clientFileName.equals("")){
            try {
                //формируем команду - тип, длину и содержимое и отправляем ее
                byte[] dataType = new byte[1];
                dataType[0] = 16;
                byte[] command = ("UPLOAD " + clientFileName).getBytes();
                ByteBuffer buf = ByteBuffer.allocate(4);
                buf.putInt(command.length);

                clientController.sendMessage(dataType);
                clientController.sendMessage(buf.array());
                clientController.sendMessage(command);
                //формируем и передаем тип(файл), длину имени, имя и размер файла
                dataType[0] = 15;
                byte[] fileName = (clientFileName).getBytes();
                ByteBuffer buff = ByteBuffer.allocate(4);
                buff.putInt(fileName.length);
                ByteBuffer fileSize = ByteBuffer.allocate(8);
                fileSize.putLong(Files.size(Paths.get("client_storage/" + clientFileName)));

                clientController.sendMessage(dataType);
                clientController.sendMessage(buff.array());
                clientController.sendMessage(fileName);
                clientController.sendMessage(fileSize.array());
                //передаем файл
                writeFile(clientFileName);

            } catch (IOException e){

            }
        }
    }
    //передача файла через буфер в 4 байта
    private void writeFile(String fileName){
        try {
            Path path = Paths.get("client_storage/" + fileName) ;
            if (Files.exists(path)){
                InputStream inputStream = Files.newInputStream(path);
                while (inputStream.available() > 0){
                    byte[] data = new byte[4];
                    inputStream.read(data);
                    clientController.sendMessage(data);
                }
                refreshLocalFilesList();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void refreshLocalFilesList() {
        if (Platform.isFxApplicationThread()) {
            try {
                filesList.getItems().clear();
                Files.list(Paths.get("client_storage")).map(p -> p.getFileName().toString()).forEach(o -> filesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Platform.runLater(() -> {
                try {
                    filesList.getItems().clear();
                    Files.list(Paths.get("client_storage")).map(p -> p.getFileName().toString()).forEach(o -> filesList.getItems().add(o));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void refreshServerFilesList() {
        if (Platform.isFxApplicationThread()) {
            try {
                serverFilesList.getItems().clear();
                Files.list(Paths.get("server_storage")).map(p -> p.getFileName().toString()).forEach(o -> serverFilesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Platform.runLater(() -> {
                try {
                    serverFilesList.getItems().clear();
                    Files.list(Paths.get("server_storage")).map(p -> p.getFileName().toString()).forEach(o -> serverFilesList.getItems().add(o));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public String checkServerFileListElement(){
        return serverFilesList.getSelectionModel().getSelectedItem();
    }

    public String checkClientFileListElement(){
        return filesList.getSelectionModel().getSelectedItem();
    }
}