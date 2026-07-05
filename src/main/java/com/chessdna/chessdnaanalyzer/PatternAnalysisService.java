package com.chessdna.chessdnaanalyzer;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class PatternAnalysisService {

    public List<PhaseStats> analyzePatterns(List<List<StockfishService.AnalyzedMove>> allGamesAnalysis) {
        List<StockfishService.AnalyzedMove> allMoves = flattenAllMoves(allGamesAnalysis);

        List<PhaseStats> result = new ArrayList<>();
        result.add(calculatePhaseStats(allMoves, "opening"));
        result.add(calculatePhaseStats(allMoves, "middlegame"));
        result.add(calculatePhaseStats(allMoves, "endgame"));

        return result;
    }

    private List<StockfishService.AnalyzedMove> flattenAllMoves(
            List<List<StockfishService.AnalyzedMove>> allGamesAnalysis) {
        List<StockfishService.AnalyzedMove> allMoves = new ArrayList<>();

        for (List<StockfishService.AnalyzedMove> oneGame : allGamesAnalysis) {
            for (StockfishService.AnalyzedMove move : oneGame) {
                allMoves.add(move);
            }
        }

        return allMoves;
    }

    private PhaseStats calculatePhaseStats(List<StockfishService.AnalyzedMove> allMoves, String targetPhase) {
        List<StockfishService.AnalyzedMove> phaseMoves = new ArrayList<>();

        for (StockfishService.AnalyzedMove move : allMoves) {
            if (move.phase().equals(targetPhase)) {
                phaseMoves.add(move);
            }
        }

        int total = phaseMoves.size();
        if (total == 0) {
            return new PhaseStats(targetPhase, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        int bestCount = 0, excellentCount = 0, goodCount = 0,
                inaccuracyCount = 0, mistakeCount = 0, blunderCount = 0;
        int totalCpLoss = 0;

        for (StockfishService.AnalyzedMove move : phaseMoves) {
            totalCpLoss += move.centipawnLoss();

            switch (move.quality()) {
                case "BEST" -> bestCount++;
                case "EXCELLENT" -> excellentCount++;
                case "GOOD" -> goodCount++;
                case "INACCURACY" -> inaccuracyCount++;
                case "MISTAKE" -> mistakeCount++;
                case "BLUNDER" -> blunderCount++;
            }
        }

        double averageCpLoss = (double) totalCpLoss / total;
        double accuracy = (double) (bestCount + excellentCount) / total * 100;

        return new PhaseStats(targetPhase, total, averageCpLoss, accuracy,
                bestCount, excellentCount, goodCount, inaccuracyCount,
                mistakeCount, blunderCount);
    }

    public record PhaseStats(
            String phaseName,
            int totalMoves,
            double averageCentipawnLoss,
            double accuracyPercentage,
            int bestCount,
            int excellentCount,
            int goodCount,
            int inaccuracyCount,
            int mistakeCount,
            int blunderCount
    ) {}
}