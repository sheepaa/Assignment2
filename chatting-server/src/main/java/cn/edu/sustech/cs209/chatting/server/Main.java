package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.HttpRequest;
import cn.edu.sustech.cs209.chatting.common.HttpResponse;
import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Main {
    //    private static List<ClientThread> clients = new ArrayList<>();
    private static Map<String, ClientThread> clients = new HashMap<>();
//    private static Map<>

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        try {
            ServerSocket serverSocket = new ServerSocket(8080);
            System.out.println("Chat server is running on port " + "8080");

            while (true) {
                // 等待新的客户端连接
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String identifier = in.readLine();
                if("REQUEST".equals(identifier)){
                    // 创建一个新的线程来处理客户端请求
                    System.out.println("Get request socket");
                    ClientThread clientThread = new ClientThread(clientSocket);
                    clientThread.start();
                }else if("LISTEN".equals(identifier)){
                    String username = in.readLine();
                    System.out.println("Get listen socket for" + username);
                    clients.get(username).setListenSocket(clientSocket);
                }

            }
        } catch (IOException e) {
            System.out.println("Error starting chat server: " + e.getMessage());
        }

    }
    private static class ClientThread extends Thread {
        private Socket clientSocket;
        private BufferedWriter out;
        private BufferedReader in;
        private Socket listenSocket;
        private BufferedWriter listenOut;
        private BufferedReader listenIn;
        private ByteArrayOutputStream bos;
        private ObjectOutputStream outObj;
        private ObjectInputStream inObj;
        private String username ;
        private Thread heartBeatSender;

        public ClientThread(Socket clientSocket) throws IOException {
            this.clientSocket = clientSocket;
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        }
        public void setListenSocket(Socket socket) throws IOException {
            this.listenSocket = socket;
            this.listenIn = new BufferedReader(new InputStreamReader(listenSocket.getInputStream()));
            this.listenOut = new BufferedWriter(new OutputStreamWriter(listenSocket.getOutputStream()));


        }


        private void print(String message) throws IOException {
            out.write(message);
            out.newLine();
            out.flush();
        }
        private synchronized void printToListen(String message) throws IOException {
            listenOut.write(message);
            listenOut.newLine();
            listenOut.flush();
        }
        private Message deserialize(String str) throws IOException, ClassNotFoundException {
            byte[] bytes = Base64.getDecoder().decode(str.getBytes());
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            Message message = (Message) ois.readObject();
            ois.close();
            return message;
        }
        private  String serialize(Message message) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(message);
            oos.close();
            return new String(Base64.getEncoder().encode(baos.toByteArray()));
        }


        public void run() {
            try {
                // 处理客户端请求
                while (true) {
                    String op = in.readLine();
                    switch (op) {
                        case "/checkUsername":
                            System.out.println("in check user name");
                            String username = in.readLine();
                            if (clients.containsKey(username)) {
                                print("409");
//                                out.println("409");//冲突，让改名
                            } else {
                                clients.put(username, this);
                                System.out.println("put username");
                                this.username = username;
                                print("200");
//                                out.println("200");
                            }
                            break;
                        case "/getAllRecords"://如果有聊天记录（True），依次发给客户端，如果没有（False），则创建该用户的文件夹
                            File records = new File("chatting-server/src/main/database/" + this.username);
                            if(!records.exists()){
                                System.out.println("mkdir");
                                boolean isFinish = records.mkdirs();
                                print("False");
                                System.out.println(isFinish);
                            }else{
                                print("True");
                                File[] files = records.listFiles();
                                if (files != null) {
                                    print(Integer.toString(files.length));//告诉client有多少个
                                    for (File file : files) {
                                        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                                            print(file.getName().substring(0, file.getName().length()-4));//会话名字
                                            String line;
                                            while ((line = reader.readLine()) != null) {
                                                // 读取文件内容并处理
                                                print(line);
                                                System.out.println(line);
                                            }
                                            print("end");
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }else{
                                    print("0");
                                }
                            }
//                            File recordsFiles = new File(Objects.requireNonNull(classLoader.getResource("database")).toURI());
//                            File file = new File(Objects.requireNonNull(classLoader.getResource("")).getFile());
                            break;
                        case "/getAllUsers":
                            System.out.println("in getallusers");
                            print(Integer.toString(clients.size()-1));
                            StringBuilder keys = new StringBuilder("");
                            for (String key : clients.keySet()) {
                                if (key.equals(this.username)) continue;
                                keys.append(key + "\n");
                                System.out.println("server prints : " + key);
                            }
                            out.write(keys.toString());
                            out.flush();
                            break;
                        case "/createPrivateChat":
                            System.out.println("in create");
                            String other = in.readLine();
                            System.out.println(other);
                            ClientThread otherClient = clients.get(other);
                            otherClient.printToListen("/createPrivateChat\n"+this.username);
                            //在username文件夹下创建other的文件，在other文件夹下创建username文件
                            File newFile = new File("chatting-server/src/main/database/"+this.username + "/" + other + ".txt");
                            newFile.createNewFile();
                            File newFile2 = new File("chatting-server/src/main/database/" + other + "/" + this.username + ".txt");
                            newFile2.createNewFile();
                            break;
                        case "/sendMessage":
                            String strMessage = in.readLine();
                            Message oriMessage = deserialize(strMessage);
                            String[] split = oriMessage.getSendTo().split(",");
                            if(split.length == 1){//是私聊
                                ClientThread clientThread = clients.get(oriMessage.getSendTo());
                                clientThread.printToListen("/receivePrivateMessage");
                                clientThread.printToListen(serialize(oriMessage));
                                //在username文件中的sendTo.txt中加记录，在sendTo文件夹中的username.txt中加记录
                                PrintWriter writer = new PrintWriter(new FileWriter("chatting-server/src/main/database/"+this.username + "/" + oriMessage.getSendTo() + ".txt",true));
                                writer.println(strMessage);
                                writer.close();
                                PrintWriter writer1 = new PrintWriter(new FileWriter("chatting-server/src/main/database/" + oriMessage.getSendTo() + "/" + this.username + ".txt",true));
                                writer1.println(strMessage);
                                writer1.close();
                            }else{//是群聊
                                String chatName = oriMessage.getSendTo();
                                String[] users = chatName.split(",");
                                String sendBy = oriMessage.getSentBy();
                                for (int i = 0; i < users.length; i++) {
                                    //要剔除的是sendby的人
                                    //oriMessage不变，sendby是send信息的用户名，sendto是群聊名称
                                    if(users[i].equals(sendBy))continue;
                                    ClientThread clientThread = clients.get(users[i]);
                                    clientThread.printToListen("/receiveGroupMessage");
                                    clientThread.printToListen(serialize(oriMessage));
                                }
                            }
                            break;
                        case "/createGroupChat":
                            System.out.println("11111");
                            String chatName = in.readLine();
                            System.out.println("server : "+ chatName);
                            String[] users = chatName.split(",");
                            for (int i = 0; i < users.length-1; i++) {
                                String user = users[i];
                                clients.get(user).printToListen("/createGroupChat\n"+chatName);
                            }
                            break;
                        default:
                            break;
                    }
                }
            } catch (NullPointerException e) {
                System.out.println(username + " exit!");
                return;
            } catch (IOException | ClassNotFoundException  e){
                throw new RuntimeException(e);
            }
        }
    }
}
