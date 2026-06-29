package com.gov.ac.feature.assistant.dto;

import java.util.List;

public record AssistantAnswerResponseDto(
    String text, List<AssistantActionDto> actions, boolean llmUsed) {}
