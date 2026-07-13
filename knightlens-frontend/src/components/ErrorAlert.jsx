// src/components/ErrorAlert.jsx
//
// Displays user-friendly error messages with a Retry button.
// WHY SEPARATE?
// Error display appears in multiple places (form, progress, results).
// Centralizing it means consistent styling and messaging everywhere.
//
// PROPS:
//   message  — string to display (should be user-friendly, no stack traces)
//   onRetry  — function called when user clicks Retry

export default function ErrorAlert({ message, onRetry }) {
  if (!message) return null;

  return (
    // aria-live="assertive" tells screen readers to announce this immediately
    // when it appears — important for accessibility
    <div
      role="alert"
      aria-live="assertive"
      style={{
        background: "var(--color-danger-bg)",
        border: "1px solid var(--color-danger-border)",
        borderRadius: "var(--radius-md)",
        padding: "14px 18px",
        marginBottom: 20,
        display: "flex",
        alignItems: "center",
        gap: 12,
      }}
    >
      <span style={{ color: "var(--color-danger)", flex: 1, fontSize: 13 }}>
        {message}
      </span>
      {onRetry && (
        <button
          onClick={onRetry}
          style={{
            padding: "5px 16px",
            borderRadius: "var(--radius-sm)",
            background: "transparent",
            border: "1px solid var(--color-danger-border)",
            color: "var(--color-danger)",
            cursor: "pointer",
            fontSize: 12,
            fontWeight: 500,
          }}
        >
          Retry
        </button>
      )}
    </div>
  );
}
