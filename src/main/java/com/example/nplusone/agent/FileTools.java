package com.example.nplusone.agent;

import java.nio.file.Files;
import java.nio.file.Path;

public class FileTools {

    public static String readFile(String filePath) {
        try {
            Path path = Path.of(filePath);

            if (!Files.exists(path)) {
                return "ERROR: File does not exist: " + filePath;
            }

            if (!Files.isRegularFile(path)) {
                return "ERROR: Path is not a file: " + filePath;
            }

            return Files.readString(path);

        } catch (Exception e) {
            return "ERROR reading file: " + e.getMessage();
        }
    }
}