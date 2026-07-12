package com.chessdna.chessdnaanalyzer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class StockfishService {

    @Value("${stockfish.path}")
    private String stockfishPath;

    @Value("${stockfish.depth:18}")
    private int defaultDepth;

    private static final int POOL_SIZE = 8;

    private BlockingQueue<StockfishEngine> enginePool;

    @PostConstruct
    public void initPool() throws IOException {
        enginePool = new ArrayBlockingQueue<>(POOL_SIZE);
        for (int i = 0; i < POOL_SIZE; i++) {
            enginePool.offer(new StockfishEngine(stockfishPath));
        }
    }

    @PreDestroy
    public void shutdownPool() {
        for (StockfishEngine engine : enginePool) {
            engine.close();
        }
    }

    public AnalysisResult analyzePosition(String fen, int depth) throws IOException, InterruptedException {
        StockfishEngine engine = enginePool.poll(120, TimeUnit.SECONDS);
        if (engine == null) {
            throw new RuntimeException("No Stockfish engine available — pool timeout");
        }
        try {
            return engine.analyze(fen, depth);
        } finally {
            enginePool.offer(engine);
        }
    }

    public List<AnalyzedMove> analyzeGame(List<String> fens, int depth) throws InterruptedException {

        // ── PHASE 1: Evaluate ALL positions in PARALLEL ──
        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        String startingFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

        List<String> allFens = new ArrayList<>();
        allFens.add(startingFen);
        allFens.addAll(fens);

        for (String fen : allFens) {
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
                try {
                    AnalysisResult result = analyzePosition(fen, depth);
                    return normalizeToWhitePerspective(result.evaluationCentipawns(), fen);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }

        List<Integer> allEvaluations = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        // ── PHASE 2: Calculate differences + classify phase/quality ──
        List<AnalyzedMove> results = new ArrayList<>();
        for (int i = 1; i < allEvaluations.size(); i++) {
            int previousEval = allEvaluations.get(i - 1);
            int currentEval = allEvaluations.get(i);
            int cpLoss = previousEval - currentEval;

            String fenBeforeThisMove = allFens.get(i - 1);
            String phase = determinePhase(fenBeforeThisMove, i);
            String quality = classifyMoveQuality(cpLoss);

            results.add(new AnalyzedMove(i, cpLoss, phase, quality));
        }

        return results;
    }

    public int normalizeToWhitePerspective(int evaluation, String fen) {
        String turnIndicator = fen.split(" ")[1];
        if (turnIndicator.equals("b")) {
            return -evaluation;
        }
        return evaluation;
    }

    private int countPieces(String fen) {
        String boardPart = fen.split(" ")[0];
        int count = 0;
        for (char c : boardPart.toCharArray()) {
            if (Character.isLetter(c)) {
                count++;
            }
        }
        return count;
    }

    public String determinePhase(String fen, int moveNumber) {
        int pieceCount = countPieces(fen);
        boolean queensGone = !fen.split(" ")[0].contains("Q")
                && !fen.split(" ")[0].contains("q");

        if (pieceCount <= 12) {
            return "endgame";
        }
        if (queensGone) {
            return "endgame";
        }
        if (moveNumber <= 15) {
            return "opening";
        }
        return "middlegame";
    }

    public String classifyMoveQuality(int cpLoss) {
        if (cpLoss <= 10) return "BEST";
        if (cpLoss <= 25) return "EXCELLENT";
        if (cpLoss <= 50) return "GOOD";
        if (cpLoss <= 100) return "INACCURACY";
        if (cpLoss <= 200) return "MISTAKE";
        return "BLUNDER";
    }

    public record AnalysisResult(String bestMove, int evaluationCentipawns) {}
    public record AnalyzedMove(int moveNumber, int centipawnLoss, String phase, String quality) {}

    // ── Inner class: one single Stockfish process ──
    private static class StockfishEngine {
        private final Process process;
        private final BufferedReader reader;
        private final BufferedWriter writer;

        StockfishEngine(String path) throws IOException {
            ProcessBuilder builder = new ProcessBuilder(path);
            builder.redirectErrorStream(true);
            this.process = builder.start();
            this.reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

            sendCommand("uci");
            waitForResponse("uciok");
            sendCommand("isready");
            waitForResponse("readyok");
        }

        AnalysisResult analyze(String fen, int depth) throws IOException {
            sendCommand("position fen " + fen);
            sendCommand("go depth " + depth);

            String bestMove = null;
            int evaluation = 0;
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.contains("score cp")) {
                    evaluation = extractScore(line);
                }
                if (line.startsWith("bestmove")) {
                    bestMove = line.split(" ")[1];
                    break;
                }
            }

            return new AnalysisResult(bestMove, evaluation);
        }

        private void sendCommand(String command) throws IOException {
            writer.write(command);
            writer.newLine();
            writer.flush();
        }

        private void waitForResponse(String expectedKeyword) throws IOException {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(expectedKeyword)) break;
            }
        }

        private int extractScore(String infoLine) {
            String[] parts = infoLine.split(" ");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("cp")) {
                    return Integer.parseInt(parts[i + 1]);
                }
            }
            return 0;
        }

        void close() {
            try {
                sendCommand("quit");
                process.destroy();
            } catch (IOException ignored) {}
        }
    }
}