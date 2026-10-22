# Training estimates: implemented policy and limitations

## What is implemented

Milestone 4 adds derived `estimates` to every set response, including nested workout/exercise reads. Calculations live in `backend/src/main/java/com/punttiurpo/domain/TrainingCalculations.java`; the immutable result is `SetEstimates.java`. The mapper supplies scalar values rather than handing persistence entities into the calculator. There is no database access, Spring dependency or stored estimate column in this domain code.

Read-only estimates are recalculated from the current recorded set on every response. Existing rows need no migration or backfill. Updating a set changes its estimates immediately; an estimate is not another editable input. The calculation milestone does not create personal-record tracking, progression statistics or load recommendations. Milestone 5 now displays these server estimates in the React UI.

## RPE and approximate RIR

The input uses the resistance-training RIR-based RPE scale, not Borg's separate 6–20 scale. Supported input remains optional RPE 6–10, in half-point steps. For a completed set with a recorded RPE:

`estimatedRir = 10 − RPE`

The result has one decimal place: RPE 10 → 0.0, 9 → 1.0, 8 → 2.0 and 8.5 → 1.5. These are conventional approximations from subjective effort, not measured spare repetitions; fractional values are scale interpolation, not proof a literal half repetition remained. RPE 6 maps to approximately 4.0 under this project's restricted scale. No RPE means null RIR, not zero. Drafts return null RIR even if a draft RPE was entered. Completed warmup/high-repetition/zero-load sets may still have RIR even when e1RM is unavailable.

The RIR-based scale's original study compared experienced and novice squatters and mapped RPE to reported repetitions remaining. It does not make individual RIR objectively measured or validate every exercise. [Zourdos et al. (2016), primary study](https://pubmed.ncbi.nlm.nih.gov/26049792/).

Suggested Finnish UI explanation: “RPE kuvaa omaa arviotasi sarjan rasittavuudesta. RPE 10 ≈ 0, RPE 9 ≈ 1 ja RPE 8 ≈ 2 toistoa varastossa. RIR on likiarvo, ei mitattu tulos.” The request schema in Swagger includes this explanation; Milestone 5 frontend controls now render this explanation.

## Repetition-based Epley estimate

The approved project formula is:

`e1RM = weightKg × (1 + repetitions / 30)`

The equivalent implemented expression is `weightKg × (30 + repetitions) / 30`. All operations use BigDecimal. Multiplication happens before division, avoiding a prematurely rounded repetition factor. Only the final result is rounded to two decimal places with HALF_UP. This is decimal display precision, not a claim the prediction is accurate to 0.01 kg.

Examples: 100 kg × 6 → 120.00 kg; 82.25 kg × 6 → 98.70 kg; 100 kg × 10 → 133.33 kg. A rounding boundary test uses 0.15 kg × 3 → 0.165 → 0.17 kg. Calculations do not change the recorded load/reps, and RPE/RIR are never added to the repetition count in the implemented formula.

**One-repetition product convention:** one completed repetition returns the recorded load exactly (at two decimal places), rather than multiplying by 31/30. It always carries `SINGLE_REPETITION_NOT_VERIFIED_MAXIMUM`. A recorded single is not necessarily a maximal effort or a verified personal record, even if RPE is reported as 10.

### Eligibility and status

The first matching exclusion determines `e1rmStatus`, in this order:

- `NOT_COMPLETED`: draft/incomplete set; neither derived value is presented as completed performance.
- `NOT_WORKING_SET`: completed warmup/non-working set; RIR may still be available.
- `NO_POSITIVE_LOAD`: missing, zero or negative load; RIR may still be available. Zero is valid logging input but insufficient for this load-based estimate.
- `UNSUPPORTED_REPETITIONS`: missing, nonpositive or more than 10 repetitions; RIR may still be available. Higher-repetition sets remain valid training records.
- `AVAILABLE`: completed WORKING set, positive recorded load and 1–10 repetitions.

