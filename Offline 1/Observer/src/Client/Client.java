package Client;

import Utils.Message;
import Utils.MessageType;

import java.io.*;
import java.net.Socket;

public class Client implements Runnable{
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private String hostname;
    private int PORT;
    private String user;
    private boolean socketType;

    public Client(String hostname, int PORT, boolean socketType, String userName) {
        this.hostname = hostname;
        this.PORT = PORT;
        this.socketType = socketType;
        this.user = userName;
    }
    @Override
    public void run() {
        try {

            initConnection();
            if (!socketType) login();


            while (socket.isConnected()){
                Message msg = null;
                try {
                    msg =  (Message) objectInputStream.readObject();
//                    System.out.println(msg.getMessage());
                } catch (EOFException | ClassNotFoundException e){
                    e.printStackTrace();
                }
            }

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void initConnection(){
        try {
            socket = new Socket(hostname,PORT);
            outputStream = socket.getOutputStream();
            objectOutputStream = new ObjectOutputStream(outputStream);
            inputStream = socket.getInputStream();
            objectInputStream = new ObjectInputStream(inputStream);

            if (socketType) System.out.println("Established connection with the file socket");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void login(){
        Message message = new Message(MessageType.LOGIN_REQUEST, user);
        try {
            objectOutputStream.writeObject(message);
        } catch (IOException e) {
            System.err.println("Unable to send login request");
        }

        try {
            Message message1 = (Message) objectInputStream.readObject();

            if (message1.getType() == MessageType.LOGIN_OK){
                System.out.println("Logged in successfully!");

                Client client = new Client("localhost",PORT+1, true, user);
                Thread t = new Thread(client);
                t.start();

            } else {
                System.err.println("Login failed! Terminating connection with server");
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }




    }

}
