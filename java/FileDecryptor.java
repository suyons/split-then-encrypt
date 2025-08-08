import java.nio.file.*;
import java.util.Base64;
import java.io.ByteArrayOutputStream;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

import java.util.*;
import java.io.IOException;

public class FileDecryptor {
    public static void main(String[] args) throws Exception {
        List<Path> inputFiles = loadInputFiles("input").stream()
                .filter(p -> p.getFileName().toString().matches("\\d+\\.txt"))
                .toList();
        if (inputFiles.isEmpty()) {
            System.err.println("[Decrypt] No .txt files found in 'input' directory. Aborting.");
            return;
        }
        System.out.println("[Decrypt] Found files: " + inputFiles);

        byte[] concatenated = concatenateTxtFiles(inputFiles);
        System.out.println("[Decrypt] Concatenated txt file size: " + concatenated.length);

        String password = askForPassword();
        byte[] decrypted = decryptBase64Aes(concatenated, password);

        Path outputDir = Paths.get("output");
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        Files.write(outputDir.resolve("output.zip"), decrypted);
        System.out.println("[Decrypt] Decryption complete. Output: output/output.zip");
        printFileHash("output/output.zip");
    }

    private static List<Path> loadInputFiles(String inputDir) throws IOException {
        try (var stream = Files.list(Paths.get(inputDir))) {
            List<Path> files = new ArrayList<>();
            stream.filter(Files::isRegularFile).forEach(files::add);
            return files;
        }
    }

    private static byte[] concatenateTxtFiles(List<Path> files) throws IOException {
        List<Path> txtFiles = new ArrayList<>();
        for (Path file : files) {
            if (file.getFileName().toString().endsWith(".txt")) {
                txtFiles.add(file);
            }
        }
        // Sort by filename (e.g., 1.txt, 2.txt, ...)
        txtFiles.sort(Comparator.comparing(p -> p.getFileName().toString()));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (Path txt : txtFiles) {
            baos.write(Files.readAllBytes(txt));
        }
        return baos.toByteArray();
    }

    @SuppressWarnings("resource")
    private static String askForPassword() {
        Scanner scanner = new Scanner(System.in); // Do not close System.in
        String password;
        do {
            System.out.print("[Decrypt] Enter password (16 bytes): ");
            password = scanner.nextLine();
            if (password.length() != 16) {
                System.out.println("[Decrypt] Invalid password length. Please enter a 16 bytes password.");
            }
        } while (password.length() != 16);
        return password;
    }

    private static byte[] decryptBase64Aes(byte[] base64Bytes, String password) throws Exception {
        byte[] encrypted = Base64.getDecoder().decode(base64Bytes);
        SecretKey key = new SecretKeySpec(password.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(encrypted);
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
}
