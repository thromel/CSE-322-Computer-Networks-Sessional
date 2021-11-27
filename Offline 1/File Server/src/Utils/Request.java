package Utils;

import java.io.Serializable;

public class Request implements Serializable {
    private static final long serialVersionUID = 4L;
    private String description;
    private int number;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }
}
