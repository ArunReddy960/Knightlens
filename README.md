# Chess DNA Analyzer

A backend system that analyzes a chess player's game history and generates a personalized **"Chess DNA" report** — identifying recurring weaknesses across opening, middlegame, and endgame phases using real engine analysis.

Built to solve a real gap: existing tools explain individual moves, but none model the *player* across many games. This system does.

---

## Architecture

```
Lichess Public API
      ↓
LichessService         → fetches PGN game history for any username
      ↓
Chesspresso             → parses PGN into per-move FEN positions
      ↓
StockfishService        → parallel engine analysis via process pool
  (4 Stockfish engines, CompletableFuture, BlockingQueue)
      ↓
PatternAnalysisService  → aggregates centipawn loss across phases
      ↓
PostgreSQL              → async job tracking and result persistence
      ↓
REST API                → Spring Boot endpoints, async job pattern
```

---

## Key Engineering Decisions

### Stockfish Process Pool
Stockfish is a native binary — spawning a new process per move would cost ~200ms startup overhead per position. Instead, a `BlockingQueue<StockfishEngine>` pre-warms 4 engine processes at startup and manages them as a pool (borrow → use → return), cutting per-move overhead to near-zero.

### Parallel Move Analysis (CompletableFuture)
Each position's evaluation is independent of others. Phase 1 evaluates all moves simultaneously using `CompletableFuture.supplyAsync()` across the pool. Phase 2 calculates centipawn differences sequentially — since each loss requires the preceding evaluation. This separation of concerns reduced single-game analysis time from ~85s to ~15s.

### Async Job Pattern
Full game analysis (5 games ≈ 30 seconds) would block HTTP threads and timeout. The `/analyze-async` endpoint creates a job record, returns the job ID in ~615ms, and offloads processing to a Spring `@Async` background thread. Clients poll `/jobs/{id}` for status.

### Perspective Normalization
Stockfish's `score cp` value is always relative to the side-to-move, not a fixed perspective. Without normalization, centipawn loss alternates between large positive and negative values across consecutive moves — making pattern detection meaningless. Solution: flip sign when FEN's turn indicator is `b`, normalizing all evaluations to White's perspective before calculating differences.

### Phase Classification
Phases are determined by board state, not move number (a common but inaccurate shortcut). Rules in priority order:
1. Total pieces ≤ 12 → **Endgame**
2. Both queens exchanged → **Endgame** (override — queenless positions are practically endgames regardless of material count)
3. Move ≤ 15 → **Opening**
4. Otherwise → **Middlegame**

---

## API Reference

### Start Async Analysis
```
POST /api/games/{username}/analyze-async?gameCount=10
```
Returns immediately with job ID. Analysis runs in background.

**Response:**
```json
{
  "id": 7,
  "username": "DrNykterstein",
  "gameCount": 5,
  "status": "PENDING",
  "createdAt": "2026-07-05T15:40:47"
}
```

### Poll Job Status
```
GET /api/games/jobs/{jobId}
```
Returns `IN_PROGRESS` while running, or full results when `COMPLETED`.

**Completed response:**
```json
[
  {
    "phaseName": "opening",
    "totalMoves": 75,
    "accuracyPercentage": 96.0,
    "averageCentipawnLoss": 0.88,
    "bestCount": 65,
    "excellentCount": 7,
    "mistakeCount": 0,
    "blunderCount": 0
  },
  {
    "phaseName": "middlegame",
    "totalMoves": 199,
    "accuracyPercentage": 81.4,
    "averageCentipawnLoss": -2.89,
    "blunderCount": 4
  },
  {
    "phaseName": "endgame",
    "totalMoves": 170,
    "accuracyPercentage": 77.1,
    "averageCentipawnLoss": 2.92,
    "blunderCount": 4
  }
]
```

### Single Position Analysis
```
GET /api/games/bestmove?fen={fen}
```
Returns Stockfish's best move for any FEN position.

### Raw Game Data
```
GET /api/games/{username}?gameCount=10       → raw PGN
GET /api/games/{username}/fens?gameCount=5   → FEN per move
```

---

## Move Quality Classification

| Classification | Centipawn Loss |
|---|---|
| Best | ≤ 10 cp |
| Excellent | 11–25 cp |
| Good | 26–50 cp |
| Inaccuracy | 51–100 cp |
| Mistake | 101–200 cp |
| Blunder | > 200 cp |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5 |
| Build | Gradle |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Chess Engine | Stockfish 18 (subprocess, UCI protocol) |
| PGN Parsing | Chesspresso |
| Async | Spring @Async, CompletableFuture, BlockingQueue |
| Game Data | Lichess Public API |

---

## Local Setup

### Prerequisites
- Java 17+
- PostgreSQL
- [Stockfish 18](https://stockfishchess.org/download/) installed locally

### Environment Variables
```
DB_USERNAME=postgres
DB_PASSWORD=your_password
STOCKFISH_PATH=C:\path\to\stockfish.exe   # or /usr/games/stockfish on Linux
```

### Run
```bash
./gradlew bootRun
```

App starts on `http://localhost:8080`

---

## Performance

| Metric | Value |
|---|---|
| Single game analysis (136 moves) | ~15 seconds |
| 5 games async end-to-end | ~30 seconds |
| POST response time (async) | ~615ms |
| Stockfish pool size | 4 engines |
| Parallelism model | Per-move within each game |

---

## Real Results

Analysis of **DrNykterstein** (Magnus Carlsen's Lichess account), 5 games:

```
Opening:     96.0% accuracy  (0 blunders)
Middlegame:  81.4% accuracy  (4 blunders)
Endgame:     77.1% accuracy  (4 blunders)
```

Accuracy decreasing from opening → endgame is a coherent, expected pattern — confirming the analysis produces meaningful signal, not noise.

---

## What Makes This Different

Most chess analysis tools explain individual moves. This system models the **player** — aggregating patterns across 10–20 games to answer: *where do you consistently lose advantage?*

The architecture separates concerns cleanly:
- **Stockfish** provides ground-truth numerical evaluation (not guesswork)
- **Phase classification** uses board state, not move number
- **Async job pattern** makes deep analysis practical for real users
- **Process pool** makes Stockfish efficient enough to analyze at scale
