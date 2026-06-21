package com.chessdna.chessdnaanalyzer;

import org.springframework.web.bind.annotation.*;
import chesspresso.game.Game;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/games")
public class AnalysisController {

    private final ChessPlatformService chessPlatformService;
    private final StockfishService stockfishService;

    public AnalysisController(LichessService lichessService, StockfishService stockfishService) {
        this.chessPlatformService = lichessService;
        this.stockfishService = stockfishService;
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
    public String getBestMoveForFen(@RequestParam String fen) throws IOException {
        return stockfishService.getBestMove(fen);
    }
}