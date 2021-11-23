package Utils;

public class UtilsTest {
    public static void main(String[] args) {
        File file = new File();
        byte[] array = file.load("thejoyofcryptography.pdf");
        file.setFileName("thejoyofcryptography.pdf");
        file.setOwner("Tanzim");
        file.setCHUNK_SIZE(6969);
        Chunk[] chunks = file.split(array);
        array = file.join(chunks);
        System.out.println("Total chunks: " + chunks.length);
        file.save(array);
    }
}
