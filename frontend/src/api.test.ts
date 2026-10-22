import { expect, it, vi } from "vitest";
import { api } from "./api";
import { jsonResponse } from "./test/fixtures";

it("handles successful empty DELETE responses without trying to decode JSON", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(new Response(null, { status: 204 })),
  );
  await expect(
    api("/api/sets/1", { method: "DELETE" }),
  ).resolves.toBeUndefined();
});
it("shows safe validation messages from ProblemDetail field errors", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        jsonResponse(
          {
            detail: "Tarkista tiedot.",
            errors: [{ field: "name", message: "Nimi on pakollinen." }],
          },
          400,
        ),
      ),
  );
  await expect(
    api("/api/exercises", { method: "POST", body: {} }),
  ).rejects.toThrow("Nimi on pakollinen.");
});
it("does not automatically repeat a mutation after a lost response", async () => {
  const fetch = vi.fn().mockRejectedValue(new TypeError("offline"));
  vi.stubGlobal("fetch", fetch);
  await expect(
    api("/api/workouts", { method: "POST", body: {} }),
  ).rejects.toThrow("ennen tallennuksen uusimista");
  expect(fetch).toHaveBeenCalledTimes(1);
});
it("handles a non-JSON gateway response without leaking its raw body", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        new Response("<pre>private diagnostic</pre>", { status: 502 }),
      ),
  );
  await expect(api("/api/workouts")).rejects.toThrow(
    "Tietoja ei voitu käsitellä",
  );
});
