package cn.edu.sustech.cs209.chatting.server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class test {



    public static void main(String[] args) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter("chatting-server/src/main/database/aaa"+  "/" + "bbb" + ".txt",true));
        writer.println("test");
        writer.flush();
        writer.close();
        PrintWriter writer1 = new PrintWriter(new FileWriter("chatting-server/src/main/database/aaa"+  "/" + "bbb" + ".txt",true));
        writer1.println("test1");
        writer1.flush();
        writer1.close();

//        writer.println("text2");
//        writer.flush();
    }
}
