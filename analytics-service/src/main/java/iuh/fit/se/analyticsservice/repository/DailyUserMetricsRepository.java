package iuh.fit.se.analyticsservice.repository;

import iuh.fit.se.analyticsservice.entity.DailyUserMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyUserMetricsRepository extends JpaRepository<DailyUserMetrics, UUID> {

    Optional<DailyUserMetrics> findByDate(LocalDate date);

    List<DailyUserMetrics> findByDateBetweenOrderByDateDesc(LocalDate startDate, LocalDate endDate);

    @Query("SELECT d FROM DailyUserMetrics d WHERE d.date >= :startDate ORDER BY d.date DESC")
    List<DailyUserMetrics> findRecentMetrics(@Param("startDate") LocalDate startDate);

    @Query("SELECT SUM(d.newRegistrations) FROM DailyUserMetrics d WHERE d.date BETWEEN :startDate AND :endDate")
    Long sumNewRegistrationsByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT AVG(d.dailyActiveUsers) FROM DailyUserMetrics d WHERE d.date BETWEEN :startDate AND :endDate")
    Double avgDailyActiveUsersByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
