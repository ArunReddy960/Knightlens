// src/components/Badge.jsx
//
// WHY A SEPARATE COMPONENT?
// Badges (small colored labels) appear in many places:
// job status, depth mode, game count, etc.
// Instead of copy-pasting the same span 10 times,
// we define it once and pass different props.
// Same concept as a reusable Java utility method.
//
// PROPS:
//   children  — the text inside the badge (e.g. "Quick", "COMPLETED")
//   variant   — controls the color scheme

const VARIANT_STYLES = {
  neutral: { bg: "#21262d", color: "#8b949e", border: "#30363d" },
  blue:    { bg: "#1f3a5f", color: "#58a6ff", border: "#1f6feb" },
  green:   { bg: "#1a3a2a", color: "#3fb950", border: "#2ea043" },
  red:     { bg: "#3a1a1a", color: "#f85149", border: "#da3633" },
  amber:   { bg: "#3a2a0a", color: "#d29922", border: "#9e6a03" },
};

export default function Badge({ children, variant = "neutral" }) {
  const c = VARIANT_STYLES[variant] || VARIANT_STYLES.neutral;

  return (
    <span
      style={{
        display: "inline-block",
        padding: "2px 8px",
        borderRadius: "20px",
        fontSize: "11px",
        fontWeight: 600,
        background: c.bg,
        color: c.color,
        border: `1px solid ${c.border}`,
        lineHeight: "1.6",
        whiteSpace: "nowrap",
      }}
    >
      {children}
    </span>
  );
}
