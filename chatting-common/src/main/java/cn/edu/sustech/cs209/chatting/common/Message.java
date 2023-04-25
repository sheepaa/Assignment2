package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable {

    private Long timestamp;

    private String sentBy;

    private String sendTo;

    private String data;
    private static final long serialVersionUID = 123456789L;

    //文件传输
    private boolean isFile;
    private String fileName;
    private UUID UUID;

    public boolean isFile() {
        return isFile;
    }

    public void setFile(boolean file) {
        isFile = file;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public UUID getUUID() {
        return UUID;
    }

    public void setUUID(UUID UUID) {
        this.UUID = UUID;
    }


    public Message(Long timestamp, String sentBy, String sendTo, String data) {
        this.timestamp = timestamp;
        this.sentBy = sentBy;
        this.sendTo = sendTo;
        this.data = data;
        this.isFile = false;//默认为不是file信息
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getSentBy() {
        return sentBy;
    }

    public boolean test() {
        System.out.println("compile???");
        return false;
    }

    @Override
    public String toString() {
        return "Message{" +
                "timestamp=" + timestamp +
                ", sentBy='" + sentBy + '\'' +
                ", sendTo='" + sendTo + '\'' +
                ", data='" + data + '\'' +
                '}';
    }

    public String getSendTo() {
        return sendTo;
    }

    public String getData() {
        return data;
    }
}
