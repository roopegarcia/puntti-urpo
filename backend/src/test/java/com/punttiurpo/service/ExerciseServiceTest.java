package com.punttiurpo.service;

import java.util.Optional;

import com.punttiurpo.dto.ExerciseRequest;
import com.punttiurpo.entity.Exercise;
import com.punttiurpo.exception.BuiltInExerciseException;
import com.punttiurpo.exception.ExerciseNotFoundException;
import com.punttiurpo.repository.ExerciseRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExerciseServiceTest {
    private final ExerciseRepository repository = mock(ExerciseRepository.class);
    private final ExerciseService service = new ExerciseService(repository);

    @Test
    void missingExerciseHasDomainSpecificFailure() {
        when(repository.findById(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(42L)).isInstanceOf(ExerciseNotFoundException.class);
    }

    @Test
    void builtInExerciseCannotBeModified() {
        Exercise builtIn = mock(Exercise.class);
        when(builtIn.isCustom()).thenReturn(false);
        when(repository.findById(1L)).thenReturn(Optional.of(builtIn));

        assertThatThrownBy(() -> service.update(1L, new ExerciseRequest("Muutos", null, null, null, null)))
                .isInstanceOf(BuiltInExerciseException.class);
        verify(builtIn, never()).update(any(), any(), any(), any(), any());
        verify(repository, never()).flush();
    }

    @Test
    void customUpdateReplacesOptionalFieldsAndKeepsCustomOrigin() {
        Exercise custom = new Exercise("Vanha", "Luokka", "Rinta", "Laite", "Muistiinpano");
        when(repository.findById(9L)).thenReturn(Optional.of(custom));

        var response = service.update(9L, new ExerciseRequest("  Uusi  ", null, null, null, null));

        assertThat(response.name()).isEqualTo("Uusi");
        assertThat(response.notes()).isNull();
        assertThat(response.category()).isNull();
        assertThat(response.custom()).isTrue();
        verify(repository).flush();
    }
}
