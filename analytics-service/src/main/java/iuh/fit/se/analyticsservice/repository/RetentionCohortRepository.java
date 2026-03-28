package iuh.fit.se.analyticsservice.repository;

import iuh.fit.se.analyticsservice.entity.RetentionCohort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RetentionCohortRepository extends JpaRepository<RetentionCohort, UUID> {

    Optional<RetentionCohort> findByCohortDate(LocalDate cohortDate);

    List<RetentionCohort> findByCohortDateBetweenOrderByCohortDateDesc(LocalDate startDate, LocalDate endDate);

    @Query("SELECT r FROM RetentionCohort r WHERE r.cohortDate >= :startDate ORDER BY r.cohortDate DESC")
    List<RetentionCohort> findRecentCohorts(@Param("startDate") LocalDate startDate);

    @Query("SELECT AVG(r.retention30Day) FROM RetentionCohort r WHERE r.cohortDate BETWEEN :startDate AND :endDate AND r.retention30Day IS NOT NULL")
    Double avgRetention30DayByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT AVG(r.retention7Day) FROM RetentionCohort r WHERE r.cohortDate BETWEEN :startDate AND :endDate AND r.retention7Day IS NOT NULL")
    Double avgRetention7DayByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
