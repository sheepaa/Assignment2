package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.HttpRequest;
import cn.edu.sustech.cs209.chatting.common.HttpResponse;
import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.privateChat;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

import javax.management.loading.PrivateClassLoader;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;


public class Controller implements Initializable {

    @FXML
    ListView<Message> chatContentList;

    @FXML
    ListView<String> chatList;
    ChatObjCellFactory chatObjCellFactory = new ChatObjCellFactory();
    @FXML
    TextArea inputArea;



    ObservableList<String> chatObj;
    ObservableList<Message> chatContentObj;

    List<String> test;
//    ListView<privateChat> chatList;

    String username;
    Client client;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            client = new Client(this);
        } catch (IOException e) {
            System.out.println("Fail in create client");
            throw new RuntimeException(e);
        }

        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText(null);
        dialog.setContentText("Username:");

        Optional<String> input = dialog.showAndWait();
        if (input.isPresent() && !input.get().isEmpty()) {
            /*
               TODO: Check if there is a user with the same name among the currently logged-in users,
                     if so, ask the user to change the username
             */

            username = input.get();
            boolean check;
            try {
                while(!client.checkUsername(username)){
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
//                    alert.setHeaderText("发生了一个错误");
                    alert.setContentText("用户名重复，请更改");
                    alert.showAndWait();
                    this.initialize(url, resourceBundle);
                    break;
                }

            } catch (IOException | ClassNotFoundException e) {
                System.out.println("fail in checkusername");
                throw new RuntimeException(e);
            }

        } else {
            System.out.println("Invalid username " + input + ", exiting");
            Platform.exit();
        }
        //登录成功
        try {
            client.startListenThread();
            client.notifyAllUsers();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            client.getAllRecords();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        chatContentList.setCellFactory(new MessageCellFactory());
//        Message ini = new Message(1000L,username,"other", "呱");
//        Message ini2 = new Message(900L, "other", username,"呱");
        chatContentObj = FXCollections.observableArrayList();
//        chatObjCellFactory = new ChatObjCellFactory();
        chatList.setCellFactory(chatObjCellFactory);

//        chatList.setCellFactory(new ChatObjCellFactory());

        chatObj = FXCollections.observableArrayList();

        chatContentList.setItems(chatContentObj);
        chatList.setItems(chatObj);
//
//        chatContentObj.add(ini);
//        chatContentObj.add(ini2);

//        test = Arrays.asList("a","b","c");
//        chatList.getItems().addAll(test);
//        test.add("d");
    }

    @FXML
    public void createPrivateChat() throws IOException {
        AtomicReference<String> user = new AtomicReference<>();

        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();

        // FIXME: get the user list from server, the current user's name should be filtered out
        List<String> users = client.getAllUsers(); //会有IOException
        userSel.getItems().addAll(users);

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            stage.close();
        });

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();

        // TODO: if the current user already chatted with the selected user, just open the chat with that user
        // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
        if(user.get() != null)client.checkPrivateChatById(user.get());
    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat() throws IOException {

        Stage stage = new Stage();
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));

        List<String> users = client.getAllUsers();
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        List<CheckBox> checkBoxList = new ArrayList<>(); // 存储CheckBox的List
        for (int i = 0; i < users.size(); i++) {
            CheckBox checkBox = new CheckBox(users.get(i));
            checkBoxList.add(checkBox); // 添加到List中
        }
        vbox.getChildren().addAll(checkBoxList);
        Button okBtn = new Button("OK");
        List<String> selectedItems = new ArrayList<>();
        okBtn.setOnAction(e -> {
            for (CheckBox checkBox : checkBoxList) {
                if (checkBox.isSelected()) {
                    selectedItems.add(checkBox.getText());
                    stage.close();
                }
            }
        });

        System.out.println("select: " + selectedItems);
        box.getChildren().addAll(vbox, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();

        //获取了群聊对象后创建群聊
        if(selectedItems.size() != 0)client.createGroupChat(selectedItems);

    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() throws IOException {
        // TODO
        String text = inputArea.getText();
//        System.out.println("in send message");
//        System.out.println(text);
        //发送的信息不能为空
        if(text.equals("")){
            System.out.println("text is null");
            Alert alert = new Alert(Alert.AlertType.ERROR);
//            alert.setTitle("错误");
            alert.setContentText("发送的信息不能为空");
            alert.showAndWait();
        }else{
            client.sendMessage(text);
        }
        inputArea.setText("");

    }
    @FXML
    public void doUploadFile() throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");

        // 设置文件选择器的初始目录
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        // 显示文件选择器，并获取用户选择的文件
        File selectedFile = fileChooser.showOpenDialog(inputArea.getScene().getWindow());

        // 在这里处理选择的文件，例如上传文件
        if (selectedFile != null) {
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());
            // 你可以在这里添加文件上传的代码
            client.uploadFile(selectedFile);
        } else {
            System.out.println("No file selected");
        }

    }

    public void doDownloadFile(UUID uuid, String fileName) throws IOException {
//        System.out.println("in download file");
//        System.out.println("filename is " + fileName);
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Download Location");
        File selectedDirectory = directoryChooser.showDialog(inputArea.getScene().getWindow());

        if (selectedDirectory != null) {
            client.downloadFile(uuid, fileName, selectedDirectory);
        }
    }

    @FXML
    public void changeChat(){

        String selectedItem = chatList.getSelectionModel().getSelectedItem();
        client.loadRecords(selectedItem);
    }

    public void notifyMessage(String str){
        System.out.println("notifymessage");
        chatObjCellFactory.setCircleColor(chatList, str,Color.red);
    }
    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */
    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {

                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");


                    if(msg.isFile() && !username.equals(msg.getSentBy())){
                        HBox message = new HBox();
                        Label messageLabel = new Label(msg.getFileName());
                        Button button = new Button("download");
                        button.setOnAction(e -> {
                            try {
                                doDownloadFile(msg.getUUID(),msg.getFileName());
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        });
                        message.getChildren().addAll(messageLabel, button);
                        if (username.equals(msg.getSentBy())) {
                            wrapper.setAlignment(Pos.TOP_RIGHT);
                            wrapper.getChildren().addAll(message, nameLabel);
                            message.setPadding(new Insets(0, 20, 0, 0));
                        } else {
                            wrapper.setAlignment(Pos.TOP_LEFT);
                            wrapper.getChildren().addAll(nameLabel, message);
                            message.setPadding(new Insets(0, 0, 0, 20));
                        }
                    }else{
                        Label msgLabel;
                        if(msg.isFile()) msgLabel = new Label(msg.getFileName());
                        else msgLabel = new Label(msg.getData());
                        if (username.equals(msg.getSentBy())) {
                            wrapper.setAlignment(Pos.TOP_RIGHT);
                            wrapper.getChildren().addAll(msgLabel, nameLabel);
                            msgLabel.setPadding(new Insets(0, 20, 0, 0));
                        } else {
                            wrapper.setAlignment(Pos.TOP_LEFT);
                            wrapper.getChildren().addAll(nameLabel, msgLabel);
                            msgLabel.setPadding(new Insets(0, 0, 0, 20));
                        }
                    }


                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }
    private class ChatObjCellFactory implements Callback<ListView<String>, ListCell<String>> {
        public void setCircleColor(ListView<String> listView, String str, Color color) {
            listView.getItems().stream()
                    .map(item -> listView.lookup("#hbox-" + item))
                    .filter(hbox -> hbox instanceof HBox)
                    .map(hbox -> (HBox) hbox)
                    .filter(hbox -> {
                        Label label = (Label) hbox.getChildren().get(0);
                        return label.getText().equals(str);
                    })
                    .forEach(hbox -> {
                        Circle circle = (Circle) hbox.getChildren().get(1);
                        circle.setFill(javafx.scene.paint.Color.RED);
                    });
        }
        @Override
        public ListCell<String> call(ListView<String> stringListView) {
            return new ListCell<String>() {

                @Override
                public void updateItem(String msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    Circle circle = new Circle(4);
                    circle.setFill(null);
                    Label nameLabel = new Label(msg);
                    HBox hbox = new HBox();
                    hbox.setId("hbox-" + msg); // 添加一个唯一的ID
                    hbox.getChildren().addAll(nameLabel, circle);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(hbox);

                }
            };
        }
    }
}
