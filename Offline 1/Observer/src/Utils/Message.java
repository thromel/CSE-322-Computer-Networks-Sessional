package Utils;

import java.io.Serializable;

public class Message implements Serializable {
    public MessageType type;
    private String user;
    private File file;

    public Message(MessageType type, String user) {
        this.type = type;
        this.user = user;
    }

    public Message() {
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
}
