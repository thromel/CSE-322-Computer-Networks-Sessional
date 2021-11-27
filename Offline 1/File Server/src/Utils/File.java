package Utils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Arrays;

public class File implements Serializable {
    private String fileName;
    private String owner;
    private boolean isPublic;
    private int CHUNK_SIZE;
    private int fileSize;
    private static final long serialVersionUID = 2L;

    public File(String fileName, String owner, boolean isPublic, int CHUNK_SIZE) {
        this.fileName = fileName;
        this.owner = owner;
        this.isPublic = isPublic;
        this.CHUNK_SIZE = CHUNK_SIZE;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public int getCHUNK_SIZE() {
        return CHUNK_SIZE;
    }

    public void setCHUNK_SIZE(int CHUNK_SIZE) {
        this.CHUNK_SIZE = CHUNK_SIZE;
    }

    public File() {
    }

    public byte[] load (String directory) {
        byte[] array = new byte[0];
        try {
            array = Files.readAllBytes(Paths.get(directory));
        } catch (NoSuchFileException d){

        } catch (IOException e ) {
            System.err.println("Error while reading the file");
            e.printStackTrace();
        }
        fileSize = array.length;
        System.err.println("Total filesize: " + fileSize);
        return array;
    }

    public void save (byte[] bytes){
        mkdir(owner);
        try (FileOutputStream stream = new FileOutputStream("files/%s/%s".formatted(owner, fileName))) {
            stream.write(bytes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.err.println("File not found");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("IO Exception");
        }
    }

    public Chunk[] split(byte[] array){
        ByteBuffer bb = ByteBuffer.wrap(array);
        int chunkNo=0;
        int totalChunks = (int) Math.ceil(fileSize*1.0/CHUNK_SIZE);
        Chunk[] chunkArray = new Chunk[totalChunks];

        int start = 0;

        for(int i = 0; i < totalChunks; i++) {
            byte[] ret = new byte[Math.min(CHUNK_SIZE, array.length - i*CHUNK_SIZE)];
            ret = Arrays.copyOfRange(array, start, start + Math.min(CHUNK_SIZE, array.length - i*CHUNK_SIZE));
            start += CHUNK_SIZE ;
            chunkArray[i]=new Chunk(ret, i+1,totalChunks,Math.min(CHUNK_SIZE, array.length - i*CHUNK_SIZE));
        }

        return chunkArray;
    }


    public byte[] join (Chunk[] chunkArray){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for(int i = 0; i < chunkArray.length; i++){
            try {
                stream.write(chunkArray[i].getBytes());
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Error while joining chunks");
            }
        }
        return stream.toByteArray();
    }

    public static void mkdir (String dirName){
        boolean file = new java.io.File("files/"+dirName).mkdirs();

    }

    public int getFileSize() {
        return fileSize;
    }

    @Override
    public String toString() {
        return "File{" +
                "fileName='" + fileName + '\'' +
                ", owner='" + owner + '\'' +
                ", isPublic=" + isPublic +
                ", CHUNK_SIZE=" + CHUNK_SIZE +
                ", fileSize=" + fileSize +
                "}\n";
    }
}
