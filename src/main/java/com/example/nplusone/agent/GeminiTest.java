package com.example.nplusone.agent;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

public class GeminiTest {

    public static void main(String[] args) {
        Client client = new Client();

        GenerateContentResponse response = client.models.generateContent(
                "gemini-3.6-flash",
                "Reply with exactly: Gemini connection successful.",
                null
        );

        System.out.println(response.text());
    }
}