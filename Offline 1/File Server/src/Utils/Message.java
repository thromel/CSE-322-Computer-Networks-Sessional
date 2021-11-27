package Utils;

import java.io.Serializable;

public class Message implements Serializable {
    public MessageType type;
    private String user;
    private File file;
    private String text;
    private Request request;
    private static final long serialVersionUID = 1L;
    private boolean isRead;

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", user='" + user + '\'' +
                ", text='" + text + '\'' +
                '}';
    }

    public Message(MessageType type, String user) {
        this.type = type;
        this.user = user;
    }

    public Message() {
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }
}
