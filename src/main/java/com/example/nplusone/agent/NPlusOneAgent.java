package com.example.nplusone.agent;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

public class NPlusOneAgent {

    public static void main(String[] args) {

        Client client = new Client();

        String controller = FileTools.readFile(
                "src/main/java/com/example/nplusone/AuthorController.java");

        String repository = FileTools.readFile(
                "src/main/java/com/example/nplusone/AuthorRepository.java");

        String author = FileTools.readFile(
                "src/main/java/com/example/nplusone/Author.java");

        String prompt = """
                You are an autonomous software engineering agent.

                You have inspected the actual project files below.

                Analyze the Spring Boot application for an N+1 database
                query problem.

                Determine:

                1. Whether an N+1 problem exists.
                2. The exact code causing it.
                3. The expected SQL query behavior.
                4. One concrete Spring Data JPA fix.
                5. What must be verified after applying the fix.

                Do not modify files.
                Do not claim runtime verification.

                ===== AuthorController.java =====

                %s

                ===== AuthorRepository.java =====

                %s

                ===== Author.java =====

                %s
                """.formatted(controller, repository, author);

        GenerateContentResponse response = client.models.generateContent(
                "gemini-3.6-flash",
                prompt,
                null
        );

        System.out.println("===== AGENT ANALYSIS =====");
        System.out.println(response.text());
    }
}