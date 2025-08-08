import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class FileEncryptor {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        String inputDir = "input";
        // Check if input directory exists and has files
        if (!Files.exists(Paths.get(inputDir)) ||
                Files.list(Paths.get(inputDir)).filter(Files::isRegularFile).findAny().isEmpty()) {
            System.err.println("[Encrypt] No files found in 'input' directory. Aborting.");
            return;
        }
        String password = askForPassword(scanner);
        while (password.length() != 16) {
            System.out.println("[Encrypt] Invalid password length. Please enter a 16-character password.");
            password = askForPassword(scanner);
        }
        String zipFile = "input.zip";
        compressDirectoryToZip(inputDir, zipFile);
        printFileHash(zipFile);
        byte[] encrypted = encryptFile(zipFile, password);
        base64EncodeAndSplit(encrypted, 100);
        deleteZipFile(zipFile);
    }

    private static String askForPassword(Scanner scanner) {
        System.out.print("[Encrypt] Enter new password (16 bytes): ");
        String password = scanner.nextLine();
        if (password.length() != 16) {
            System.out.println("[Encrypt] Invalid password. Please enter a 16 bytes password.");
            return askForPassword(scanner); // Recursive call to ask again
        }
        return password;
    }

    private static void compressDirectoryToZip(String inputDir, String zipFile) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(
                Files.newOutputStream(Paths.get(zipFile)))) {
            Files.walk(Paths.get(inputDir))
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            ZipEntry zipEntry = new ZipEntry(
                                    Paths.get(inputDir).relativize(path).toString());
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    private static void printFileHash(String zipFile) {
        try {
            // Print SHA-256 hash of the zip file
            byte[] zipBytes = Files.readAllBytes(Paths.get(zipFile));
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(zipBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            System.out.println("[Encrypt] SHA-256 hash of zip file: " + sb.toString());
        } catch (Exception e) {
            System.err.println("[Encrypt] Failed to compute SHA-256 hash: " + e.getMessage());
        }
    }

    private static void deleteZipFile(String zipFile) throws Exception {
        Files.deleteIfExists(Paths.get(zipFile));
    }

    private static byte[] encryptFile(String filePath, String keyStr) throws Exception {
        byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
        SecretKey key = new SecretKeySpec(keyStr.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(fileBytes);
    }

    private static void base64EncodeAndSplit(byte[] encrypted, int chunkSizeMB) throws Exception {
        byte[] base64 = Base64.getEncoder().encode(encrypted);
        int chunkSize = chunkSizeMB * 1000 * 1000;
        int totalChunks = (base64.length + chunkSize - 1) / chunkSize;
        // Ensure input directory exists
        Files.createDirectories(Paths.get("input"));
        for (int i = 0; i < totalChunks; i++) {
            int start = i * chunkSize;
            int end = Math.min(base64.length, (i + 1) * chunkSize);
            byte[] chunk = Arrays.copyOfRange(base64, start, end);
            Files.write(Paths.get("input", (i + 1) + ".txt"), chunk);
        }
    }
}
