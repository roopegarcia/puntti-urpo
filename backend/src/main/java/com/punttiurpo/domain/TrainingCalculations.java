package com.punttiurpo.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.punttiurpo.domain.SetEstimates.E1rmStatus;
import com.punttiurpo.domain.SetEstimates.Warning;

/** Pure arithmetic: no entities, repositories, Spring or database access. */
public final class TrainingCalculations {
    private static final BigDecimal SIX = new BigDecimal("6");
    private static final BigDecimal TEN = new BigDecimal("10");
    private static final BigDecimal HALF = new BigDecimal("0.5");
    private static final BigDecimal THIRTY = new BigDecimal("30");
    private static final int MAX_ESTIMATED_REPETITIONS = 10;

    private TrainingCalculations() {
    }

    /** Conventional approximation on the supported resistance-training RPE scale. */
    public static Optional<BigDecimal> rpeToEstimatedRir(BigDecimal rpe) {
        if (rpe == null) {
            return Optional.empty();
        }
        if (rpe.compareTo(SIX) < 0 || rpe.compareTo(TEN) > 0 || rpe.remainder(HALF).signum() != 0) {
            throw new IllegalArgumentException("RPE must be 6–10 in steps of 0.5.");
        }
        return Optional.of(TEN.subtract(rpe).setScale(1));
    }

    public static SetEstimates calculate(BigDecimal weightKg, Integer repetitions,
                                         BigDecimal rpe, boolean completed, boolean workingSet) {
        if (!completed) {
            return unavailable(null, E1rmStatus.NOT_COMPLETED);
        }
        BigDecimal rir = rpeToEstimatedRir(rpe).orElse(null);
        if (!workingSet) {
            return unavailable(rir, E1rmStatus.NOT_WORKING_SET);
        }
        if (weightKg == null || weightKg.signum() <= 0) {
            return unavailable(rir, E1rmStatus.NO_POSITIVE_LOAD);
        }
        if (repetitions == null || repetitions < 1 || repetitions > MAX_ESTIMATED_REPETITIONS) {
            return unavailable(rir, E1rmStatus.UNSUPPORTED_REPETITIONS);
        }

        List<Warning> warnings = new ArrayList<>();
        if (rpe == null) {
            warnings.add(Warning.RPE_NOT_RECORDED);
        } else if (rpe.compareTo(TEN) < 0) {
            warnings.add(Warning.SUBMAXIMAL_RPE_NOT_ADJUSTED);
        }

        BigDecimal estimate;
        if (repetitions == 1) {
            // Product convention: a single returns its recorded load, not a verified maximum.
            estimate = weightKg.setScale(2, RoundingMode.HALF_UP);
            warnings.add(Warning.SINGLE_REPETITION_NOT_VERIFIED_MAXIMUM);
        } else {
            // Algebraic Epley form avoids rounding reps / 30 before multiplying the load.
            estimate = weightKg.multiply(THIRTY.add(BigDecimal.valueOf(repetitions)))
                    .divide(THIRTY, 2, RoundingMode.HALF_UP);
        }
        return new SetEstimates(rir, estimate, E1rmStatus.AVAILABLE, warnings);
    }

    private static SetEstimates unavailable(BigDecimal rir, E1rmStatus reason) {
        return new SetEstimates(rir, null, reason, List.of());
    }
}
