package iuh.fit.se.analyticsservice.repository;

import iuh.fit.se.analyticsservice.entity.LoginMethodAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoginMethodAnalyticsRepository extends JpaRepository<LoginMethodAnalytics, UUID> {

    Optional<LoginMethodAnalytics> findByDateAndLoginMethod(LocalDate date, String loginMethod);

    List<LoginMethodAnalytics> findByDate(LocalDate date);

    List<LoginMethodAnalytics> findByDateBetweenOrderByDateDescSuccessfulAttemptsDesc(LocalDate startDate, LocalDate endDate);

    @Query("SELECT l FROM LoginMethodAnalytics l WHERE l.date = :date ORDER BY l.successfulAttempts DESC")
    List<LoginMethodAnalytics> findTopMethodsByDate(@Param("date") LocalDate date);

    @Query("SELECT l.loginMethod, SUM(l.successfulAttempts) as total FROM LoginMethodAnalytics l WHERE l.date BETWEEN :startDate AND :endDate GROUP BY l.loginMethod ORDER BY total DESC")
    List<Object[]> sumSuccessfulAttemptsByMethod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT AVG(l.successRate) FROM LoginMethodAnalytics l WHERE l.date BETWEEN :startDate AND :endDate AND l.loginMethod = :loginMethod")
    Double avgSuccessRateByMethod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("loginMethod") String loginMethod);
}
