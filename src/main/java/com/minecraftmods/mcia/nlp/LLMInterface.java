package com.minecraftmods.mcia.nlp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.amithkoujalgi.ollama4j.core.OllamaAPI;
import io.github.amithkoujalgi.ollama4j.core.exceptions.OllamaBaseException;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatMessageRole;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatRequestBuilder;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatRequestModel;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatResult;

import java.io.IOException;

public class LLMInterface {
    private static final OllamaAPI ollamaAPI = new OllamaAPI("http://localhost:11434/");
    private static final Gson gson = new Gson();

    public static JsonObject getStructureParams(String description) {
        ollamaAPI.setRequestTimeoutSeconds(600);
        String systemPrompt = PromptBuilder.buildStructurePrompt();

        OllamaChatRequestBuilder builder = OllamaChatRequestBuilder.getInstance("llama3.2:latest");

        try {
            OllamaChatRequestModel requestModel = builder
                    .withMessage(OllamaChatMessageRole.SYSTEM, systemPrompt)
                    .withMessage(OllamaChatMessageRole.USER, description)
                    .build();

            OllamaChatResult chatResult = ollamaAPI.chat(requestModel);
            String response = chatResult.getResponse().trim();

            return gson.fromJson(response, JsonObject.class);
        } catch (OllamaBaseException | IOException | InterruptedException e) {
            e.printStackTrace();
            return new JsonObject();
        }
    }
}