package service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Practical 11: end-of-day report built by walking the logs folder with NIO. */
public class ReportGenerator {
    private ReportGenerator() {
    }

    public static List<String> generate(Path logsDir, Path reportFile) throws IOException {
        long totalDeposits = 0;
        long totalWithdrawals = 0;
        List<String> report = new ArrayList<>();
        report.add("===== END-OF-DAY REPORT =====");

        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logsDir)) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    files.add(path);
                }
            }
        }
        Collections.sort(files);

        for (Path path : files) {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            report.add("File: " + path.getFileName() + " | size=" + attrs.size() + " bytes | last modified="
                    + attrs.lastModifiedTime());
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length < 3) {
                        continue;
                    }
                    try {
                        long amount = Long.parseLong(parts[2]);
                        if (parts[0].equalsIgnoreCase("DEPOSIT")) {
                            totalDeposits += amount;
                        } else if (parts[0].equalsIgnoreCase("WITHDRAW")) {
                            totalWithdrawals += amount;
                        }
                    } catch (NumberFormatException e) {
                        // not a transaction line; skip it
                    }
                }
            }
        }

        report.add("Total deposits    : " + totalDeposits);
        report.add("Total withdrawals : " + totalWithdrawals);
        report.add("Net change        : " + (totalDeposits - totalWithdrawals));

        if (reportFile.getParent() != null) {
            Files.createDirectories(reportFile.getParent());
        }
        Files.write(reportFile, report);
        return report;
    }
}