The 10-repetition cutoff and warmup exclusion are explicit conservative product policies, not universal scientific thresholds. AVAILABLE means the arithmetic passed these gates; it does not certify the method for an exercise/person/device.

### Effort warnings

For available e1RM:

- RPE below 10: `SUBMAXIMAL_RPE_NOT_ADJUSTED`. The set was reported below maximal effort; the formula still uses only completed reps and kg.
- RPE absent: `RPE_NOT_RECORDED`. No maximal effort or zero RIR is assumed.
- One rep: `SINGLE_REPETITION_NOT_VERIFIED_MAXIMUM`, in addition to any effort warning.

For 100 kg × 6 at RPE 8.5 and the same set at RPE 10, the repetition-based value is 120.00 kg in both cases; the RIR/warnings differ. This deliberate behavior must remain visible. Empty warnings for a multi-rep RPE-10 set still do not turn the prediction into a measured maximum.

### Applicability limits

The calculation should be labelled **“Toistomäärään perustuva e1RM-arvio (ei RPE-korjattu)”**, never simply “maksimi” or “ennätys”. Prediction depends on the exercise, technique, range of motion, equipment and effort. Machine/cable stack markings are not universally comparable forces; dumbbell loads may be entered per hand and are not doubled automatically. Keep comparisons within the same movement, equipment and logging convention. There is no validated exercise-specific eligibility flag or guarantee for all ten catalogue exercises, especially isolation/machine movements. A client may choose not to display an estimate for a particular use case; arithmetic availability is not a training recommendation.

Max-repetition prediction research uses defined protocols and populations; higher repetitions and lower effort can weaken the interpretation of a load/repetition result. An upper-body validation study reported meaningful individual variability and warned against interchanging prediction methods with direct testing. This supports labelling estimates distinctly; it does not validate this app's policy for every exercise. [Sigvaldsen et al. (2023), primary study](https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0288649).

## Separate research note: RPE-aware estimation (not implemented)

Reviewed on 2026-10-22. The application currently has **no RPE-adjusted maximum**. Do not silently substitute the following research candidate into the repetition-based result.

A candidate heuristic would first approximate repetitions-to-failure:

`effectiveRepetitions = completedRepetitions + (10 − RPE)`

and substitute those effective repetitions into an Epley-style equation. For 100 kg × 6 at RPE 8, this would use 8 effective repetitions instead of 6. This is an explicitly identified inference combining our two conventions, not a validated prediction method established by the papers reviewed here. Fractional effective repetitions and the single-rep case introduce additional policy choices.

The resistance-specific scale and its practical application are documented by [Zourdos et al. (2016)](https://pubmed.ncbi.nlm.nih.gov/26049792/) and [Helms et al. (2016)](https://pubmed.ncbi.nlm.nih.gov/27531969/). Their support for subjective RIR-based effort monitoring does not establish that adding RIR to an Epley repetition count produces an accurate maximum. A different RPE-prediction study used Borg 6–20, so its conclusions must not be transferred directly to our 6–10 resistance-specific scale. [Eston and Evans (2009), primary study](https://www.jssm.org/volume08/iss4/cap/jssm-08-567.pdf).

Before any RPE-aware implementation: verify the selected model's primary derivation, supported effort/repetition ranges and exercise/population applicability; compare against independently observed maxima under an appropriate validation protocol; quantify uncertainty and bias; define single-rep, missing-RPE and cutoff behavior; and expose a separate method label rather than rewriting this historical repetition-based meaning. This research does not ask Roope to perform maximal tests or prescribe training. No percentage chart, third-party table or RPE-adjusted code was imported.

## Verification and interpretation

Pure unit tests cover every supported RPE step, invalid RPE, missing effort, single/ten/eleven-rep boundaries, exact decimals/final rounding and explicit exclusions. HTTP/PostgreSQL tests verify the result on POST, PUT, GET set, GET logged exercise and nested workout reads, including changes after editing/completing a set.

These tests prove implementation matches the documented arithmetic/policy. They do not establish physiological validity, clinical suitability or measured performance. Workout history/charts and record aggregation remain separate Milestone 6 work.
