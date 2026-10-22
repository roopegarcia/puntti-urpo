package com.punttiurpo.domain;

import java.math.BigDecimal;
import java.util.stream.Stream;
import com.punttiurpo.domain.SetEstimates.E1rmStatus;
import com.punttiurpo.domain.SetEstimates.Warning;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TrainingCalculationsTest {
    @ParameterizedTest
    @CsvSource({"6,4", "6.5,3.5", "7,3", "7.5,2.5", "8,2", "8.5,1.5", "9,1", "9.5,0.5", "10,0"})
    void mapsEverySupportedRpeStepToApproximateRir(String rpe, String expected) {
        assertThat(TrainingCalculations.rpeToEstimatedRir(new BigDecimal(rpe)).orElseThrow())
                .isEqualByComparingTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "5.5", "6.25", "8.2", "10.5"})
    void rejectsInvalidRpeAtThePureCalculationBoundary(String rpe) {
        assertThatIllegalArgumentException().isThrownBy(() ->
                TrainingCalculations.rpeToEstimatedRir(new BigDecimal(rpe)));
    }

    @Test
    void absentRpeRemainsUnknownRatherThanZeroRir() {
        assertThat(TrainingCalculations.rpeToEstimatedRir(null)).isEmpty();
        var result = calculate("100", 5, null, true, true);
        assertThat(result.estimatedRir()).isNull();
        assertThat(result.warnings()).containsExactly(Warning.RPE_NOT_RECORDED);
    }

    @ParameterizedTest
    @CsvSource({"100,6,120.00", "100,10,133.33", "82.25,1,82.25", "100.01,5,116.68",
            "0.15,3,0.17", "9999.99,10,13333.32", "0.01,10,0.01", "100.01,2,106.68"})
    void calculatesWithoutIntermediateRoundingAndRoundsFinalKgHalfUp(String weight, int reps, String expected) {
        var result = calculate(weight, reps, "10", true, true);
        assertThat(result.e1rmStatus()).isEqualTo(E1rmStatus.AVAILABLE);
        assertThat(result.repetitionBasedE1rmKg()).isEqualByComparingTo(expected);
        assertThat(result.repetitionBasedE1rmKg().scale()).isEqualTo(2);
    }

    @Test
    void submaximalRpeNeverChangesTheEpleyValueButIsExplicitlyFlagged() {
        var submaximal = calculate("100", 6, "8.5", true, true);
        var maximalReportedEffort = calculate("100", 6, "10", true, true);
        assertThat(submaximal.repetitionBasedE1rmKg()).isEqualTo(maximalReportedEffort.repetitionBasedE1rmKg());
        assertThat(submaximal.estimatedRir()).isEqualByComparingTo("1.5");
        assertThat(submaximal.warnings()).containsExactly(Warning.SUBMAXIMAL_RPE_NOT_ADJUSTED);
        assertThat(maximalReportedEffort.warnings()).isEmpty();
    }

    @Test
    void singleReturnsRecordedLoadWithWarningEvenWhenEffortWasSubmaximal() {
        var result = calculate("100", 1, "8", true, true);
        assertThat(result.repetitionBasedE1rmKg()).isEqualByComparingTo("100.00");
        assertThat(result.warnings()).containsExactly(Warning.SUBMAXIMAL_RPE_NOT_ADJUSTED,
                Warning.SINGLE_REPETITION_NOT_VERIFIED_MAXIMUM);
    }

    @ParameterizedTest
    @MethodSource("unsuitableSets")
    void unsuitableSetsReturnAnExplicitReasonInsteadOfAZeroOrFabricatedMaximum(
            String weight, Integer reps, boolean completed, boolean working, E1rmStatus expected) {
        var result = calculate(weight, reps, "8", completed, working);
        assertThat(result.repetitionBasedE1rmKg()).isNull();
        assertThat(result.e1rmStatus()).isEqualTo(expected);
    }

    static Stream<Arguments> unsuitableSets() {
        return Stream.of(
                Arguments.of("100", 6, false, true, E1rmStatus.NOT_COMPLETED),
                Arguments.of("100", 6, true, false, E1rmStatus.NOT_WORKING_SET),
                Arguments.of(null, 6, true, true, E1rmStatus.NO_POSITIVE_LOAD),
                Arguments.of("0", 6, true, true, E1rmStatus.NO_POSITIVE_LOAD),
                Arguments.of("-1", 6, true, true, E1rmStatus.NO_POSITIVE_LOAD),
                Arguments.of("100", null, true, true, E1rmStatus.UNSUPPORTED_REPETITIONS),
                Arguments.of("100", 0, true, true, E1rmStatus.UNSUPPORTED_REPETITIONS),
                Arguments.of("100", 11, true, true, E1rmStatus.UNSUPPORTED_REPETITIONS),
                Arguments.of("100", Integer.MAX_VALUE, true, true, E1rmStatus.UNSUPPORTED_REPETITIONS));
    }

    @Test
    void completedSetRirDoesNotDependOnWhetherE1rmIsAvailable() {
        assertThat(calculate("100", 12, "9.5", true, true).estimatedRir()).isEqualByComparingTo("0.5");
        assertThat(calculate("100", 5, "9", true, false).estimatedRir()).isEqualByComparingTo("1.0");
        assertThat(calculate("0", 5, "8", true, true).estimatedRir()).isEqualByComparingTo("2.0");
    }

    @Test
    void draftRpeIsNotInterpretedAsCompletedPerformance() {
        var result = calculate("100", 6, "8", false, true);
        assertThat(result.estimatedRir()).isNull();
        assertThat(result.warnings()).isEmpty();
    }

    private static SetEstimates calculate(String weight, Integer reps, String rpe, boolean completed, boolean working) {
        return TrainingCalculations.calculate(weight == null ? null : new BigDecimal(weight), reps,
                rpe == null ? null : new BigDecimal(rpe), completed, working);
    }
}
