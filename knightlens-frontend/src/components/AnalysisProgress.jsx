// src/components/AnalysisProgress.jsx
//
// Shows while a job is PENDING or IN_PROGRESS.
// WHY NO FAKE PROGRESS?
// The backend doesn't return per-stage progress data.
// Faking it (e.g., "moving through stages based on time") is dishonest —
// it shows the user information the system doesn't actually know.
// Instead, we show an indeterminate state — "in progress, we don't know exactly where" —
// which is honest and still informative.
//
// PROPS:
//   username   — string, for display
//   mode       — "quick" | "deep"
//   depth      — number (12 or 18)
//   gameCount  — number
//   jobId      — number
//   status     — "PENDING" | "IN_PROGRESS"
//   elapsed    — number, seconds elapsed since job started

import Badge from "./Badge";

const STATUS_COLORS = {
  PENDING: "neutral",
  IN_PROGRESS: "blue",
  COMPLETED: "green",
  FAILED: "red",
};

export default function AnalysisProgress({ username, mode, depth, gameCount, jobId, status, elapsed }) {
  const isActive = status === "IN_PROGRESS";
  const isPending = status === "PENDING";

  const statusLabel = isPending
    ? "Waiting to start..."
    : isActive
    ? "Analysis in progress"
    : status;

  const elapsedDisplay = elapsed > 0
    ? elapsed >= 60
      ? `${Math.floor(elapsed / 60)}m ${elapsed % 60}s elapsed`
      : `${elapsed}s elapsed`
    : null;

  return (
    <div style={{
      background: "#161b22",
      border: "1px solid #21262d",
      borderRadius: 12,
      padding: 28,
      marginBottom: 24,
    }}>
      {/* Header */}
      <div style={{ fontSize: 15, fontWeight: 600, color: "#e6edf3", marginBottom: 4 }}>
        Analyzing {username}
      </div>

      {/* Job metadata badges */}
      <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 24 }}>
        <Badge variant={mode === "quick" ? "neutral" : "blue"}>
          {mode === "quick" ? "Quick" : "Deep"}
        </Badge>
        <Badge variant="neutral">depth {depth}</Badge>
        <Badge variant="neutral">{gameCount} games</Badge>
        <Badge variant={STATUS_COLORS[status] || "neutral"}>{status}</Badge>
      </div>

      {/* INDETERMINATE PROGRESS BAR
       * WHY INDETERMINATE?
       * We don't know the real % complete — the backend doesn't expose it.
       * An indeterminate bar (pulsing animation) honestly represents
       * "something is happening, we don't know how far along" —
       * which is exactly the truth. Faking a % would be misleading.
       */}
      <div style={{
        height: 6,
        borderRadius: 3,
        background: "#21262d",
        overflow: "hidden",
        marginBottom: 12,
      }}>
        {isActive && (
          <div style={{
            height: "100%",
            width: "40%",
            background: "linear-gradient(90deg, transparent, #58a6ff, transparent)",
            borderRadius: 3,
            animation: "indeterminate 1.5s ease-in-out infinite",
          }} />
        )}
        {isPending && (
          <div style={{
            height: "100%",
            width: "8%",
            background: "#8b949e",
            borderRadius: 3,
          }} />
        )}
      </div>

      {/* Status message */}
      <div
        role="status"
        aria-live="polite"
        style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13, color: "#8b949e", marginBottom: 16 }}
      >
        {isActive && <span className="spinner" aria-hidden="true" />}
        <span>{statusLabel}</span>
      </div>

      {/* Footer: job ID + elapsed */}
      <div style={{ fontSize: 11, color: "#8b949e", display: "flex", gap: 16, flexWrap: "wrap" }}>
        <span>Job #{jobId}</span>
        <span>Polling every 5 seconds</span>
        {elapsedDisplay && <span>{elapsedDisplay}</span>}
      </div>

      {/* Indeterminate animation keyframe — inline since it's specific to this component */}
      <style>{`
        @keyframes indeterminate {
          0%   { transform: translateX(-200%); }
          100% { transform: translateX(350%); }
        }
      `}</style>
    </div>
  );
}
