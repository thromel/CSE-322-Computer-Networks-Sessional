package Utils;

public class Chunk {
    private byte[] bytes;
    private int chunkNo;
    private int totalChunks;
    private int chunkSize;

    public Chunk(byte[] bytes, int chunkNo, int totalChunks, int chunkSize) {
        this.bytes = bytes;
        this.chunkNo = chunkNo;
        this.totalChunks = totalChunks;
        this.chunkSize = chunkSize;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public int getChunkNo() {
        return chunkNo;
    }

    public void setChunkNo(int chunkNo) {
        this.chunkNo = chunkNo;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    @Override
    public String toString() {
        return "Chunk{" +
                "chunkNo=" + chunkNo +
                ", totalChunks=" + totalChunks +
                ", chunkSize=" + chunkSize +
                '}';
    }
}
