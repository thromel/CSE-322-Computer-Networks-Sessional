package Server;

import java.io.*;
import java.net.*;
import java.util.*;

import Utils.*;
import Utils.File;

public class Server {
    private static final int PORT = 8818;
    private static final int MAX_BUFFER_SIZE = 100000000;
    private static final int MAX_CHUNK_SIZE = 100;
    private static final int MIN_CHUNK_SIZE = 10;

    private static HashSet<String> users = new HashSet<>();
    private static ServerSocket listener;
    private static HashMap<String, ObjectOutputStream> oos = new HashMap<>();
    private static HashMap<String, ObjectOutputStream> fos = new HashMap<>();
    private static HashMap<String, String> ip = new HashMap<>();
    private static HashMap<String, ArrayList<File>> files = new HashMap<>();
    private static HashMap<String, ArrayList<Request>> requests = new HashMap<>();
    private static HashMap<String, ArrayList<Message>> messages = new HashMap<>();

    private static int BUFFER_USED = 0;
    private static int total_requests;

    public static void main(String[] args) {
        System.out.println("Server running . . .");

        FileHandler fileHandler = new FileHandler(PORT, false);
        Thread messageThread = new Thread(fileHandler);
        messageThread.start();

        FileHandler fileHandler1 = new FileHandler(PORT+1, true);
        Thread fileThread = new Thread(fileHandler1);
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

    public static HashSet<String> getAllUsers(){
        return users;
    }

    public static Set<String> getOnlineUsers() {
        return oos.keySet();
    }

    public static synchronized int getBufferUsed() {
        return BUFFER_USED;
    }

    public static synchronized boolean useBuffer(int size) {
        if (size > MAX_BUFFER_SIZE - BUFFER_USED) {
            return false;
        }
        BUFFER_USED = BUFFER_USED + size;
        return true;
    }

    public static synchronized void freeBuffer(int size){
        BUFFER_USED = BUFFER_USED - size;
    }

    public static int getChunkSize(){
        Random rand = new Random();
        return rand.nextInt(MIN_CHUNK_SIZE, MAX_CHUNK_SIZE);
    }

    public static void addFile(String user, File file){
        if (files.get(user) == null){
            files.put(user, new ArrayList<File>());
        }
        files.get(user).add(file);
    }

    public static ArrayList<File> getFileList(String user){
        if (files.get(user) == null){
            files.put(user, new ArrayList<File>());
        }
        return files.get(user);
    }

    public static ArrayList<File> getPublicFileList(String user){
        ArrayList<File> files = getFileList(user);
        files.removeIf(file -> !file.isPublic());
        return files;
    }

    public static void addRequest (String user, Request request){
        if (requests.get(user) == null){
            requests.put(user, new ArrayList<Request>());
        }
        request.setNumber(++total_requests);
        requests.get(user).add(request);

        Message message = new Message();
        message.setText("File requested by " + user + ": " + request.getDescription());
        message.setType(MessageType.BROADCAST);

        for(ObjectOutputStream stream : oos.values()){
            try {
                stream.writeObject(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void removeRequest (String user, int requestNumber){
        requests.get(user).removeIf(request -> request.getNumber() == requestNumber);
    }
}

class FileHandler implements Runnable{

    private int PORT;
    private ServerSocket listener;
    private boolean socketType;

    public FileHandler(int PORT, boolean socketType) {
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

        while (socket.isConnected()){
            try {
                Message msg = receiveMsg();
                handleCommand(msg);
            } catch (IOException e) {
                System.err.println("Connection lost with user: " + user);
                Server.removeStream(user);
                Server.removeFileStream(user);
                Server.removeIP(user);
                break;

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
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

    private void handleFileUploadRequest(Message msg){
        File file = msg.getFile();


        //Handles file request
        //if buffer available, accept
        //else, reject

        if (Server.useBuffer(file.getFileSize())){
            msg.setType(MessageType.FILE_ACCEPT);
            msg.getFile().setCHUNK_SIZE(Server.getChunkSize());
            msg.setText("File upload request accepted.\nChunk Size: " + msg.getFile().getCHUNK_SIZE());
        } else {
            msg.setType(MessageType.BUFFER_FULL);
        }
        sendMsg(msg);

        //

    }

    private void sendMsg (Message msg){
        try {
            objectOutputStream.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleCommand(Message msg){
        Message response = new Message();

        if (msg == null) {
            System.err.println("NULL Message");
        }
//
        if (msg.getType() == MessageType.LOOKUP_STD){
            response = getStudentList();
            try {
                objectOutputStream.writeObject(response);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (msg.getType() == MessageType.FILE_REQUEST) {
            handleFileUploadRequest(msg);
        } else if (msg.getType() == MessageType.FILE_INIT && socketType) {
            handleFileUpload(msg);
        } else if (msg.getType() == MessageType.LOOKUP_FILES_SELF) {
            response.setText(Server.getFileList(user).toString());
            response.setType(MessageType.FILE_LIST);
            try {
                objectOutputStream.writeObject(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (msg.getType() == MessageType.LOOKUP_FILES_OTHER) {
            response.setText("Files uploaded by " + msg.getText() + ":\n" + Server.getPublicFileList(msg.getText()).toString());
            response.setType(MessageType.SERVER_RESPONSE);

            try {
                objectOutputStream.writeObject(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (msg.getType() == MessageType.REQUEST) {
            response.setText("Request saved and sent to all other online students ");
            response.setType(MessageType.SERVER_RESPONSE);
            Server.addRequest();
        } else {

            response.setText("Couldn't parse the request!");
            response.setType(MessageType.ERROR);
            try {
                objectOutputStream.writeObject(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Message getStudentList(){
        String txt = "Users who logged in at least once:\n";
        Message msg = new Message();
        txt += Server.getAllUsers().toString() + "\n";
        txt += "Users online:\n";
        txt += Server.getOnlineUsers().toString() + "\n";
        msg.setText(txt);
        return msg;
    }

    private void handleFileUpload(Message msg){
        ArrayList<Chunk> chunks = new ArrayList<>();
        Message response = new Message();
        File file = msg.getFile();
        System.out.println(file);
        if (msg.getType() == MessageType.FILE_INIT){
            while(socket.isConnected()){
                try {
                    Chunk chunk = (Chunk) objectInputStream.readObject();

                    response.setType(MessageType.CHUNK_RECEIVED);
                    objectOutputStream.writeObject(response);

                    chunks.add(chunk);
                    if (chunk.getChunkNo() == chunk.getTotalChunks()) {
                        response.setText("File upload completed!");
                        response.setType(MessageType.FILE_UL_COMPLETE);
                        objectOutputStream.writeObject(response);
                        System.out.println("File upload completed!");

                        Chunk[] chunkArray = chunks.toArray(new Chunk[chunks.size()]);
                        byte[] bytes = file.join(chunkArray);
                        Server.addFile(file.getOwner(), file);
                        file.save(bytes);

                        break;
                    }


                } catch (IOException e) {
                    System.err.println("Error while getting file from user");
                    e.printStackTrace();
                    break;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

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

    private Message receiveMsg() throws IOException, ClassNotFoundException {
        Message message = (Message) objectInputStream.readObject();
        return message;
    }

}

