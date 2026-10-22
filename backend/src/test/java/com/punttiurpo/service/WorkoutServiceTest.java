package com.punttiurpo.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import com.punttiurpo.dto.WorkoutRequest;
import com.punttiurpo.dto.WorkoutSetRequest;
import com.punttiurpo.exception.InvalidWorkoutException;
import com.punttiurpo.repository.ExerciseRepository;
import com.punttiurpo.repository.WorkoutExerciseRepository;
import com.punttiurpo.repository.WorkoutRepository;
import com.punttiurpo.repository.WorkoutSetRepository;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class WorkoutServiceTest {
    private final WorkoutRepository workouts = mock(WorkoutRepository.class);
    private final WorkoutExerciseRepository exercises = mock(WorkoutExerciseRepository.class);
    private final WorkoutSetRepository sets = mock(WorkoutSetRepository.class);
    private final ExerciseRepository catalogue = mock(ExerciseRepository.class);
    private final com.punttiurpo.security.CurrentAccount account = mock(com.punttiurpo.security.CurrentAccount.class);
    @org.junit.jupiter.api.BeforeEach
    void authenticatedOwner() { org.mockito.Mockito.when(account.key()).thenReturn("owner"); }
    private final WorkoutService service = new WorkoutService(workouts, exercises, sets, catalogue, account);

    @Test
    void rejectsEndWithoutStartBeforePersisting() {
        var request = new WorkoutRequest(LocalDate.of(2026, 10, 22), null, null, null,
                Instant.parse("2026-10-22T10:00:00Z"));
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(InvalidWorkoutException.class);
        verifyNoInteractions(workouts);
    }

    @Test
    void rejectsIncompletelySpecifiedCompletedSetBeforePersisting() {
        var request = new WorkoutSetRequest(null, null, null, null, null, true);
        assertThatThrownBy(() -> service.addSet(1L, request)).isInstanceOf(InvalidWorkoutException.class);
        verifyNoInteractions(sets, exercises);
    }

    @Test
    void rejectsRpeOutsideHalfPointStepsBeforePersisting() {
        var request = new WorkoutSetRequest(null, new BigDecimal("20"), 5, new BigDecimal("8.2"), null, true);
        assertThatThrownBy(() -> service.addSet(1L, request)).isInstanceOf(InvalidWorkoutException.class);
        verifyNoInteractions(sets, exercises);
    }
}
