package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.HttpRequest;
import cn.edu.sustech.cs209.chatting.common.HttpResponse;
import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

public class Client {
    private Socket socket;
    private BufferedWriter out;
    private BufferedReader in;

    private Controller controller;
    private Map<String, ObservableList<Message>> chatMap;
    private String username;
    private ListenThread listenThread;
    private String current;

    public Client(Controller controller) throws IOException {
        this.socket = new Socket("localhost", 8080);
        System.out.println("client running at " + this.socket.getLocalPort());
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        print("REQUEST");
        this.controller = controller;
        this.chatMap = new HashMap<>();
    }

    public void startListenThread() throws IOException {
        this.listenThread = new ListenThread(this);
        listenThread.start();
    }

    private void print(String message) throws IOException {
        out.write(message);
        out.newLine();
        out.flush();
    }

    public boolean checkUsername(String username) throws IOException, ClassNotFoundException {
//        out.println("/checkUsername");
//        out.println(username);
        print("/checkUsername");
        print(username);
        while (true) {
            String response = in.readLine();
            if (response == null) continue;
            else {
                this.username = username;
                return response.equals("200");
            }
        }
    }

    public void getAllRecords() throws IOException, ClassNotFoundException {
        print("/getAllRecords");
        if(in.readLine().equals("True")){
            int count = Integer.parseInt(in.readLine());//有多少个会话
            for (int i = 0; i < count; i++) {
                String chatName = in.readLine();//会话名称
                ObservableList<Message> add = FXCollections.observableArrayList();
                Platform.runLater(()->
                        controller.chatObj.add(chatName)
                );
                String mstr;
                while(!(mstr = in.readLine()).equals("end")){
                    Message message = deserialize(mstr);
                    add.add(message);
                }
                chatMap.put(chatName, add);
            }
        }
    }

    public List<String> getAllUsers() throws IOException {
//        out.println("/getAllUsers");
        print("/getAllUsers");
        List<String> users = new ArrayList<>();
        int size = Integer.parseInt(in.readLine());
        for (int i = 0; i < size; i++) {
            String user = in.readLine();
            users.add(user);
        }
        return users;
//        while(in.ready()){
//            String user = in.readLine();
//            users.add(user);
//            System.out.println("client receive: " + user);
//        }

//        while (true) {
//            while(in.ready()){
//                String user = in.readLine();
//                users.add(user);
//                System.out.println("client receive: " + user);
//            }
//
//        }
    }

    public void checkPrivateChatById(String id) throws IOException {
        if (chatMap.containsKey(id)) {
            loadRecords(id);
        } else {
            ObservableList<Message> list = FXCollections.observableArrayList();
            print("/createPrivateChat\n" + id);
            chatMap.put(id, list);
            controller.chatObj.add(id);
            loadRecords(id);
        }
    }

    public void loadRecords(String id) {
        Platform.runLater(()->
                controller.chatContentList.setItems(chatMap.get(id)) );
        this.current = id;

    }

    public void sendMessage(String text) throws IOException {
        System.out.println("in send message");
        Message message = new Message(System.currentTimeMillis(), username, current,text);
        String serialized = serialize(message);//Ioexception
        print("/sendMessage");
        print(serialized);
        chatMap.get(current).add(message);
    }

    public void createGroupChat(List<String> otherUsers) throws IOException {
        //群聊名称为others+username
        StringBuilder chatNameBuilder = new StringBuilder();
        for (String otherUser : otherUsers) {
            chatNameBuilder.append(otherUser+",");
        }
        chatNameBuilder.append(username);
        String chatName = chatNameBuilder.toString();
        System.out.println(chatName);
        //向服务器发送请求
        print("/createGroupChat");
        print(chatName);
        //加入chatList
        ObservableList<Message> list = FXCollections.observableArrayList();
        chatMap.put(chatName, list);
        controller.chatObj.add(chatName);
        loadRecords(chatName);
    }


    private  String serialize(Message message) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        oos.close();
        return new String(Base64.getEncoder().encode(baos.toByteArray()));
    }
    private static Message deserialize(String str) throws IOException, ClassNotFoundException {
        byte[] bytes = Base64.getDecoder().decode(str.getBytes());
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Message message = (Message) ois.readObject();
        ois.close();
        return message;
    }

    //    private void sendRequest(HttpRequest request) throws IOException {
