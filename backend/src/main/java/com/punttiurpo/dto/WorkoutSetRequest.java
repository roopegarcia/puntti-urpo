package com.punttiurpo.dto;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;
import com.punttiurpo.entity.WorkoutSetType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record WorkoutSetRequest(
        @Min(value = 0, message = "Järjestysnumeron on oltava vähintään 0.")
        @Max(value = 1_000_000, message = "Järjestysnumero on liian suuri.") Integer setOrder,
        @DecimalMin(value = "0.0", message = "Paino ei voi olla negatiivinen.")
        @Digits(integer = 4, fraction = 2, message = "Painossa sallitaan enintään 4 kokonais- ja 2 desimaalinumeroa.") BigDecimal weightKg,
        @Min(value = 0, message = "Toistomäärä ei voi olla negatiivinen.")
        @Max(value = 1000, message = "Toistomäärä saa olla enintään 1000.") Integer repetitions,
        @Schema(description = "Vapaaehtoinen voimaharjoittelun RPE 6–10 puolen pisteen välein. "
                + "RPE 10 ≈ 0, RPE 9 ≈ 1 ja RPE 8 ≈ 2 toistoa varastossa. "
                + "Subjektiivinen arvio, ei mitattu suorituskyky.", example = "8.5")
        @DecimalMin(value = "6.0", message = "RPE on vähintään 6.")
        @DecimalMax(value = "10.0", message = "RPE on enintään 10.")
        @Digits(integer = 2, fraction = 1, message = "RPE annetaan enintään yhdellä desimaalilla.") BigDecimal rpe,
        WorkoutSetType setType, Boolean completed) {
    public WorkoutSetRequest {
        if (setType == null) {
            setType = WorkoutSetType.WORKING;
        }
        if (completed == null) {
            completed = false;
        }
    }
}
