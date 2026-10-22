package com.punttiurpo.repository;

import com.punttiurpo.entity.Workout;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {
    org.springframework.data.domain.Page<Workout> findByOwnerKey(String ownerKey, org.springframework.data.domain.Pageable page);
    java.util.Optional<Workout> findByIdAndOwnerKey(long id, String ownerKey);
}
