import { beforeEach, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import axe from "axe-core";
import { App } from "./App";
import { jsonResponse, workout } from "./test/fixtures";

beforeEach(() => {
  window.history.replaceState(null, "", "#/workouts");
  localStorage.clear();
});
const empty = { items: [], page: 0, size: 8, totalElements: 0, totalPages: 0 };
it("has no automated axe semantic violations on the empty workout view", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse(empty)));
  render(<App />);
  await screen.findByText("Ensimmäinen harjoitus odottaa");
  const results = await axe.run(document.body, {
    rules: { "color-contrast": { enabled: false } },
  });
  expect(results.violations).toEqual([]);
});
it("has no automated axe semantic violations on a loaded workout editor", async () => {
  window.history.replaceState(null, "", "#/workouts/1");
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation(() => Promise.resolve(jsonResponse(workout))),
  );
  render(<App />);
  await screen.findByText("Penkkipunnerrus");
  const results = await axe.run(document.body, {
    rules: { "color-contrast": { enabled: false } },
  });
  expect(results.violations).toEqual([]);
});
it("guards unsaved set input when navigating away", async () => {
  window.history.replaceState(null, "", "#/workouts/1");
  const user = userEvent.setup();
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation(() => Promise.resolve(jsonResponse(workout))),
  );
  const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
  render(<App />);
  await screen.findByText("Penkkipunnerrus");
  await user.type(screen.getByLabelText(/Paino/), "72,5");
  await user.click(screen.getByRole("link", { name: "Liikkeet" }));
  await waitFor(() => expect(confirm).toHaveBeenCalled());
  expect(window.location.hash).toBe("#/workouts/1");
  expect(screen.getByLabelText(/Paino/)).toHaveValue("72,5");
});
it("starts a named workout without an erroneous discard prompt after saving", async () => {
  const user = userEvent.setup();
  const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((path: string, options: RequestInit) => {
      if (options.method === "POST")
        return Promise.resolve(
          jsonResponse(
            { ...workout, id: 3, title: "Jalkapäivä", exercises: [] },
            201,
          ),
        );
      return Promise.resolve(
        jsonResponse(
          path === "/api/workouts/3"
            ? { ...workout, id: 3, title: "Jalkapäivä", exercises: [] }
            : empty,
        ),
      );
    }),
  );
  render(<App />);
  await screen.findByText("Ensimmäinen harjoitus odottaa");
  await user.click(screen.getByRole("button", { name: "Aloita harjoitus" }));
  await user.type(screen.getByLabelText(/Harjoituksen nimi/), "Jalkapäivä");
  await user.click(screen.getByRole("button", { name: "Aloita nyt" }));
  await waitFor(() =>
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(
      "Jalkapäivä",
    ),
  );
  expect(confirm).not.toHaveBeenCalled();
});
it("stores only the theme preference and provides a working skip link", async () => {
  const user = userEvent.setup();
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse(empty)));
  render(<App />);
  await screen.findByText("Ensimmäinen harjoitus odottaa");
  await user.click(screen.getByRole("button", { name: "Tumma teema" }));
  expect(document.documentElement.dataset.theme).toBe("dark");
  expect(localStorage.getItem("puntti-urpo-theme")).toBe("dark");
  expect(localStorage.length).toBe(1);
  await user.click(screen.getByRole("link", { name: "Siirry sisältöön" }));
  expect(screen.getByRole("main")).toHaveFocus();
  expect(window.location.hash).toBe("#/workouts");
});

