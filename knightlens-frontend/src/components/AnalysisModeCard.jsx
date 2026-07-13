// src/components/AnalysisModeCard.jsx
//
// The Quick/Deep selector card.
// WHY SEPARATE?
// This card has its own interactive state (selected/unselected),
// its own keyboard behavior, and its own visual logic.
// Extracting it makes AnalysisForm simpler and this easier to test.
//
// PROPS:
//   mode       — object from ANALYSIS_MODES constant (key, label, sub, badge, bullets)
//   selected   — boolean, is this card currently selected?
//   onSelect   — function called when user clicks or presses Enter/Space

export default function AnalysisModeCard({ mode, selected, onSelect }) {
  // WHY onKeyDown?
  // By default, only <button> elements handle Enter/Space for activation.
  // Since we're using a <div> for the card layout, we add keyboard handling
  // manually so keyboard users can select modes without a mouse.
  const handleKeyDown = (e) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      onSelect(mode.key);
    }
  };

  return (
    <div
      role="radio"
      // aria-pressed tells screen readers this is a toggle/selectable control
      // aria-checked is used for radio-button semantics
      aria-checked={selected}
      tabIndex={0}
      onClick={() => onSelect(mode.key)}
      onKeyDown={handleKeyDown}
      style={{
        padding: "14px 16px",
        borderRadius: 10,
        cursor: "pointer",
        background: selected ? "#1a2a3a" : "#0d1117",
        border: `${selected ? 2 : 1}px solid ${selected ? "#58a6ff" : "#30363d"}`,
        transition: "all 0.15s",
        outline: "none",
      }}
      // Focus visible via global CSS button:focus-visible — but since this is a div,
      // we handle it explicitly
      onFocus={(e) => { if (!e.currentTarget.matches(":focus-visible")) return; }}
    >
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 4 }}>
        <div style={{ fontSize: 13, fontWeight: 600, color: selected ? "#58a6ff" : "#e6edf3" }}>
          {selected && <span aria-hidden="true" style={{ marginRight: 6 }}>✓</span>}
          {mode.label}
        </div>
        <span style={{
          fontSize: 10, fontWeight: 600, padding: "2px 7px", borderRadius: 20,
          background: selected ? "#1f3a5f" : "#21262d",
          color: selected ? "#58a6ff" : "#8b949e",
          border: `1px solid ${selected ? "#1f6feb" : "#30363d"}`,
          marginLeft: 8,
        }}>
          {mode.badge}
        </span>
      </div>
      <div style={{ fontSize: 11, color: "#8b949e", marginBottom: 8 }}>{mode.sub}</div>
      {mode.bullets.map((b) => (
        <div key={b} style={{ fontSize: 11, color: "#8b949e", marginTop: 3 }}>· {b}</div>
      ))}
    </div>
  );
}
