import { describe, expect, it } from "vitest";
import { nextSet, setPayload, todayLocal, workoutPayload } from "./utils";
import { draftSet, workout } from "./test/fixtures";

describe("set entry boundaries", () => {
  it("accepts Finnish decimal comma without performing strength arithmetic", () => {
    expect(
      setPayload(
        { weight: "82,25", reps: "6", rpe: "8.5", type: "WORKING" },
        true,
      ),
    ).toEqual({
      weightKg: 82.25,
      repetitions: 6,
      rpe: 8.5,
      setType: "WORKING",
      completed: true,
    });
  });
  it.each([
    ["-1", "5"],
    ["82,251", "5"],
    ["10000", "5"],
    ["1e2", "5"],
    ["20", "-1"],
    ["20", "1.5"],
    ["20", "1001"],
  ])("rejects invalid weight/reps %s / %s", (weight, reps) => {
    expect(() =>
      setPayload({ weight, reps, rpe: "", type: "WORKING" }, true),
    ).toThrow();
  });
  it("allows a zero load but not a completed set with zero repetitions", () => {
    expect(
      setPayload({ weight: "0", reps: "1", rpe: "", type: "WORKING" }, true)
        .weightKg,
    ).toBe(0);
    expect(() =>
      setPayload({ weight: "0", reps: "0", rpe: "", type: "WORKING" }, true),
    ).toThrow("vähintään yhden");
  });
  it("keeps blank draft fields unknown rather than converting them to zeros", () => {
    expect(
      setPayload({ weight: "", reps: "", rpe: "", type: "WORKING" }, false),
    ).toEqual({
      weightKg: null,
      repetitions: null,
      rpe: null,
      setType: "WORKING",
      completed: false,
    });
  });
  it("copies previous completed load and reps but never assumes the next RPE or completion", () => {
    const previous = {
      ...draftSet,
      weightKg: 0,
      repetitions: 8,
      rpe: 9,
      completed: true,
    };
    expect(nextSet([previous, { ...draftSet, id: 13, weightKg: 40 }])).toEqual({
      weightKg: 0,
      repetitions: 8,
      rpe: null,
      setType: "WORKING",
      completed: false,
    });
  });
  it("uses local calendar components rather than a UTC date for a new session", () => {
    expect(todayLocal(new Date(2026, 9, 22, 0, 1))).toBe("2026-10-22");
  });
  it("preserves optional metadata/timestamps in full replacement requests", () => {
    expect(workoutPayload(workout).startedAt).toBe("2026-10-22T08:22:33.123Z");
    expect(workoutPayload(workout)).not.toHaveProperty("exercises");
  });
});