it("keeps timestamp precision and unsaved row input when saving workout metadata", async () => {
  window.history.replaceState(null, "", "#/workouts/1");
  const user = userEvent.setup();
  const fetch = vi
    .fn()
    .mockImplementation((_path: string, options: RequestInit) =>
      Promise.resolve(
        jsonResponse(
          options.method === "PUT"
            ? { ...workout, title: "Uusi otsikko" }
            : workout,
        ),
      ),
    );
  vi.stubGlobal("fetch", fetch);
  render(<App />);
  await screen.findByText("Penkkipunnerrus");
  await user.type(screen.getByLabelText(/Paino/), "72,5");
  await user.click(screen.getByText("Harjoituksen tiedot ja muistiinpanot"));
  await user.clear(screen.getByLabelText("Nimi"));
  await user.type(screen.getByLabelText("Nimi"), "Uusi otsikko");
  await user.click(screen.getByRole("button", { name: "Tallenna tiedot" }));
  await waitFor(() =>
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(
      "Uusi otsikko",
    ),
  );
  const put = fetch.mock.calls.find(([, options]) => options.method === "PUT");
  expect(JSON.parse(put![1].body as string).startedAt).toBe(
    "2026-10-22T08:22:33.123Z",
  );
  expect(screen.getByLabelText(/Paino/)).toHaveValue("72,5");
});
it("retains a custom exercise form after a duplicate-name response", async () => {
  window.history.replaceState(null, "", "#/exercises");
  const user = userEvent.setup();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockImplementation((_path: string, options: RequestInit) =>
        Promise.resolve(
          jsonResponse(
            options.method === "POST"
              ? { detail: "Samanniminen liike on jo olemassa." }
              : empty,
            options.method === "POST" ? 409 : 200,
          ),
        ),
      ),
  );
  render(<App />);
  await screen.findByText("Liikettä ei löytynyt");
  await user.click(screen.getByRole("button", { name: "Oma liike" }));
  await user.type(screen.getByLabelText(/Nimi/), "Penkkipunnerrus");
  await user.click(screen.getByRole("button", { name: "Tallenna liike" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Samanniminen");
  expect(screen.getByLabelText(/Nimi/)).toHaveValue("Penkkipunnerrus");
});
it("keeps the persisted logged exercise visible when creating its first set fails", async () => {
  window.history.replaceState(null, "", "#/workouts/1");
  const user = userEvent.setup();
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((path: string, options: RequestInit) => {
      if (path.endsWith("/sets"))
        return Promise.resolve(
          jsonResponse({ detail: "Tilapäinen virhe." }, 500),
        );
      if (options.method === "POST")
        return Promise.resolve(
          jsonResponse({ ...workout.exercises[0], id: 99, sets: [] }, 201),
        );
      return Promise.resolve(
        jsonResponse(
          path.startsWith("/api/exercises?")
            ? {
                ...empty,
                items: [workout.exercises[0].exercise],
                totalElements: 1,
              }
            : { ...workout, exercises: [] },
        ),
      );
    }),
  );
  render(<App />);
  await screen.findByText("Mistä aloitetaan?");
  await user.click(screen.getByRole("button", { name: "Valitse liike" }));
  await screen.findByText("Penkkipunnerrus");
  await user.click(screen.getByRole("button", { name: /Lisää harjoitukseen/ }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Liike lisättiin");
  expect(
    screen.getByRole("heading", { name: "Penkkipunnerrus" }),
  ).toBeVisible();
  expect(screen.getByRole("button", { name: /Lisää sarja/ })).toBeVisible();
});

it("focuses each newly appended set without returning to the first set", async () => {
  window.history.replaceState(null, "", "#/workouts/1");
  const user = userEvent.setup();
  let nextId = 20;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((path: string, options: RequestInit) => {
      if (path.endsWith("/sets"))
        return Promise.resolve(
          jsonResponse({ ...workout.exercises[0].sets[0], id: nextId++ }, 201),
        );
      if (options.method === "POST")
        return Promise.resolve(
          jsonResponse({ ...workout.exercises[0], sets: [] }, 201),
        );
      return Promise.resolve(
        jsonResponse(
          path.startsWith("/api/exercises?")
            ? {
                ...empty,
                items: [workout.exercises[0].exercise],
                totalElements: 1,
              }
            : { ...workout, exercises: [] },
        ),
      );
    }),
  );
  render(<App />);
  await screen.findByText("Mistä aloitetaan?");
  await user.click(screen.getByRole("button", { name: "Valitse liike" }));
  await user.click(
    await screen.findByRole("button", { name: /Lisää harjoitukseen/ }),
  );
  await waitFor(() => expect(screen.getByLabelText(/Paino/)).toHaveFocus());
  await user.click(screen.getByRole("button", { name: /Lisää sarja/ }));
  await waitFor(() =>
    expect(screen.getAllByLabelText(/Paino/)[1]).toHaveFocus(),
  );
});

it("disables mutation controls in the demo workout editor", async () => {
  window.history.replaceState(null, "", "#/workouts/1");
  const fetch = vi.fn().mockResolvedValue(jsonResponse(workout));
  vi.stubGlobal("fetch", fetch);
  render(<App account={{ username: "demo", readOnly: true }} />);
  await screen.findByText("Penkkipunnerrus");
  expect(screen.getByLabelText(/Paino/)).toBeDisabled();
  expect(screen.getByRole("button", { name: /Merkitse sarja/ })).toBeDisabled();
  expect(
    screen.getByRole("button", { name: "Poista koko harjoitus" }),
  ).toBeDisabled();
  expect(
    fetch.mock.calls.every(([, options]) => options.method === "GET"),
  ).toBe(true);
});
