package com.chessdna.chessdnaanalyzer;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import chesspresso.game.Game;
import chesspresso.pgn.PGNReader;
import chesspresso.position.Position;
import java.io.StringReader;
import java.util.ArrayList;

import java.util.List;

@Service
public class LichessService implements ChessPlatformService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public List<String> fetchGames(String username, int gameCount) {
        String url = "https://lichess.org/api/games/user/"
                + username
                + "?max=" + gameCount;
                //+ "&perfType=rapid,classical";

        // Tell Lichess we want PGN format
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/x-chess-pgn");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );

        String rawPgn = response.getBody();

        if (rawPgn == null || rawPgn.isBlank()) {
            return List.of(); // return empty list, don't crash
        }

        return parseGames(rawPgn);
    }
    private List<String> parseGames(String rawPgn) {
        String[] games = rawPgn.split("\n\n\n");
        return List.of(games);
    }


    public List<String> extractFensFromPgn(String pgnText) {
        List<String> fens = new ArrayList<>();

        try {
            PGNReader reader = new PGNReader(new StringReader(pgnText), "game");
            Game game = reader.parseGame();

            game.gotoStart();   // ✅ fixed — called on game, not position

            while (game.hasNextMove()) {
                game.goForward();
                fens.add(game.getPosition().getFEN());  // also fixed — get FRESH position each time
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse PGN", e);
        }

        return fens;
    }
    public List<List<String>> fetchGamesAsFens(String username, int gameCount) {
        List<String> pgnGames = fetchGames(username, gameCount);

        List<List<String>> allGamesFens = new ArrayList<>();

        for (String pgn : pgnGames) {
            List<String> fensForThisGame = extractFensFromPgn(pgn);
            allGamesFens.add(fensForThisGame);
        }

        return allGamesFens;
    }
}