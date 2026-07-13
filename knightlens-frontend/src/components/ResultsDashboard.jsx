// src/components/ResultsDashboard.jsx
//
// Displays completed analysis results:
// 1. Primary weakness summary (most impactful insight, shown first)
// 2. Phase performance cards (opening / middlegame / endgame)
// 3. Coaching report from Claude
//
// PROPS:
//   username      — string, player name
//   phases        — array of PhaseStats (may be null if parsing failed)
//   report        — string, coaching report from Claude (may be null)
//   depth         — number (12 or 18)
//   onNewAnalysis — function, called when user wants to start over

import PhaseCard from "./PhaseCard";
import Badge from "./Badge";
import { depthToMode } from "../services/analysisApi";

export default function ResultsDashboard({ username, phases, report, depth, onNewAnalysis }) {
  // DEFENSIVE: find the weakest phase only if phases is a valid array
  const worstPhase = Array.isArray(phases) && phases.length > 0
    ? [...phases].sort((a, b) => (a.accuracyPercentage ?? 100) - (b.accuracyPercentage ?? 100))[0]
    : null;

  const modeLabel = depthToMode(depth) === "quick" ? "Quick" : "Deep";
  const displayName = username || "Player";

  return (
    <div>
      {/* Page header with back button */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
        <div>
          <div style={{ fontSize: 18, fontWeight: 700, color: "#e6edf3" }}>
            KnightLens Report — {displayName}
          </div>
          <div style={{ display: "flex", gap: 8, marginTop: 6, flexWrap: "wrap" }}>
            <Badge variant={depth === 12 ? "neutral" : "blue"}>{modeLabel}</Badge>
            <Badge variant="neutral">depth {depth}</Badge>
          </div>
        </div>
        <button
          onClick={onNewAnalysis}
          style={{
            padding: "6px 16px",
            borderRadius: "var(--radius-sm)",
            background: "transparent",
            border: "1px solid #30363d",
            color: "#8b949e",
            cursor: "pointer",
            fontSize: 12,
          }}
        >
          ← New analysis
        </button>
      </div>

      {/* PRIMARY WEAKNESS SUMMARY
       * WHY show this first?
       * Users care most about "what's my biggest problem" —
       * put the most important insight at the top, not buried in numbers.
       * Only rendered if we actually have phase data with blunder counts.
       */}
      {worstPhase && (
        <div style={{
          background: "#1a0f0f",
          border: "1px solid #5a1d1d",
          borderRadius: 10,
          padding: "16px 20px",
          marginBottom: 20,
          display: "flex",
          alignItems: "flex-start",
          gap: 14,
        }}>
          <span aria-hidden="true" style={{ fontSize: 22, flexShrink: 0 }}>⚠</span>
          <div>
            <div style={{ fontSize: 13, color: "#f85149", fontWeight: 600, marginBottom: 4 }}>
              Primary weakness detected
            </div>
            <div style={{ fontSize: 14, color: "#e6edf3", lineHeight: 1.6 }}>
              Your <strong>{worstPhase.phaseName}</strong> accuracy is{" "}
              {typeof worstPhase.accuracyPercentage === "number"
                ? worstPhase.accuracyPercentage.toFixed(1)
                : "–"}% — the lowest of your three phases
              {(worstPhase.blunderCount ?? 0) > 0
                ? `, with ${worstPhase.blunderCount} blunder${worstPhase.blunderCount !== 1 ? "s" : ""} detected.`
                : "."}
            </div>
          </div>
        </div>
      )}

      {/* PHASE PERFORMANCE CARDS */}
      {Array.isArray(phases) && phases.length > 0 ? (
        <div style={{
          background: "#161b22",
          border: "1px solid #21262d",
          borderRadius: 12,
          padding: 24,
          marginBottom: 20,
        }}>
          <div style={{ fontSize: 14, fontWeight: 600, color: "#e6edf3", marginBottom: 16 }}>
            Phase performance
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(210px, 1fr))", gap: 14 }}>
            {phases.map((p) => (
              <PhaseCard key={p.phaseName} phase={p} />
            ))}
          </div>
        </div>
      ) : (
        // GRACEFUL DEGRADATION:
        // If resultJson was missing or unparseable, show a clear message
        // instead of a broken/empty section.
        <div style={{
          background: "#161b22",
          border: "1px solid #21262d",
          borderRadius: 12,
          padding: 24,
          marginBottom: 20,
          color: "#8b949e",
          fontSize: 14,
        }}>
          The analysis completed, but phase data could not be loaded.
        </div>
      )}

      {/* COACHING REPORT
       * Only render if we actually have a report string.
       * Serif font makes it feel like a real coach's written notes.
       */}
      {report ? (
        <div style={{
          background: "#161b22",
          border: "1px solid #21262d",
          borderRadius: 12,
          padding: 24,
          marginBottom: 20,
        }}>
          <div style={{ fontSize: 14, fontWeight: 600, color: "#e6edf3", marginBottom: 16 }}>
            Coaching report
          </div>
          <div style={{
            background: "#0d1117",
            border: "1px solid #21262d",
            borderRadius: 8,
            padding: 20,
            lineHeight: 1.75,
            fontSize: 14,
            color: "#c9d1d9",
            whiteSpace: "normal",
            fontFamily: "Georgia, 'Times New Roman', serif",
          }}>
            {report.split('\n').map((line, i) => {
             if (line.startsWith('## ')) return <h3 key={i} style={{color:'#58a6ff', fontSize:14, fontWeight:700, margin:'16px 0 6px'}}>{line.replace('## ','')}</h3>;
             if (line.startsWith('# ')) return <h2 key={i} style={{color:'#e6edf3', fontSize:16, fontWeight:700, margin:'0 0 12px'}}>{line.replace('# ','')}</h2>;
             if (line.startsWith('---')) return <hr key={i} style={{border:'none', borderTop:'1px solid #21262d', margin:'12px 0'}} />;
             if (line.trim() === '') return <br key={i} />;
             return <p key={i} style={{margin:'4px 0'}} dangerouslySetInnerHTML={{__html: line.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')}} />;
})}
          </div>
        </div>
      ) : null}
    </div>
  );
}
