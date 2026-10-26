package service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Practical 11: append-only transaction log. */
public class TransactionLog {
    private TransactionLog() {
    }

    public static void append(Path file, String line) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.write(file, (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
