package com.gov.ac.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ac.assistant")
@Getter
@Setter
public class AssistantProperties {

  /** When true and api-key is set, answers are refined by an OpenAI-compatible chat API. */
  private boolean llmEnabled = false;

  private String llmApiUrl = "https://api.openai.com/v1/chat/completions";

  private String llmModel = "gpt-4o-mini";

  private String llmApiKey = "";

  private int llmTimeoutSeconds = 25;
}
