// src/components/PhaseCard.jsx
//
// Displays stats for a single chess phase (opening, middlegame, endgame).
// WHY SEPARATE?
// Used 3 times in ResultsDashboard. Extracting avoids duplication
// and makes each card independently testable.
//
// PROPS:
//   phase — PhaseStats object from backend:
//     { phaseName, totalMoves, accuracyPercentage, averageCentipawnLoss,
//       bestCount, excellentCount, goodCount, inaccuracyCount, mistakeCount, blunderCount }

import { PHASE_COLORS } from "../constants/analysisConfig";

export default function PhaseCard({ phase }) {
  // DEFENSIVE RENDERING:
  // WHY these checks? If phase is null/undefined or fields are missing,
  // calling .toFixed() on null crashes the whole page.
  // Same as null-checking in Java before calling methods on an object.
  if (!phase) return null;

  const color = PHASE_COLORS[phase.phaseName] || "#58a6ff";
  const accuracy = typeof phase.accuracyPercentage === "number"
    ? phase.accuracyPercentage.toFixed(1)
    : "–";
  const name = phase.phaseName
    ? phase.phaseName.charAt(0).toUpperCase() + phase.phaseName.slice(1)
    : "Unknown";

  const qualityRows = [
    ["Best",       phase.bestCount,        false],
    ["Excellent",  phase.excellentCount,   false],
    ["Good",       phase.goodCount,        false],
    ["Inaccuracy", phase.inaccuracyCount,  false],
    ["Mistake",    phase.mistakeCount,     (phase.mistakeCount ?? 0) > 0],
    ["Blunder",    phase.blunderCount,     (phase.blunderCount ?? 0) > 0],
  ];

  return (
    <article
      aria-label={`${name} phase performance`}
      style={{
        background: "#0d1117",
        border: "1px solid #21262d",
        borderRadius: 10,
        padding: 20,
        borderTop: `3px solid ${color}`,
      }}
    >
      {/* Phase name */}
      <div style={{
        fontSize: 11, fontWeight: 600, textTransform: "uppercase",
        letterSpacing: "0.08em", color: "#8b949e", marginBottom: 12,
      }}>
        {name}
      </div>

      {/* Accuracy number */}
      <div style={{ fontSize: 34, fontWeight: 700, color: "#e6edf3", lineHeight: 1, marginBottom: 4 }}>
        {accuracy}%
      </div>
      <div style={{ fontSize: 12, color: "#8b949e", marginBottom: 12 }}>accuracy</div>

      {/* Progress bar */}
      <div style={{ height: 6, borderRadius: 3, background: "#21262d", overflow: "hidden", marginBottom: 16 }}>
        <div style={{
          height: "100%",
          width: `${Math.min(100, Math.max(0, phase.accuracyPercentage ?? 0))}%`,
          background: color,
          borderRadius: 3,
          transition: "width 0.8s ease",
        }} />
      </div>

      {/* Quality breakdown */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "4px 12px" }}>
        {qualityRows.map(([label, val, warn]) => (
          <div key={label} style={{ display: "flex", justifyContent: "space-between", fontSize: 12, padding: "2px 0" }}>
            <span style={{ color: "#8b949e" }}>{label}</span>
            <span style={{ fontWeight: 600, color: warn ? "#f85149" : "#e6edf3" }}>
              {val ?? "–"}
            </span>
          </div>
        ))}
      </div>

      {/* Total moves */}
      <div style={{ marginTop: 10, fontSize: 11, color: "#8b949e" }}>
        {phase.totalMoves ?? 0} moves analyzed
      </div>
    </article>
  );
}
