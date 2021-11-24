package Server;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

import Utils.File;
import Utils.Message;
import Utils.MessageType;

public class Server {
    private static final int PORT = 8818;
    private static ArrayList<String> users = new ArrayList<>();
    private static ServerSocket listener;
    private static HashMap<String, ObjectOutputStream> oos = new HashMap<>();
    private static HashMap<String, ObjectOutputStream> fos = new HashMap<>();
    private static HashMap<String, String> ip = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("Server running . . .");

        Listener listener = new Listener(PORT, false);
        Thread messageThread = new Thread(listener);
        messageThread.start();

        Listener listener1 = new Listener(PORT+1, true);
        Thread fileThread = new Thread(listener1);
        fileThread.start();
    }

    public static void addUser(String user){
        users.add(user);
    }

    public static void addStream(ObjectOutputStream objectOutputStream, String user){
        oos.put(user, objectOutputStream);
    }

    public static void removeStream(String user){
        oos.remove(user);
    }

    public static boolean findUser(String user){
        return oos.containsKey(user);
    }
    public static ObjectOutputStream getStream(String user){
        return oos.get(user);
    }

    public static String getIP (String user){
        return ip.get(user);
    }

    public static void addIP (String user, String IP){
        ip.put(user, IP);
    }

    public static void removeIP (String user){
        ip.remove(user);
    }

    public static void addFileStream(ObjectOutputStream objectOutputStream, String user){
        fos.put(user, objectOutputStream);
    }

    public static void removeFileStream(String user){
        fos.remove(user);
    }


}

class Listener implements Runnable{

    private int PORT;
    private ServerSocket listener;
    private boolean socketType;

    public Listener(int PORT, boolean socketType) {
        this.PORT = PORT;
        this.socketType = socketType;
    }

    @Override
    public void run() {
        if (socketType) System.out.println("File Server running at PORT: " + PORT);
        else System.out.println("Message server running at PORT: " + PORT);

        try {
            listener = new ServerSocket(PORT);
            System.out.println("Started the serverSocket");
        } catch (IOException e){
            System.err.println("Unable to start the server");
        }

        try{
            while (true){
                Handler serverHandler = new Handler(listener.accept(), socketType);
                Thread serverThread = new Thread(serverHandler);
                serverThread.start();
            }
        } catch (IOException e){
            System.err.println("Unable to listen");
        } finally {
            try {
                listener.close();
            } catch (IOException e){
                System.err.println("Unable to close the serverThread");
            }
        }
    }
}

class Handler implements Runnable{
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private String user;
    private boolean socketType;

    public Handler(Socket accept, boolean socketType) {
        this.socket = accept;
        this.socketType = socketType;
    }

    @Override
    public void run() {
        initConnection();
        if (!socketType) handleLogin();
        else handleFSLogin();

//        while (socket.isConnected()){
//            handleCommand(receiveMsg());
//        }
    }

    private void handleLogin(){
        try {
            Message loginRequest = (Message) objectInputStream.readObject();
            this.user = loginRequest.getUser();
            InetSocketAddress sockaddr = (InetSocketAddress) socket.getRemoteSocketAddress();
            InetAddress inaddr = sockaddr.getAddress();
            Inet4Address in4addr = (Inet4Address)inaddr;
            byte[] ip4bytes = in4addr.getAddress(); // returns byte[4]
            String ip4string = in4addr.toString();
            System.out.println(user + ", " + ip4string);

            if (loginRequest.getType() == MessageType.LOGIN_REQUEST){
                if (Server.findUser(user) && !Server.getIP(user).equals(ip4string)){
                    System.err.println(user+ " already logged in from another IP");
                    loginRequest.setType(MessageType.LOGIN_FAIL);
                    objectOutputStream.writeObject(loginRequest);
                } else {
                    System.out.println(user+ " just logged in!");
                    loginRequest.setType(MessageType.LOGIN_OK);
                    File.mkdir(user);
                    objectOutputStream.writeObject(loginRequest);

                    Server.addIP(user, ip4string);
                    Server.addStream(objectOutputStream, user);
                    Server.addUser(user);

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void handleFSLogin(){
        Server.addFileStream(objectOutputStream, user);
        System.out.println("Logged into file server");
    }

    private void handleCommand(Message msg){

        if (msg == null) {
            System.err.println("NULL Message");
        }
//        String token[] = msg.getMessage().split(" ");

//        if (token[0].equalsIgnoreCase("S")){
//            stockPlatform.addObserver(token[1], this.user);
//        } else if (token[0].equalsIgnoreCase("U")){
//            stockPlatform.removeObserver(token[1],this.user);
//        } else if (token[0].equalsIgnoreCase("I") && this.user.isAdmin()){
//            stockPlatform.incPrice(token[1], Double.parseDouble(token[2]));
//        } else if (token[0].equalsIgnoreCase("D") && this.user.isAdmin()){
//            stockPlatform.decPrice(token[1], Double.parseDouble(token[2]));
//        } else if (token[0].equalsIgnoreCase("C") && this.user.isAdmin()){
//            stockPlatform.setQuantity(token[1], Integer.parseInt(token[2]));
//        }  else {
//            sendMsg("Invalid command");
//        }
    }



    private void initConnection(){
        try {
            System.out.println("Attempting to connect to an user . . .");
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            objectInputStream = new ObjectInputStream(inputStream);
            objectOutputStream = new ObjectOutputStream(outputStream);


            System.out.println("User connected");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private Message receiveMsg(){
        try {
            Message message = (Message) objectInputStream.readObject();
            return message;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException classNotFoundException) {
            classNotFoundException.printStackTrace();
        }
        return new Message();
    }
}

