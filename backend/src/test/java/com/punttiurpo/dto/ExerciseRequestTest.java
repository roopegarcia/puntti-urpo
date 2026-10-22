package com.punttiurpo.dto;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExerciseRequestTest {
    @Test
    void normalizesInputBeforeValidation() {
        var request = new ExerciseRequest("  Oma liike  ", "  ", " Selkä ", null, " ");
        assertThat(request.name()).isEqualTo("Oma liike");
        assertThat(request.category()).isNull();
        assertThat(request.primaryMuscleGroup()).isEqualTo("Selkä");
        assertThat(request.notes()).isNull();
    }

    @Test
    void validatesBlankAndOversizedFieldsAtBoundaries() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new ExerciseRequest(" ", null, null, null, null)))
                    .hasSize(1);
            assertThat(validator.validate(new ExerciseRequest("a".repeat(100), "a".repeat(50),
                    "a".repeat(50), "a".repeat(50), "a".repeat(2000)))).isEmpty();
            assertThat(validator.validate(new ExerciseRequest("a".repeat(101), "a".repeat(51),
                    "a".repeat(51), "a".repeat(51), "a".repeat(2001)))).hasSize(5);
        }
    }
}
