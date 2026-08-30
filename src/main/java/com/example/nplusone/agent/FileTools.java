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

    public static String writeFile(String filePath, String content) {
        try {
            Path path = Path.of(filePath);

            String allowedPath =
                    "src/main/java/com/example/nplusone/AuthorRepository.java";

            if (!filePath.equals(allowedPath)) {
                return "ERROR: write_file is restricted to: " + allowedPath;
            }

            Files.writeString(path, content);

            return "SUCCESS: File written: " + filePath;

        } catch (Exception e) {
            return "ERROR writing file: " + e.getMessage();
        }
    }

    public static String runVerification() {
        try {
            ProcessBuilder processBuilder =
                    new ProcessBuilder(".\\mvnw.cmd", "test");

            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            String output =
                    new String(process.getInputStream().readAllBytes());

            int exitCode = process.waitFor();

            StringBuilder result = new StringBuilder();

            result.append("EXIT_CODE: ")
                    .append(exitCode)
                    .append("\n\n");

            // Find the runtime SQL query containing the book LEFT JOIN.
            String lowerOutput = output.toLowerCase();

            int joinIndex = lowerOutput.indexOf("left join");

            if (joinIndex != -1) {

                int sqlStart =
                        lowerOutput.lastIndexOf("select", joinIndex);

                int sqlEnd =
                        output.indexOf("[Hibernate]", joinIndex);

                result.append("SQL EVIDENCE:\n");

                if (sqlStart != -1) {
                    if (sqlEnd != -1) {
                        result.append(
                                output.substring(sqlStart, sqlEnd).trim()
                        );
                    } else {
                        result.append(
                                output.substring(sqlStart).trim()
                        );
                    }
                }

                result.append("\n\n");

            } else {

                result.append("SQL EVIDENCE:\n");
                result.append("No LEFT JOIN query found.\n\n");
            }

            // Extract Maven test summary.
            int testStart = output.indexOf("Tests run:");

            if (testStart != -1) {

                int testEnd = output.indexOf("\n", testStart);

                result.append("TEST RESULT:\n");

                if (testEnd != -1) {
                    result.append(
                            output.substring(testStart, testEnd).trim()
                    );
                } else {
                    result.append(
                            output.substring(testStart).trim()
                    );
                }

                result.append("\n\n");
            }

            // Report final build status.
            if (output.contains("BUILD SUCCESS")) {
                result.append("BUILD SUCCESS");
            } else if (output.contains("BUILD FAILURE")) {
                result.append("BUILD FAILURE");
            } else {
                result.append("BUILD STATUS: UNKNOWN");
            }

            return result.toString();

        } catch (Exception e) {
            return "ERROR running verification: " + e.getMessage();
        }
    }
}