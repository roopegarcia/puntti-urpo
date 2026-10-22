package com.punttiurpo.dto;

import java.math.BigDecimal;
import com.punttiurpo.entity.WorkoutSetType;

public record WorkoutSetResponse(Long id, int setOrder, BigDecimal weightKg, Integer repetitions,
                                 BigDecimal rpe, WorkoutSetType setType, boolean completed) {
}
