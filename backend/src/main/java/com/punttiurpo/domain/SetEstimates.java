package com.punttiurpo.domain;

import java.math.BigDecimal;
import java.util.List;

/** Derived values, never persisted and never a measured personal record. */
public record SetEstimates(BigDecimal estimatedRir, BigDecimal repetitionBasedE1rmKg,
                           E1rmStatus e1rmStatus, List<Warning> warnings) {
    public SetEstimates {
        warnings = List.copyOf(warnings);
    }

    public enum E1rmStatus {
        AVAILABLE,
        NOT_COMPLETED,
        NOT_WORKING_SET,
        NO_POSITIVE_LOAD,
        UNSUPPORTED_REPETITIONS
    }

    public enum Warning {
        SUBMAXIMAL_RPE_NOT_ADJUSTED,
        RPE_NOT_RECORDED,
        SINGLE_REPETITION_NOT_VERIFIED_MAXIMUM
    }
}
