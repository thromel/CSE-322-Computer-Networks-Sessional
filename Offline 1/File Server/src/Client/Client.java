package Client;

import Utils.*;
import Utils.File;

import java.io.*;
import java.net.Socket;
import java.nio.file.NoSuchFileException;
import java.util.Scanner;

class FileClient implements Runnable{
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private String hostname;
    private int PORT;
    private String user;
    private boolean socketType;
    private File file;
    private Chunk[] chunks;
    private boolean isUploadMode;

    public FileClient(String hostname, int PORT, boolean socketType, String userName, File file, Chunk[] chunks, boolean isUploadMode) {
        this.hostname = hostname;
        this.PORT = PORT;
        this.socketType = socketType;
        this.user = userName;
        this.file = file;
        this.chunks = chunks;
        this.isUploadMode = isUploadMode;
    }

    @Override
    public void run() {
        initConnection();

        if (isUploadMode){
            Message uploadInitMsg = new Message();
            uploadInitMsg.setType(MessageType.FILE_INIT);
            uploadInitMsg.setFile(file);
            try {
                objectOutputStream.writeObject(uploadInitMsg);

                while (socket.isConnected()){
                    for(int i = 0; i < chunks.length; i++){
                        objectOutputStream.writeObject(chunks[i]);
                        Message response = (Message) objectInputStream.readObject();

                        if (response.getType() == MessageType.CHUNK_RECEIVED) continue;
                        else if (response.getType() == MessageType.FILE_UL_COMPLETE && i == chunks.length-1){
                            System.out.println(response.getText());
                        } else if (response.getType() == MessageType.ERROR){
                            System.err.println("File upload failed");
                            break;
                        }
                    }
                    socket.close();
                    break;
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
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

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public ObjectInputStream getObjectInputStream() {
        return objectInputStream;
    }

    public void setObjectInputStream(ObjectInputStream objectInputStream) {
        this.objectInputStream = objectInputStream;
    }

    public ObjectOutputStream getObjectOutputStream() {
        return objectOutputStream;
    }

    public void setObjectOutputStream(ObjectOutputStream objectOutputStream) {
        this.objectOutputStream = objectOutputStream;
    }
}

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
    private Scanner sc = new Scanner(System.in);
    private byte[] bytes;

    public Client(String hostname, int PORT, boolean socketType, String userName) {
        this.hostname = hostname;
        this.PORT = PORT;
        this.socketType = socketType;
        this.user = userName;
    }
    @Override
    public void run() {

        initConnection();
        if (!socketType) login();

        while (socket.isConnected()){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Options:");
            System.out.println("1. Get list of students");
            System.out.println("2. Get list of files uploaded by other student");
            System.out.println("3. Get list of files uploaded by myself");
            System.out.println("4. Request for a file");
            System.out.println("5. Upload a file");
            System.out.println("6. View all unread messages");

            System.out.print("["+user+"@fileserver]$ ");
            Message msg = null;

            int choice = Integer.parseInt(sc.nextLine());
            msg = handleCommand(choice);

            try {
                objectOutputStream.writeObject(msg);
                msg =  (Message) objectInputStream.readObject();
                System.out.println(msg.getText());
                handleServerResponse(msg);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

    private void initConnection(){
        try {
            socket = new Socket(hostname,PORT);
            outputStream = socket.getOutputStream();
            objectOutputStream = new ObjectOutputStream(outputStream);
            inputStream = socket.getInputStream();
            objectInputStream = new ObjectInputStream(inputStream);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private FileClient initFileSocket(File file, Chunk[] chunks, boolean mode){
        FileClient fileClient = new FileClient("localhost",PORT+1, true, user, file, chunks, mode);
        Thread t = new Thread(fileClient);
        t.start();
        return fileClient;
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

    private Message handleCommand(int choice){
        System.out.println(165);
        Message msg = new Message();
        msg.setUser(user);
        if (choice == 1){
            msg.setType(MessageType.LOOKUP_STD);

        } else if (choice == 2){
            msg.setType(MessageType.LOOKUP_FILES_OTHER);
            System.out.println("Username: ");
            String userName = sc.nextLine();
            msg.setText(userName);
        } else if (choice == 3){
            msg.setType(MessageType.LOOKUP_FILES_SELF);
        } else if (choice == 4) {
            msg.setType(MessageType.FILE_REQUEST);
            System.out.println("Description for file: ");
            String description = sc.nextLine();
            Request request = new Request();
            request.setDescription(description);
            msg.setRequest(request);
        } else if (choice == 5){
            System.out.println("Filename: ");
            msg.setType(MessageType.FILE_REQUEST);
            File file = new File();
            String fileName = sc.nextLine();
            System.out.println("Public? Y/N");
            String privacy = sc.nextLine();

            if(privacy.equalsIgnoreCase("Y")){
                file.setPublic(true);
            } else {
                file.setPublic(false);
            }

            file.setOwner(user);
            file.setFileName(fileName);
            bytes = file.load(fileName);

            msg.setFile(file);
        }
        return msg;
    }

    private void handleServerResponse(Message msg){
        if (msg.getType() == MessageType.FILE_ACCEPT){
            Chunk[] chunks = msg.getFile().split(bytes);
            FileClient fileClient = initFileSocket(msg.getFile(), chunks, true);
        }
    }

}
