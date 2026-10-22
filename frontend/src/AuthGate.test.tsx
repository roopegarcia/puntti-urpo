import { beforeEach, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import axe from "axe-core";
import { AuthGate } from "./AuthGate";
import { jsonResponse } from "./test/fixtures";
import { setCsrf } from "./api";

const empty = { items: [], page: 0, size: 8, totalElements: 0, totalPages: 0 };
beforeEach(() => {
  window.history.replaceState(null, "", "#/workouts");
  localStorage.clear();
  setCsrf(null);
});

it("shows an accessible login without fetching private records before authentication", async () => {
  const fetch = vi
    .fn()
    .mockImplementation((path: string) =>
      Promise.resolve(
        path.endsWith("/csrf")
          ? jsonResponse({ token: "masked", headerName: "X-CSRF-TOKEN" })
          : jsonResponse({ detail: "Kirjaudu sisään." }, 401),
      ),
    );
  vi.stubGlobal("fetch", fetch);
  render(<AuthGate />);
  await screen.findByRole("heading", { name: "Tervetuloa takaisin." });
  expect(
    fetch.mock.calls.every(([path]) => path.startsWith("/api/auth/")),
  ).toBe(true);
  expect(
    (
      await axe.run(document.body, {
        rules: { "color-contrast": { enabled: false } },
      })
    ).violations,
  ).toEqual([]);
});

it("logs in with CSRF, refreshes the token, then clears private UI on logout", async () => {
  let signedIn = false;
  const fetch = vi
    .fn()
    .mockImplementation((path: string, _options?: RequestInit) => {
      if (path.endsWith("/csrf"))
        return Promise.resolve(
          jsonResponse({
            token: signedIn ? "fresh" : "initial",
            headerName: "X-CSRF-TOKEN",
          }),
        );
      if (path.endsWith("/login")) {
        signedIn = true;
        return Promise.resolve(new Response(null, { status: 204 }));
      }
      if (path.endsWith("/logout")) {
        signedIn = false;
        return Promise.resolve(new Response(null, { status: 204 }));
      }
      if (path.endsWith("/me"))
        return Promise.resolve(
          signedIn
            ? jsonResponse({ username: "owner", readOnly: false })
            : jsonResponse({}, 401),
        );
      return Promise.resolve(jsonResponse(empty));
    });
  vi.stubGlobal("fetch", fetch);
  const user = userEvent.setup();
  render(<AuthGate />);
  await screen.findByRole("button", { name: "Kirjaudu sisään" });
  await user.type(screen.getByLabelText("Salasana"), "not-a-real-password");
  await user.click(screen.getByRole("button", { name: "Kirjaudu sisään" }));
  await screen.findByText("Ensimmäinen harjoitus odottaa");
  const login = fetch.mock.calls.find(([path]) => path.endsWith("/login"))!;
  expect((login[1]!.headers as Record<string, string>)["X-CSRF-TOKEN"]).toBe(
    "initial",
  );
  expect(login[1]!.body).toBeInstanceOf(URLSearchParams);
  await user.click(screen.getByRole("button", { name: "Kirjaudu ulos" }));
  await screen.findByRole("heading", { name: "Tervetuloa takaisin." });
  expect(
    screen.queryByText("Ensimmäinen harjoitus odottaa"),
  ).not.toBeInTheDocument();
  expect(localStorage.length).toBe(0);
});

it("shows demo mode without create controls or private custom exercises", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockImplementation((path: string) =>
        Promise.resolve(
          jsonResponse(
            path.endsWith("/csrf")
              ? { token: "masked", headerName: "X-CSRF-TOKEN" }
              : path.endsWith("/me")
                ? { username: "demo", readOnly: true }
                : empty,
          ),
        ),
      ),
  );
  const user = userEvent.setup();
  render(<AuthGate />);
  await screen.findByText(/Demotili — vain kuvitteellisia/);
  expect(
    screen.queryByRole("button", { name: "Aloita harjoitus" }),
  ).not.toBeInTheDocument();
  await user.click(screen.getByRole("link", { name: "Liikkeet" }));
  await waitFor(() =>
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(
      "Liikekirjasto",
    ),
  );
  expect(
    screen.queryByRole("button", { name: "Oma liike" }),
  ).not.toBeInTheDocument();
});

it("keeps unsaved set input through expiry and reauthentication as the same account", async () => {
  const { workout } = await import("./test/fixtures");
  window.history.replaceState(null, "", "#/workouts/1");
  let signedIn = true;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((path: string, options?: RequestInit) => {
      if (path.endsWith("/csrf"))
        return Promise.resolve(
          jsonResponse({ token: "masked", headerName: "X-CSRF-TOKEN" }),
        );
      if (path.endsWith("/login")) {
        signedIn = true;
        return Promise.resolve(new Response(null, { status: 204 }));
      }
      if (path.endsWith("/me"))
        return Promise.resolve(
          signedIn
            ? jsonResponse({ username: "owner", readOnly: false })
            : jsonResponse({}, 401),
        );
      if (options?.method === "PUT") {
        signedIn = false;
        return Promise.resolve(
          jsonResponse({ detail: "Tarkista kirjautuminen." }, 403),
        );
      }
      return Promise.resolve(jsonResponse(workout));
    }),
  );
  const user = userEvent.setup();
  render(<AuthGate />);
  await screen.findByText("Penkkipunnerrus");
  await user.type(screen.getByLabelText(/Paino/), "72,5");
  await user.click(screen.getByRole("button", { name: "Tallenna muutokset" }));
  await screen.findByRole("heading", { name: "Kirjaudu uudelleen" });
  await user.type(screen.getByLabelText("Salasana"), "not-a-real-password");
  await user.click(screen.getByRole("button", { name: "Kirjaudu sisään" }));
  await waitFor(() =>
    expect(
      screen.queryByRole("heading", { name: "Kirjaudu uudelleen" }),
    ).not.toBeInTheDocument(),
  );
  expect(screen.getByLabelText(/Paino/)).toBeVisible();
  expect(screen.getByLabelText(/Paino/)).toHaveValue("72,5");
});
