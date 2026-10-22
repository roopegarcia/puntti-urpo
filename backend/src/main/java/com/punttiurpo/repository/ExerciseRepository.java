package com.punttiurpo.repository;

import com.punttiurpo.entity.Exercise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    @Query("""
            select e from Exercise e
            where (e.custom = false or e.ownerKey = :ownerKey)
            and locate(lower(:query), lower(e.name)) > 0
            order by lower(e.name), e.id
            """)
    Page<Exercise> search(@Param("query") String query, @Param("ownerKey") String ownerKey, Pageable pageable);

    @Query("select e from Exercise e where e.id = :id and (e.custom = false or e.ownerKey = :ownerKey)")
    java.util.Optional<Exercise> findVisible(@Param("id") long id, @Param("ownerKey") String ownerKey);
}
