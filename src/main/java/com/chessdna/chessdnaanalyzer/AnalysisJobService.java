package com.chessdna.chessdnaanalyzer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class AnalysisJobService {

    private final AnalysisJobRepository jobRepository;
    private final ChessPlatformService chessPlatformService;
    private final StockfishService stockfishService;
    private final PatternAnalysisService patternAnalysisService;
    private final ClaudeService claudeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private AnalysisJobService self;

    public AnalysisJobService(AnalysisJobRepository jobRepository,
                              LichessService lichessService,
                              StockfishService stockfishService,
                              PatternAnalysisService patternAnalysisService,
                              ClaudeService claudeService) {
        this.jobRepository = jobRepository;
        this.chessPlatformService = lichessService;
        this.stockfishService = stockfishService;
        this.patternAnalysisService = patternAnalysisService;
        this.claudeService = claudeService;
    }

    @Autowired
    public void setSelf(@Lazy AnalysisJobService self) {
        this.self = self;
    }

    public AnalysisJob startJob(String username, int gameCount, int depth) {
        AnalysisJob job = new AnalysisJob();
        job.setUsername(username);
        job.setGameCount(gameCount);
        job.setStatus("PENDING");
        job.setDepth(depth);
        AnalysisJob savedJob = jobRepository.save(job);
        self.processJobAsync(savedJob.getId(), username, gameCount, depth);
        return savedJob;
    }

    @Async
    public void processJobAsync(Long jobId, String username, int gameCount, int depth) {
        AnalysisJob job = jobRepository.findById(jobId).orElseThrow();

        try {
            job.setStatus("IN_PROGRESS");
            jobRepository.save(job);

            List<List<String>> allGamesFens = ((LichessService) chessPlatformService)
                    .fetchGamesAsFens(username, gameCount);

            // ── Analyze ALL games in PARALLEL ──────────────────────────────
            // Before: Game 1 → wait → Game 2 → wait → Game 3
            // After:  Game 1 ┐
            //         Game 2 ├→ all running simultaneously → join
            //         Game 3 ┘
            List<CompletableFuture<List<StockfishService.AnalyzedMove>>> gameFutures =
                    new ArrayList<>();

            for (List<String> gameFens : allGamesFens) {
                CompletableFuture<List<StockfishService.AnalyzedMove>> future =
                        CompletableFuture.supplyAsync(() -> {
                            try {
                                return stockfishService.analyzeGame(gameFens, depth);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                gameFutures.add(future);
            }

            // Wait for all games to complete
            List<List<StockfishService.AnalyzedMove>> allGamesAnalysis = gameFutures.stream()
                    .map(CompletableFuture::join)
                    .toList();
            // ───────────────────────────────────────────────────────────────

            List<PatternAnalysisService.PhaseStats> patterns =
                    patternAnalysisService.analyzePatterns(allGamesAnalysis);

            String claudeJson = claudeService.generateAnalysis(patterns, username);

            com.fasterxml.jackson.databind.JsonNode claudeNode =
                    objectMapper.readTree(claudeJson);

            String personalityJson = objectMapper.writeValueAsString(
                    claudeNode.path("personality"));
            String coachingReport = objectMapper.writeValueAsString(
                    claudeNode.path("report"));

            String resultJson = objectMapper.writeValueAsString(patterns);

            job.setResultJson(resultJson);
            job.setPersonalityJson(personalityJson);
            job.setCoachingReport(coachingReport);
            job.setStatus("COMPLETED");
            jobRepository.save(job);

        } catch (Exception e) {
            job.setStatus("FAILED");
            jobRepository.save(job);
            System.err.println("Job " + jobId + " failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}