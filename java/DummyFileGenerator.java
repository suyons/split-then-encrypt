import java.io.File;
import java.io.FileOutputStream;
import java.util.Scanner;

public class DummyFileGenerator {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        long fileSize = askForFileSize(scanner);
        String fileName = askForFileName(scanner);
        File dummyFile = createDummyFile(fileSize, fileName);
        System.out.println("[Generator] Dummy file created: " + dummyFile.getAbsolutePath());
    }

    private static long askForFileSize(Scanner scanner) {
        System.out.print("[Generator] Enter desired file size in bytes: ");
        long fileSize = scanner.nextLong();
        scanner.nextLine(); // Consume the leftover newline
        if (fileSize <= 0) {
            System.out.println("[Generator] Invalid file size. Please enter a positive number.");
            return askForFileSize(scanner); // Recursive call to ask again
        }
        return fileSize;
    }

    private static String askForFileName(Scanner scanner) {
        System.out.print("[Generator] Enter desired file name: ");
        String fileName = scanner.nextLine();
        if (fileName.isEmpty()) {
            System.out.println("[Generator] Invalid file name. Please enter a non-empty name.");
            return askForFileName(scanner); // Recursive call to ask again
        }
        return fileName;
    }

    private static File createDummyFile(long size, String name) throws Exception {
        File outputDir = new File("input");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        File dummyFile = new File(outputDir, name);
        if (dummyFile.exists()) {
            dummyFile.delete(); // Remove existing file if it exists
        }
        try (FileOutputStream fos = new FileOutputStream(dummyFile)) {
            byte[] buffer = new byte[1024];
            long remaining = size;
            while (remaining > 0) {
                int bytesToWrite = (int) Math.min(buffer.length, remaining);
                fos.write(buffer, 0, bytesToWrite);
                remaining -= bytesToWrite;
            }
        }
        return dummyFile;
    }
}
