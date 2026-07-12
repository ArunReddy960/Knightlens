package com.chessdna.chessdnaanalyzer;

import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/games")
public class AnalysisController {

    private final ChessPlatformService chessPlatformService;
    private final StockfishService stockfishService;
    private final PatternAnalysisService patternAnalysisService;
    private final AnalysisJobService analysisJobService;
    private final AnalysisJobRepository jobRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalysisController(LichessService lichessService,
                              StockfishService stockfishService,
                              PatternAnalysisService patternAnalysisService,
                              AnalysisJobService analysisJobService,
                              AnalysisJobRepository jobRepository) {
        this.chessPlatformService = lichessService;
        this.stockfishService = stockfishService;
        this.patternAnalysisService = patternAnalysisService;
        this.analysisJobService = analysisJobService;
        this.jobRepository = jobRepository;
    }

    @GetMapping("/{username}")
    public List<String> getGames(
            @PathVariable String username,
            @RequestParam(defaultValue = "10") int gameCount) {
        return chessPlatformService.fetchGames(username, gameCount);
    }

    @GetMapping("/{username}/fens")
    public List<List<String>> getGamesFens(
            @PathVariable String username,
            @RequestParam(defaultValue = "2") int gameCount) {
        return ((LichessService) chessPlatformService).fetchGamesAsFens(username, gameCount);
    }

    @GetMapping("/bestmove")
    public String getBestMoveForFen(@RequestParam String fen) throws IOException, InterruptedException {
        StockfishService.AnalysisResult result = stockfishService.analyzePosition(fen, 18);
        return result.bestMove();
    }

    @GetMapping("/{username}/analyze")
    public List<List<StockfishService.AnalyzedMove>> analyzeGames(
            @PathVariable String username,
            @RequestParam(defaultValue = "1") int gameCount,
            @RequestParam(defaultValue = "18") int depth) throws IOException, InterruptedException {

        List<List<String>> allGamesFens = ((LichessService) chessPlatformService)
                .fetchGamesAsFens(username, gameCount);

        List<List<StockfishService.AnalyzedMove>> allGamesAnalysis = new ArrayList<>();

        for (List<String> gameFens : allGamesFens) {
            List<StockfishService.AnalyzedMove> analysis = stockfishService.analyzeGame(gameFens, depth);
            allGamesAnalysis.add(analysis);
        }

        return allGamesAnalysis;
    }

    @GetMapping("/{username}/patterns")
    public List<PatternAnalysisService.PhaseStats> analyzePatterns(
            @PathVariable String username,
            @RequestParam(defaultValue = "5") int gameCount,
            @RequestParam(defaultValue = "18") int depth) throws InterruptedException {

        List<List<String>> allGamesFens = ((LichessService) chessPlatformService)
                .fetchGamesAsFens(username, gameCount);

        List<List<StockfishService.AnalyzedMove>> allGamesAnalysis = new ArrayList<>();

        for (List<String> gameFens : allGamesFens) {
            List<StockfishService.AnalyzedMove> analysis = stockfishService.analyzeGame(gameFens, depth);
            allGamesAnalysis.add(analysis);
        }

        return patternAnalysisService.analyzePatterns(allGamesAnalysis);
    }

    // ── NEW: Async endpoints ───────────────────────────────────────────
    @GetMapping("/jobs/{jobId}")
    public AnalysisJob getJobStatus(@PathVariable Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
    }

    // Old endpoint — still calling startJob with old signature
    @PostMapping("/{username}/analyze-quick")
    public AnalysisJob quickAnalysis(
            @PathVariable String username,
            @RequestParam(defaultValue = "10") int gameCount) {
        return analysisJobService.startJob(username, gameCount, 12); // depth hardcoded to 12
    }

    @PostMapping("/{username}/analyze-deep")
    public AnalysisJob deepAnalysis(
            @PathVariable String username,
            @RequestParam(defaultValue = "10") int gameCount) {
        return analysisJobService.startJob(username, gameCount, 18); // depth hardcoded to 18
    }
}