package com.punttiurpo.dto;

import java.util.List;

public record WorkoutPage(List<WorkoutSummaryResponse> items, int page, int size,
                          long totalElements, int totalPages) {
}
