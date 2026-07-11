package com.chessdna.chessdnaanalyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
public class ClaudeService {

    @Value("${claude.api.key}")
    private String apiKey;

    @Value("${claude.api.url}")
    private String apiUrl;

    @Value("${claude.api.model}")
    private String model;

    @Value("${claude.api.max-tokens}")
    private int maxTokens;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateCoachingReport(List<PatternAnalysisService.PhaseStats> patterns,
                                         String username) {
        String prompt = buildPrompt(patterns, username);
        return callClaude(prompt);
    }

    private String buildPrompt(List<PatternAnalysisService.PhaseStats> patterns,
                               String username) {
        PatternAnalysisService.PhaseStats opening = findPhase(patterns, "opening");
        PatternAnalysisService.PhaseStats middlegame = findPhase(patterns, "middlegame");
        PatternAnalysisService.PhaseStats endgame = findPhase(patterns, "endgame");

        return """
                You are a chess coach writing a personalized improvement report.
                
                Player: %s
                
                Their statistics across recent games:
                
                Opening:
                - Accuracy: %.1f%%
                - Average centipawn loss: %.1f
                - Blunders: %d, Mistakes: %d, Inaccuracies: %d
                - Total moves analyzed: %d
                
                Middlegame:
                - Accuracy: %.1f%%
                - Average centipawn loss: %.1f
                - Blunders: %d, Mistakes: %d, Inaccuracies: %d
                - Total moves analyzed: %d
                
                Endgame:
                - Accuracy: %.1f%%
                - Average centipawn loss: %.1f
                - Blunders: %d, Mistakes: %d, Inaccuracies: %d
                - Total moves analyzed: %d
                
                Write a coaching report with these sections:
                1. OVERALL ASSESSMENT (2-3 sentences, honest but encouraging)
                2. BIGGEST STRENGTH (what they do best, with specific reference to the numbers)
                3. PRIMARY WEAKNESS (their single biggest area to improve)
                4. ACTION PLAN (3 specific, practical things to work on this week)
                
                Rules:
                - Write like a real coach talking to a real player
                - Reference the actual numbers naturally, don't just list them
                - Be specific, not generic ("work on endgames" is too vague)
                - Keep it under 250 words
                - Don't start with "I" or "As a chess coach"
                """.formatted(
                username,
                opening.accuracyPercentage(), opening.averageCentipawnLoss(),
                opening.blunderCount(), opening.mistakeCount(), opening.inaccuracyCount(),
                opening.totalMoves(),
                middlegame.accuracyPercentage(), middlegame.averageCentipawnLoss(),
                middlegame.blunderCount(), middlegame.mistakeCount(), middlegame.inaccuracyCount(),
                middlegame.totalMoves(),
                endgame.accuracyPercentage(), endgame.averageCentipawnLoss(),
                endgame.blunderCount(), endgame.mistakeCount(), endgame.inaccuracyCount(),
                endgame.totalMoves()
        );
    }

    private PatternAnalysisService.PhaseStats findPhase(
            List<PatternAnalysisService.PhaseStats> patterns, String phaseName) {
        return patterns.stream()
                .filter(p -> p.phaseName().equals(phaseName))
                .findFirst()
                .orElse(new PatternAnalysisService.PhaseStats(phaseName, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    }

    private String callClaude(String prompt) {
        try {
            String requestBody = objectMapper.writeValueAsString(new ClaudeRequest(
                    model,
                    maxTokens,
                    List.of(new Message("user", prompt))
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new RuntimeException("Claude API error: " + response.statusCode()
                        + " " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            return root.path("content").get(0).path("text").asText();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Claude API call interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get coaching report from Claude", e);
        }
    }

    record ClaudeRequest(String model, int max_tokens, List<Message> messages) {}
    record Message(String role, String content) {}
}