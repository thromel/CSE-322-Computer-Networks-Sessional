package Server;

import Client.Observer;

public interface Observable {
    void addObserver (String stockName, Observer observer);

    void removeObserver (String stockName, Observer observer);

    void notifyObserver(String stockName);

}
