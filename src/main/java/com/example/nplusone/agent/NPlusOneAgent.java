package com.example.nplusone.agent;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

public class NPlusOneAgent {

    public static void main(String[] args) {

        Client client = new Client();

        // 1. Read the actual project files
        String controller = FileTools.readFile(
                "src/main/java/com/example/nplusone/AuthorController.java");

        String repository = FileTools.readFile(
                "src/main/java/com/example/nplusone/AuthorRepository.java");

        String author = FileTools.readFile(
                "src/main/java/com/example/nplusone/Author.java");

        // 2. Ask Gemini to analyze and generate a fix
        String prompt = """
                You are an autonomous software engineering agent.

                Analyze the provided Spring Boot source code for an N+1
                database query problem.

                If an N+1 problem exists, generate a concrete fix.

                You have a restricted write_file capability that can modify
                ONLY:

                src/main/java/com/example/nplusone/AuthorRepository.java

                Return your response in exactly this format:

                ANALYSIS:
                <brief explanation>

                FIX:
                <brief explanation>

                FILE_CONTENT:
                <complete replacement contents of AuthorRepository.java>

                Do not use Markdown code fences around FILE_CONTENT.

                Do not claim the fix has been verified yet.

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

        String result = response.text();

        System.out.println("===== AGENT PROPOSAL =====");
        System.out.println(result);

        // 3. Extract the proposed file
        String marker = "FILE_CONTENT:";

        int fileContentStart = result.indexOf(marker);

        if (fileContentStart == -1) {
            System.out.println("===== WRITE RESULT =====");
            System.out.println("ERROR: Gemini did not provide FILE_CONTENT.");
            return;
        }

        String newFileContent =
                result.substring(fileContentStart + marker.length()).trim();

        // 4. Apply the proposed fix
        String writeResult = FileTools.writeFile(
                "src/main/java/com/example/nplusone/AuthorRepository.java",
                newFileContent
        );

        System.out.println("===== WRITE RESULT =====");
        System.out.println(writeResult);

        if (!writeResult.startsWith("SUCCESS")) {
            return;
        }

        // 5. Automatically verify the generated code
        System.out.println("===== AGENT VERIFICATION =====");

        String verificationResult = FileTools.runVerification();

        System.out.println(verificationResult);

        // 6. Ask Gemini to evaluate the verification result
        String verificationPrompt = """
                You are the verification component of a software
                engineering agent.

                The agent previously analyzed an N+1 problem and applied
                a proposed fix.

                Review the Maven test execution result below.

                Determine:

                1. Whether the verification succeeded.
                2. Whether the output indicates a build/test failure.
                3. Whether further investigation is required.

                Do not claim that the N+1 problem is fixed solely because
                Maven tests pass. Runtime SQL query-count evidence is still
                required for definitive N+1 verification.

                ===== VERIFICATION OUTPUT =====

                %s
                """.formatted(verificationResult);

        GenerateContentResponse verificationResponse =
                client.models.generateContent(
                        "gemini-3.6-flash",
                        verificationPrompt,
                        null
                );

        System.out.println("===== VERIFICATION ANALYSIS =====");
        System.out.println(verificationResponse.text());
    }
}