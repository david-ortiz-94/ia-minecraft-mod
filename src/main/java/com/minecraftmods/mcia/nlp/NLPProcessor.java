package com.minecraftmods.mcia.nlp;

import io.github.amithkoujalgi.ollama4j.core.OllamaAPI;
import io.github.amithkoujalgi.ollama4j.core.exceptions.OllamaBaseException;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatMessageRole;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatRequestBuilder;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatRequestModel;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatResult;

import java.io.IOException;

public class NLPProcessor {
    private static final OllamaAPI ollamaAPI = new OllamaAPI("http://localhost:11434/");

    public static Intent getIntention(String userPrompt) {
        ollamaAPI.setRequestTimeoutSeconds(600);

        String systemPrompt = PromptBuilder.buildIntentPrompt();
        OllamaChatRequestBuilder builder = OllamaChatRequestBuilder.getInstance("llama3.2:latest");

        try {
            OllamaChatRequestModel requestModel = builder
                    .withMessage(OllamaChatMessageRole.SYSTEM, systemPrompt)
                    .withMessage(OllamaChatMessageRole.USER, userPrompt)
                    .build();

            OllamaChatResult chatResult = ollamaAPI.chat(requestModel);
            String response = chatResult.getResponse().trim();

            return Intent.fromString(response);
        } catch (OllamaBaseException | IOException | InterruptedException e) {
            e.printStackTrace();
            return Intent.UNSPECIFIED;
        }
    }

    public enum Intent {
        STRUCTURE("STRUCTURE"),
        ITEM("ITEM"),
        DECORATION("DECORATION"),
        UNSPECIFIED("UNSPECIFIED");
        private final String value;

        Intent(String value) {
            this.value = value;
        }

        public static Intent fromString(String text) {
            for (Intent intent : Intent.values()) {
                if (intent.value.equalsIgnoreCase(text)) {
                    return intent;
                }
            }
            return UNSPECIFIED;
        }

    }
}