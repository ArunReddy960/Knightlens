package com.chessdna.chessdnaanalyzer;

import java.util.List;

public interface ChessPlatformService {
    List<String> fetchGames(String username, int gameCount);
}