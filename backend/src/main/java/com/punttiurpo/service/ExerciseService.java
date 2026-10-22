package com.punttiurpo.service;

import com.punttiurpo.dto.ExercisePage;
import com.punttiurpo.dto.ExerciseRequest;
import com.punttiurpo.dto.ExerciseResponse;
import com.punttiurpo.entity.Exercise;
import com.punttiurpo.exception.BuiltInExerciseException;
import com.punttiurpo.exception.ExerciseNotFoundException;
import com.punttiurpo.mapper.ExerciseMapper;
import com.punttiurpo.repository.ExerciseRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExerciseService {
    private final ExerciseRepository repository;

    public ExerciseService(ExerciseRepository repository) {
        this.repository = repository;
    }

    public ExercisePage list(String query, int page, int size) {
        var result = repository.search(query.strip(), PageRequest.of(page, size));
        return new ExercisePage(result.map(ExerciseMapper::toResponse).getContent(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public ExerciseResponse get(long id) {
        return ExerciseMapper.toResponse(find(id));
    }

    @Transactional
    public ExerciseResponse create(ExerciseRequest request) {
        Exercise exercise = new Exercise(request.name(), request.category(),
                request.primaryMuscleGroup(), request.equipment(), request.notes());
        return ExerciseMapper.toResponse(repository.saveAndFlush(exercise));
    }

    @Transactional
    public ExerciseResponse update(long id, ExerciseRequest request) {
        Exercise exercise = find(id);
        if (!exercise.isCustom()) {
            throw new BuiltInExerciseException();
        }
        exercise.update(request.name(), request.category(), request.primaryMuscleGroup(),
                request.equipment(), request.notes());
        // The entity is managed: Hibernate detects changes. Flush surfaces constraint conflicts here.
        repository.flush();
        return ExerciseMapper.toResponse(exercise);
    }

    private Exercise find(long id) {
        return repository.findById(id).orElseThrow(ExerciseNotFoundException::new);
    }
}
