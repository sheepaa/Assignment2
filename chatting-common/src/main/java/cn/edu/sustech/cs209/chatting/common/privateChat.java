package cn.edu.sustech.cs209.chatting.common;

public class privateChat {
    private String sendTo;
    private String sendBy;

    public privateChat(String sendTo, String sendBy) {
        this.sendTo = sendTo;
        this.sendBy = sendBy;
    }

    public String getSendTo() {
        return sendTo;
    }

    public void setSendTo(String sendTo) {
        this.sendTo = sendTo;
    }

    public String getSendBy() {
        return sendBy;
    }

    public void setSendBy(String sendBy) {
        this.sendBy = sendBy;
    }
}
