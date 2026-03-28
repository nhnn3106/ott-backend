package iuh.fit.se.analyticsservice.repository;

import iuh.fit.se.analyticsservice.entity.GeographicAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeographicAnalyticsRepository extends JpaRepository<GeographicAnalytics, UUID> {

    Optional<GeographicAnalytics> findByDateAndCountryAndCity(LocalDate date, String country, String city);

    List<GeographicAnalytics> findByDate(LocalDate date);

    List<GeographicAnalytics> findByDateBetweenOrderByDateDescTotalSessionsDesc(LocalDate startDate, LocalDate endDate);

    @Query("SELECT g FROM GeographicAnalytics g WHERE g.date = :date ORDER BY g.totalSessions DESC")
    List<GeographicAnalytics> findTopLocationsByDate(@Param("date") LocalDate date);

    @Query("SELECT g.country, SUM(g.uniqueUsers) as total FROM GeographicAnalytics g WHERE g.date BETWEEN :startDate AND :endDate GROUP BY g.country ORDER BY total DESC")
    List<Object[]> sumUsersByCountry(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
