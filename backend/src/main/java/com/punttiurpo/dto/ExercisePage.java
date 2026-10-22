package com.punttiurpo.dto;

import java.util.List;

public record ExercisePage(List<ExerciseResponse> items, int page, int size,
                           long totalElements, int totalPages) {
}
