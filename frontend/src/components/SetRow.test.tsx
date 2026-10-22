import { useState } from "react";
import { expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { SetRow } from "./SetRow";
import { draftSet, jsonResponse, quietFeedback } from "../test/fixtures";
import type { WorkoutSet } from "../types";

function Harness({ initial = draftSet }: { initial?: WorkoutSet }) {
  const [set, update] = useState(initial);
  return (
    <SetRow
      set={set}
      number={1}
      onUpdate={update}
      onDelete={() => {}}
      feedback={quietFeedback}
    />
  );
}
it("saves the entered set in one completion action and displays backend estimates", async () => {
  const user = userEvent.setup();
  const fetch = vi
    .fn()
    .mockResolvedValue(
      jsonResponse({
        ...draftSet,
        weightKg: 82.25,
        repetitions: 6,
        rpe: 8.5,
        completed: true,
        estimates: {
          estimatedRir: 1.5,
          repetitionBasedE1rmKg: 98.7,
          e1rmStatus: "AVAILABLE",
          warnings: ["SUBMAXIMAL_RPE_NOT_ADJUSTED"],
        },
      }),
    );
  vi.stubGlobal("fetch", fetch);
  render(<Harness />);
  await user.type(screen.getByLabelText(/Paino/), "82,25");
  await user.type(screen.getByLabelText("Toistot"), "6");
  await user.selectOptions(screen.getByLabelText(/RPE/), "8.5");
  await user.click(
    screen.getByRole("button", { name: "Merkitse sarja 1 valmiiksi" }),
  );
  await waitFor(() => expect(screen.getByText("98,7 kg")).toBeVisible());
  expect(JSON.parse(fetch.mock.calls[0][1].body)).toEqual({
    weightKg: 82.25,
    repetitions: 6,
    rpe: 8.5,
    setType: "WORKING",
    completed: true,
  });
  expect(screen.getByText(/e1RM ei huomioi/)).toBeVisible();
  expect(screen.queryByText(/Muutoksia ei ole vielä/)).not.toBeInTheDocument();
});
it("keeps entered values and announces an error after a network failure", async () => {
  const user = userEvent.setup();
  vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("offline")));
  render(<Harness />);
  await user.type(screen.getByLabelText(/Paino/), "60,5");
  await user.type(screen.getByLabelText("Toistot"), "8");
  await user.click(
    screen.getByRole("button", { name: "Merkitse sarja 1 valmiiksi" }),
  );
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Yhteys palvelimeen katkesi",
  );
  expect(screen.getByLabelText(/Paino/)).toHaveValue("60,5");
  expect(screen.getByLabelText("Toistot")).toHaveValue("8");
});
it("never submits incomplete completed performance", async () => {
  const user = userEvent.setup();
  const fetch = vi.fn();
  vi.stubGlobal("fetch", fetch);
  render(<Harness />);
  await user.click(
    screen.getByRole("button", { name: "Merkitse sarja 1 valmiiksi" }),
  );
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Valmis sarja tarvitsee painon",
  );
  expect(fetch).not.toHaveBeenCalled();
});
it("requires an explicit deletion choice and cancellation sends no request", async () => {
  const user = userEvent.setup();
  const fetch = vi.fn();
  vi.stubGlobal("fetch", fetch);
  render(<Harness />);
  await user.click(screen.getByRole("button", { name: "Poista sarja 1" }));
  expect(
    screen.getByRole("group", { name: /Poistetaanko sarja/ }),
  ).toBeVisible();
  await user.click(screen.getByRole("button", { name: "Peruuta" }));
  expect(fetch).not.toHaveBeenCalled();
});
