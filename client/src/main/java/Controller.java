import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Observable;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {

    @FXML
    HBox authPanel, actionPanel1, actionPanel2;
    @FXML
    TextField loginField;
    @FXML
    PasswordField passField;
    @FXML
    ListView<File> cloudList;
    @FXML
    TableView<File> localList;
    @FXML
    ProgressBar operationProgress;

    private boolean authorized;
    private ObservableList<File> cloudFileList;
    private ObservableList<File> localFileList;

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
        this.authPanel.setManaged(!this.authorized);
        this.authPanel.setVisible(!this.authorized);
        this.actionPanel1.setManaged(this.authorized);
        this.actionPanel1.setVisible(this.authorized);
        this.actionPanel2.setManaged(this.authorized);
        this.actionPanel2.setVisible(this.authorized);

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthorized(false);
        cloudFileList = FXCollections.observableArrayList();
        localFileList = FXCollections.observableArrayList();
        cloudList.setItems(cloudFileList);

        TableColumn<File, String> tcName = new TableColumn<>("Name");
        tcName.setCellValueFactory(new PropertyValueFactory<File, String>("name"));
        tcName.setPrefWidth(200);

        TableColumn<File, String> tcSize = new TableColumn<>("Size");
        tcSize.setCellValueFactory(param -> {
            long size = param.getValue().length();
            return new ReadOnlyObjectWrapper(String.valueOf(size) + " bytes");
        });
        tcSize.setPrefWidth(120);

        localList.getColumns().addAll(tcName, tcSize);
        localList.setItems(localFileList);

        refreshLocalList();

        localList.setOnDragOver(event -> {
            if (event.getGestureSource() != localList && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        localList.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                for (int i = 0; i < db.getFiles().size(); i++) {
                    Path source = Paths.get(db.getFiles().get(i).getAbsolutePath());
                    Path target = Paths.get("client/local_storage/" + db.getFiles().get(i).getName());
                    try {
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                refreshLocalList();
                success = true;
            }
            event.consume();
        });

    }

    public void connect() {
        try {
            if (!Network.getOurInstance().isConnected()) {
                Network.getOurInstance().connect();
            }
            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        Object obj = Network.getOurInstance().readData();
                        if (obj instanceof CommandMessage) {
                            CommandMessage cm = (CommandMessage) obj;
                            if (cm.getType() == CommandMessage.CMD_MSG_AUTH_OK) {
                                setAuthorized(true);
                                break;
                            }
                        }
                    }
                    while (true) {
                        Object obj = Network.getOurInstance().readData();
                        if (obj instanceof FileListMessage) {
                            FileListMessage flm = (FileListMessage) obj;
                            Platform.runLater(() -> {
                                cloudFileList.clear();
                                cloudFileList.addAll(flm.getFiles());
                            });
                        }
                        if (obj instanceof FileMessage) {
                            FileMessage fm = (FileMessage) obj;
                            try {
                                Path path = Paths.get("client/local_storage/" + fm.getFilename());
                                if(!Files.exists(path)){
                                    Files.createFile(path);
                                }
                                RandomAccessFile raf = new RandomAccessFile(path.toFile(),"rw");
                                FileChannel outChannel = raf.getChannel();
                                outChannel.position(fm.getPartNumber()*FilePartitionWorker.PART_SIZE);
                                ByteBuffer buf = ByteBuffer.allocate(fm.getData().length);
                                buf.put(fm.getData());
                                buf.flip();
                                outChannel.write(buf);
                                buf.clear();
                                outChannel.close();
                                raf.close();
                                if(fm.getPartNumber() == fm.getPartsCount()){
                                    Platform.runLater(() -> refreshLocalList());
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                            }

                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    Network.getOurInstance().close();
                }
            });
            t.setDaemon(true);
            t.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refreshLocalList() {
        try {
            localFileList.clear();
            localFileList.addAll(Files.list(Paths.get("client/local_storage")).map(Path::toFile).collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void btnSendFile(ActionEvent actionEvent){
        Path path = Paths.get(localList.getSelectionModel().getSelectedItem().getAbsolutePath());
        ObjectEncoderOutputStream out = Network.getOurInstance().getOut();
        FilePartitionWorker.sendFileFromClient(path,out,operationProgress);
    }

    public void tryToAuthorize(ActionEvent actionEvent){
        if(!Network.getOurInstance().isConnected()){
            connect();
        }
        AuthMessage am = new AuthMessage(loginField.getText(),passField.getText());
        sendMsg(am);
    }

    private void sendMsg(AbstractMessage am) {
        try {
            Network.getOurInstance().sendData(am);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestFileDownload(ActionEvent actionEvent){
        File file = cloudList.getSelectionModel().getSelectedItem();
        CommandMessage cm = new CommandMessage(CommandMessage.CMD_MSG_REQUEST_FILE_DOWNLOAD,file);
        sendMsg(cm);
    }

    public void btnLoaclDeleteFile(ActionEvent actionEvent){
        try{
            Files.delete(Paths.get(localList.getSelectionModel().getSelectedItem().getAbsolutePath()));
            refreshLocalList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void btnLocalRefresh(ActionEvent actionEvent){
        refreshLocalList();
    }

    public void btnCloudRefresh(ActionEvent actionEvent){
        CommandMessage cm = new CommandMessage(CommandMessage.CMD_MSG_REQEST_FILES_LIST);
        sendMsg(cm);
    }

    public void requestCloudDeleteFile(ActionEvent actionEvent){
        CommandMessage cm = new CommandMessage(CommandMessage.CMD_MSG_REQEST_SERVER_DELETE_FILE,cloudList.getSelectionModel().getSelectedItem().getAbsoluteFile());
        sendMsg(cm);
    }
    public void sendFile(DragEvent dragEvent) {
    }

    public void draggingOver(DragEvent dragEvent) {
    }

    public void downloadFile(ActionEvent actionEvent) {
    }

    public void deleteFile(ActionEvent actionEvent) {
    }

    public void updateFiles(ActionEvent actionEvent) {
    }

}
