package Client;
import Server.Observable;
import Server.Stock;

public interface Observer {
    void setObservable(Observable observable);
    void update(Stock stock);
}
