package com.punttiurpo.dto;

import java.math.BigDecimal;
import com.punttiurpo.domain.SetEstimates;
import io.swagger.v3.oas.annotations.media.Schema;
import com.punttiurpo.entity.WorkoutSetType;

public record WorkoutSetResponse(Long id, int setOrder, BigDecimal weightKg, Integer repetitions,
                                 BigDecimal rpe, WorkoutSetType setType, boolean completed,
        @Schema(description = "Johdetut arviot: RIR on subjektiiviseen RPE-arvoon perustuva likiarvo. "
                + "Toistomäärään perustuva e1RM ei ole RPE-korjattu eikä mitattu maksimisuoritus. "
                + "Arvion saatavuus ei takaa soveltuvuutta tiettyyn liikkeeseen tai laitteeseen.")
        SetEstimates estimates) {
}
