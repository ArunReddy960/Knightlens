package com.chessdna.chessdnaanalyzer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PatternAnalysisServiceTest {

    private PatternAnalysisService patternAnalysisService;

    @BeforeEach
    void setUp() {
        patternAnalysisService = new PatternAnalysisService();
    }

    @Test
    void analyzePatterns_emptyGames_returnsZeroStats() {
        List<PatternAnalysisService.PhaseStats> result =
                patternAnalysisService.analyzePatterns(List.of());

        assertEquals(3, result.size()); // always 3 phases returned
        result.forEach(phase -> assertEquals(0, phase.totalMoves()));
    }

    @Test
    void analyzePatterns_openingMovesOnly_correctPhaseCount() {
        StockfishService.AnalyzedMove move1 =
                new StockfishService.AnalyzedMove(1, 5, "opening", "BEST");
        StockfishService.AnalyzedMove move2 =
                new StockfishService.AnalyzedMove(2, 15, "opening", "EXCELLENT");
        StockfishService.AnalyzedMove move3 =
                new StockfishService.AnalyzedMove(3, 60, "opening", "INACCURACY");

        List<List<StockfishService.AnalyzedMove>> allGames = List.of(
                List.of(move1, move2, move3)
        );

        List<PatternAnalysisService.PhaseStats> result =
                patternAnalysisService.analyzePatterns(allGames);

        PatternAnalysisService.PhaseStats opening = result.stream()
                .filter(p -> p.phaseName().equals("opening"))
                .findFirst().orElseThrow();

        assertEquals(3, opening.totalMoves());
        assertEquals(1, opening.bestCount());
        assertEquals(1, opening.excellentCount());
        assertEquals(1, opening.inaccuracyCount());
        assertEquals(0, opening.blunderCount());
    }

    @Test
    void analyzePatterns_accuracyCalculation_correctPercentage() {
        StockfishService.AnalyzedMove best =
                new StockfishService.AnalyzedMove(1, 5, "middlegame", "BEST");
        StockfishService.AnalyzedMove excellent =
                new StockfishService.AnalyzedMove(2, 20, "middlegame", "EXCELLENT");
        StockfishService.AnalyzedMove blunder =
                new StockfishService.AnalyzedMove(3, 300, "middlegame", "BLUNDER");
        StockfishService.AnalyzedMove mistake =
                new StockfishService.AnalyzedMove(4, 150, "middlegame", "MISTAKE");

        List<List<StockfishService.AnalyzedMove>> allGames = List.of(
                List.of(best, excellent, blunder, mistake)
        );

        List<PatternAnalysisService.PhaseStats> result =
                patternAnalysisService.analyzePatterns(allGames);

        PatternAnalysisService.PhaseStats middlegame = result.stream()
                .filter(p -> p.phaseName().equals("middlegame"))
                .findFirst().orElseThrow();

        // accuracy = (BEST + EXCELLENT) / total * 100 = (1+1)/4*100 = 50%
        assertEquals(50.0, middlegame.accuracyPercentage());
        assertEquals(4, middlegame.totalMoves());
        assertEquals(1, middlegame.blunderCount());
        assertEquals(1, middlegame.mistakeCount());
    }

    @Test
    void analyzePatterns_multipleGames_flattensCorrectly() {
        StockfishService.AnalyzedMove game1move =
                new StockfishService.AnalyzedMove(1, 5, "endgame", "BEST");
        StockfishService.AnalyzedMove game2move =
                new StockfishService.AnalyzedMove(1, 250, "endgame", "BLUNDER");

        List<List<StockfishService.AnalyzedMove>> allGames = List.of(
                List.of(game1move),
                List.of(game2move)
        );

        List<PatternAnalysisService.PhaseStats> result =
                patternAnalysisService.analyzePatterns(allGames);

        PatternAnalysisService.PhaseStats endgame = result.stream()
                .filter(p -> p.phaseName().equals("endgame"))
                .findFirst().orElseThrow();

        // Both games' moves should be combined
        assertEquals(2, endgame.totalMoves());
        assertEquals(1, endgame.bestCount());
        assertEquals(1, endgame.blunderCount());
    }
}