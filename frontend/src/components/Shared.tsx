import { useRef, useState } from "react";
import type { ReactNode } from "react";
export function Icon({
  name,
  size = 20,
}: {
  name:
    | "barbell"
    | "plus"
    | "check"
    | "arrow"
    | "sun"
    | "moon"
    | "grid"
    | "list"
    | "trash";
  size?: number;
}) {
  const paths = {
    barbell: "M3 8v8M7 5v14M17 5v14M21 8v8M7 12h10",
    plus: "M12 5v14M5 12h14",
    check: "m5 12 4 4L19 6",
    arrow: "M5 12h14m-6-6 6 6-6 6",
    sun: "M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1.5 1.5m11 11L19 19M5 19l1.5-1.5m11-11L19 5M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0",
    moon: "M20.5 14A8.5 8.5 0 0 1 10 3.5 8.5 8.5 0 1 0 20.5 14Z",
    grid: "M4 4h6v6H4Zm10 0h6v6h-6ZM4 14h6v6H4Zm10 0h6v6h-6Z",
    list: "M9 6h11M9 12h11M9 18h11M4 6h.01M4 12h.01M4 18h.01",
    trash: "M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13M10 10v7m4-7v7",
  };
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d={paths[name]} />
    </svg>
  );
}
export function ErrorMessage({
  message,
  retry,
}: {
  message: string;
  retry?: () => void;
}) {
  return (
    <div className="error" role="alert">
      <p>{message}</p>
      {retry && (
        <button className="button secondary" onClick={retry}>
          Yritä uudelleen
        </button>
      )}
    </div>
  );
}
export function Loading() {
  return (
    <p className="loading" role="status">
      Ladataan tietoja…
    </p>
  );
}
export function Empty({
  title,
  children,
}: {
  title: string;
  children: ReactNode;
}) {
  return (
    <div className="empty">
      <span className="empty-icon">
        <img src="/logo-symbol.svg" width="40" height="40" alt="" />
      </span>
      <h2>{title}</h2>
      {children}
    </div>
  );
}
export function ConfirmDelete({
  label,
  question,
  onConfirm,
  disabled = false,
}: {
  label: string;
  question: string;
  onConfirm: () => Promise<void>;
  disabled?: boolean;
}) {
  const [open, setOpen] = useState(false);
  const trigger = useRef<HTMLButtonElement>(null);
  function close() {
    setOpen(false);
    requestAnimationFrame(() => trigger.current?.focus());
  }
  if (!open)
    return (
      <button
        ref={trigger}
        type="button"
        className="icon-button danger-quiet"
        aria-label={label}
        title={label}
        disabled={disabled}
        onClick={() => setOpen(true)}
      >
        <Icon name="trash" />
      </button>
    );
  return (
    <div className="confirm" role="group" aria-label={question}>
      <span>{question} Poistoa ei voi perua.</span>
      <button
        type="button"
        className="button danger"
        disabled={disabled}
        onClick={() => void onConfirm().finally(close)}
      >
        Poista
      </button>
      <button
        type="button"
        className="button secondary"
        disabled={disabled}
        onClick={close}
      >
        Peruuta
      </button>
    </div>
  );
}
export function Pager({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;
  return (
    <nav className="pager" aria-label="Sivutus">
      <button
        className="button secondary"
        disabled={page === 0}
        onClick={() => onChange(page - 1)}
      >
        Edellinen
      </button>
      <span>
        Sivu {page + 1} / {totalPages}
      </span>
      <button
        className="button secondary"
        disabled={page + 1 >= totalPages}
        onClick={() => onChange(page + 1)}
      >
        Seuraava
      </button>
    </nav>
  );
}
