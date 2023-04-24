package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.HttpRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Test {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 8080);

        OutputStream os = socket.getOutputStream();

        HttpRequest test = new HttpRequest();
        test.setMethod("GET");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
//        out.writeObject(test);
//        byte[] serializedObject = bos.toByteArray();
//        os.write(serializedObject);


        //查询是否有相同username
        HttpRequest checkUsername = new HttpRequest();
        checkUsername.setMethod("POST");
        checkUsername.setUrl("/checkUsername");
        Map<String, String> param = new HashMap<>();
        param.put("username", "abc");
        checkUsername.setParams(param);

        out.writeObject(checkUsername);
        byte[] serializedObject = bos.toByteArray();
        os.write(serializedObject);



//        byte[] msg = "Hello server!".getBytes();
//        os.write(msg);
//        System.out.println("Client's message sent");
        os.close();


    }
}