//        outObj.writeObject(request);
//        byte[] serializedObject = bos.toByteArray();
//        out.write(serializedObject);
//    }
//
//    private HttpResponse readResponse() throws IOException, ClassNotFoundException {
//        byte[] buffer = new byte[1024];
//        int length = in.read(buffer);
//        if(length == -1)return null;
//        byte[] response = new byte[length];
//        System.arraycopy(buffer, 0,response,0, length);
//        inObj = new ObjectInputStream(new ByteArrayInputStream(response));
//        return (HttpResponse) inObj.readObject();
//    }
    private static class ListenThread extends Thread {
        private Socket listener;
        private BufferedWriter listenOut;
        private BufferedReader listenIn;
        private ByteArrayOutputStream bos;
        private ObjectOutputStream outObj;
        private ObjectInputStream inObj;
        private String username;
        private Client client;

        public ListenThread(Client client) throws IOException {
            this.client = client;
            this.listener = new Socket("localhost", 8080);
            System.out.println("listener running at " + this.listener.getLocalPort());
            this.listenIn = new BufferedReader(new InputStreamReader(listener.getInputStream()));
            this.listenOut = new BufferedWriter(new OutputStreamWriter(listener.getOutputStream()));
            print("LISTEN\n"+client.username);
        }


        private void print(String message) throws IOException {
            listenOut.write(message);
            listenOut.newLine();
            listenOut.flush();
        }

        public void run() {
            try {
//                Timer timer = new Timer();
//                timer.scheduleAtFixedRate(new TimerTask() {
//                    @Override
//                    public void run() {
//                        try {
//                            System.out.println("run timer");
//                            if(listener.isClosed()){
//                                System.out.println("server closed!");
//                                listener.close();
//                                client.socket.close();
//                                System.exit(1);
//                            }
//                        } catch (IOException e) {
//
//                        }
//                    }
//                }, 1000, 1000);

                while(true){
                    String op = listenIn.readLine();
                    if(op!=null) System.out.println("listen port read" + op);
                    switch(op){
                        case "/createPrivateChat":
                            System.out.println("1");
                            String other = listenIn.readLine();//和谁创建聊天
                            System.out.println(other);
                            Platform.runLater(()->{
                                this.client.controller.chatObj.add(other);
                                ObservableList<Message> add = FXCollections.observableArrayList();
                                this.client.chatMap.put(other,add);
                            });
                            break;
                        case "/receivePrivateMessage":
                            System.out.println("in receive");
                            Message oriMessage = deserialize(listenIn.readLine());
                            System.out.println(oriMessage);
                            String chatId = oriMessage.getSentBy();
                            System.out.println(chatId);
                            Platform.runLater(()->{
                                client.chatMap.get(chatId).add(oriMessage);
                            });
                            break;
                        case "/createGroupChat":
                            String chatName = listenIn.readLine();
                            System.out.println("listen chat name is : " + chatName);

                            Platform.runLater(()->{
                                this.client.controller.chatObj.add(chatName);
                                ObservableList<Message> add2 = FXCollections.observableArrayList();
                                this.client.chatMap.put(chatName,add2);
                            });

                            break;
                        case "/receiveGroupMessage":
                            Message message = deserialize(listenIn.readLine());
                            System.out.println(message);
                            Platform.runLater(()->{
                                client.chatMap.get(message.getSendTo()).add(message);
                            });
                            break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (NullPointerException e){
                System.out.println("server is done!");
                Platform.runLater(()->{
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText("服务器崩啦");
                    ButtonType buttonTypeOK = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
                    alert.getButtonTypes().setAll(buttonTypeOK);
                    // 设置关闭事件的处理器
                    alert.setOnCloseRequest(event -> {
                        if (alert.getResult() == buttonTypeOK) {
                           System.exit(1);
                        }
                    });
                    alert.showAndWait();
                });
//                System.exit(1);
            }

//            while (true) {
//                try {
//                    String op = in.readLine();
//                    switch (op) {
//                        case "/message":
//                            int lines = Integer.parseInt(in.readLine());
//                            StringBuilder message = new StringBuilder("");
//                            for (int i = 0; i < lines; i++) {
//                                message.append(in.readLine());
//                                message.append("\n");
//                            }
//                    }
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
        }
    }

}
