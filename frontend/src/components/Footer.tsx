const links = [
  { label: "GitHub", href: "https://github.com/roopegarcia" },
  { label: "Portfolio", href: "https://robertogarcia.iltapaska.com" },
  { label: "Blogi", href: "https://iltapaska.com" },
];

export function Footer() {
  return (
    <footer className="app-footer">
      <div className="footer-top">
        <div className="footer-intro">
          <h2>Roberto Garcia</h2>
          <p>Voimaharjoittelun päiväkirja. Rakennettu Javalla ja Reactilla.</p>
        </div>
        <nav className="footer-links" aria-label="Tekijän linkit">
          {links.map(({ label, href }) => (
            <a key={href} href={href}>
              {label}
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <path d="M7 17 17 7M7 7h10v10" />
              </svg>
            </a>
          ))}
        </nav>
      </div>
      <div className="footer-bottom">
        <small>© {new Date().getFullYear()} Roberto Garcia</small>
      </div>
    </footer>
  );
}
