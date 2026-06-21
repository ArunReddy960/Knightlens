package com.chessdna.chessdnaanalyzer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.*;

@Service
public class StockfishService {

    @Value("${stockfish.path}")
    private String stockfishPath;

    private Process stockfishProcess;
    private BufferedReader reader;
    private BufferedWriter writer;

    @PostConstruct
    public void initEngine() throws IOException {
        ProcessBuilder builder = new ProcessBuilder(stockfishPath);
        builder.redirectErrorStream(true);

        stockfishProcess = builder.start();

        reader = new BufferedReader(new InputStreamReader(stockfishProcess.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(stockfishProcess.getOutputStream()));

        // The handshake — like the phone call greeting
        sendCommand("uci");
        waitForResponse("uciok");

        sendCommand("isready");
        waitForResponse("readyok");
    }
    private void sendCommand(String command) throws IOException {
        writer.write(command);
        writer.newLine();
        writer.flush();
    }
    private void waitForResponse(String expectedKeyword) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains(expectedKeyword)) {
                break;
            }
        }
    }
    public String getBestMove(String fen) throws IOException {
        sendCommand("position fen " + fen);
        sendCommand("go depth 18");

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("bestmove")) {
                String[] parts = line.split(" ");
                return parts[1];  // the actual move, like "e2e4"
            }
        }

        return null;
    }

}

