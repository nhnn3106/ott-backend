package iuh.fit.se.analyticsservice.repository;

import iuh.fit.se.analyticsservice.entity.MonthlyUserMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonthlyUserMetricsRepository extends JpaRepository<MonthlyUserMetrics, UUID> {

    Optional<MonthlyUserMetrics> findByYearAndMonth(int year, int month);

    @Query("SELECT m FROM MonthlyUserMetrics m WHERE m.year = :year ORDER BY m.month ASC")
    List<MonthlyUserMetrics> findByYearOrderByMonth(@Param("year") int year);

    @Query("SELECT m FROM MonthlyUserMetrics m WHERE (m.year > :startYear) OR (m.year = :startYear AND m.month >= :startMonth) AND (m.year < :endYear) OR (m.year = :endYear AND m.month <= :endMonth) ORDER BY m.year DESC, m.month DESC")
    List<MonthlyUserMetrics> findByDateRange(
        @Param("startYear") int startYear,
        @Param("startMonth") int startMonth,
        @Param("endYear") int endYear,
        @Param("endMonth") int endMonth
    );

    @Query("SELECT m FROM MonthlyUserMetrics m ORDER BY m.year DESC, m.month DESC")
    List<MonthlyUserMetrics> findAllOrderByDateDesc();
}
