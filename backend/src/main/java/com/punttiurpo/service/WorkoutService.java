package com.punttiurpo.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.punttiurpo.dto.AddWorkoutExerciseRequest;
import com.punttiurpo.dto.WorkoutExerciseRequest;
import com.punttiurpo.dto.WorkoutExerciseResponse;
import com.punttiurpo.dto.WorkoutPage;
import com.punttiurpo.dto.WorkoutRequest;
import com.punttiurpo.dto.WorkoutResponse;
import com.punttiurpo.dto.WorkoutSetRequest;
import com.punttiurpo.dto.WorkoutSetResponse;
import com.punttiurpo.entity.Exercise;
import com.punttiurpo.entity.Workout;
import com.punttiurpo.entity.WorkoutExercise;
import com.punttiurpo.entity.WorkoutSet;
import com.punttiurpo.exception.ExerciseNotFoundException;
import com.punttiurpo.exception.InvalidWorkoutException;
import com.punttiurpo.exception.WorkoutNotFoundException;
import com.punttiurpo.mapper.WorkoutMapper;
import com.punttiurpo.repository.ExerciseRepository;
import com.punttiurpo.repository.WorkoutExerciseRepository;
import com.punttiurpo.repository.WorkoutRepository;
import com.punttiurpo.repository.WorkoutSetRepository;
import com.punttiurpo.security.CurrentAccount;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WorkoutService {
    private final CurrentAccount account;
    private final WorkoutRepository workouts;
    private final WorkoutExerciseRepository exercises;
    private final WorkoutSetRepository sets;
    private final ExerciseRepository catalogue;

    public WorkoutService(WorkoutRepository workouts, WorkoutExerciseRepository exercises,
                          WorkoutSetRepository sets, ExerciseRepository catalogue, CurrentAccount account) {
        this.account = account;
        this.workouts = workouts;
        this.exercises = exercises;
        this.sets = sets;
        this.catalogue = catalogue;
    }

    public WorkoutPage list(int page, int size) {
        var result = workouts.findByOwnerKey(account.key(), PageRequest.of(page, size, Sort.by("date", "id").descending()));
        return new WorkoutPage(result.map(WorkoutMapper::summary).getContent(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public WorkoutResponse get(long id) {
        return response(findWorkout(id));
    }

    public WorkoutExerciseResponse getExercise(long id) {
        return response(findExercise(id));
    }

    public WorkoutSetResponse getSet(long id) {
        return WorkoutMapper.set(findSet(id));
    }

    @Transactional
    public WorkoutResponse create(WorkoutRequest request) {
        validateTimes(request);
        Workout workout = new Workout(request.date(), request.title(), request.notes(),
                request.startedAt(), request.endedAt());
        workout.assignOwner(account.key());
        return WorkoutMapper.workout(workouts.saveAndFlush(workout), List.of());
    }

    @Transactional
    public WorkoutResponse update(long id, WorkoutRequest request) {
        validateTimes(request);
        Workout workout = findWorkout(id);
        workout.update(request.date(), request.title(), request.notes(), request.startedAt(), request.endedAt());
        workouts.flush();
        return response(workout);
    }

    @Transactional
    public void delete(long id) {
        workouts.delete(findWorkout(id));
        workouts.flush();
    }

    @Transactional
    public WorkoutExerciseResponse addExercise(long workoutId, AddWorkoutExerciseRequest request) {
        Workout workout = findWorkout(workoutId);
        Exercise exercise = catalogue.findVisible(request.exerciseId(), account.key()).orElseThrow(ExerciseNotFoundException::new);
        int order = nextOrder(exercises.maxOrder(workoutId));
        var added = exercises.saveAndFlush(new WorkoutExercise(workout, exercise, order, request.notes()));
        return WorkoutMapper.exercise(added, List.of());
    }

    @Transactional
    public WorkoutExerciseResponse updateExercise(long id, WorkoutExerciseRequest request) {
        WorkoutExercise exercise = findExercise(id);
        exercise.update(request.displayOrder(), request.notes());
        exercises.flush();
        return response(exercise);
    }

    @Transactional
    public void deleteExercise(long id) {
        exercises.delete(findExercise(id));
        exercises.flush();
    }

    @Transactional
    public WorkoutSetResponse addSet(long workoutExerciseId, WorkoutSetRequest request) {
        validateSet(request);
        WorkoutExercise exercise = findExercise(workoutExerciseId);
        int order = request.setOrder() == null ? nextOrder(sets.maxOrder(workoutExerciseId)) : request.setOrder();
        var added = sets.saveAndFlush(new WorkoutSet(exercise, order, request.weightKg(), request.repetitions(),
                request.rpe(), request.setType(), request.completed()));
        return WorkoutMapper.set(added);
    }

    @Transactional
    public WorkoutSetResponse updateSet(long id, WorkoutSetRequest request) {
        validateSet(request);
        WorkoutSet set = findSet(id);
        int order = request.setOrder() == null ? set.getSetOrder() : request.setOrder();
        set.update(order, request.weightKg(), request.repetitions(), request.rpe(), request.setType(), request.completed());
        sets.flush();
        return WorkoutMapper.set(set);
    }

    @Transactional
    public void deleteSet(long id) {
        sets.delete(findSet(id));
        sets.flush();
    }

    private WorkoutResponse response(Workout workout) {
        // Load all sets once, rather than issuing a query for each exercise.
        Map<Long, List<WorkoutSetResponse>> grouped = sets.findByWorkoutExerciseWorkoutIdOrderBySetOrderAscIdAsc(workout.getId())
                .stream().collect(Collectors.groupingBy(set -> set.getWorkoutExercise().getId(),
                        Collectors.mapping(WorkoutMapper::set, Collectors.toList())));
        var items = exercises.findByWorkoutIdOrderByDisplayOrderAscIdAsc(workout.getId()).stream()
                .map(exercise -> WorkoutMapper.exercise(exercise, grouped.getOrDefault(exercise.getId(), List.of())))
                .toList();
        return WorkoutMapper.workout(workout, items);
    }

    private WorkoutExerciseResponse response(WorkoutExercise exercise) {
        var items = sets.findByWorkoutExerciseIdOrderBySetOrderAscIdAsc(exercise.getId())
                .stream().map(WorkoutMapper::set).toList();
        return WorkoutMapper.exercise(exercise, items);
    }

    private Workout findWorkout(long id) {
        return workouts.findByIdAndOwnerKey(id, account.key()).orElseThrow(WorkoutNotFoundException::new);
    }

    private WorkoutExercise findExercise(long id) {
        return exercises.findByIdAndWorkoutOwnerKey(id, account.key()).orElseThrow(WorkoutNotFoundException::new);
    }

    private WorkoutSet findSet(long id) {
        return sets.findByIdAndWorkoutExerciseWorkoutOwnerKey(id, account.key()).orElseThrow(WorkoutNotFoundException::new);
    }

    private static int nextOrder(int current) {
        if (current >= 1_000_000) {
            throw new InvalidWorkoutException("Järjestysnumeroiden enimmäismäärä on saavutettu.");
        }
        return current + 1;
    }

    private static void validateTimes(WorkoutRequest request) {
        if (request.endedAt() != null && (request.startedAt() == null
                || request.endedAt().isBefore(request.startedAt()))) {
            throw new InvalidWorkoutException("Lopetusajan on oltava aloitusajan jälkeen tai sama.");
        }
    }

    private static void validateSet(WorkoutSetRequest request) {
        if (request.completed() && (request.weightKg() == null || request.repetitions() == null
                || request.repetitions() < 1)) {
            throw new InvalidWorkoutException("Valmis sarja tarvitsee painon ja vähintään yhden toiston.");
        }
        if (request.rpe() != null && request.rpe().remainder(new BigDecimal("0.5")).signum() != 0) {
            throw new InvalidWorkoutException("RPE annetaan puolen pisteen välein välillä 6–10.");
        }
    }
}
