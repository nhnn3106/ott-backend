package iuh.fit.se.analyticsservice.repository;

import iuh.fit.se.analyticsservice.entity.DeviceAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceAnalyticsRepository extends JpaRepository<DeviceAnalytics, UUID> {

    Optional<DeviceAnalytics> findByDateAndDeviceType(LocalDate date, String deviceType);

    List<DeviceAnalytics> findByDate(LocalDate date);

    List<DeviceAnalytics> findByDateBetweenOrderByDateDescDeviceTypeAsc(LocalDate startDate, LocalDate endDate);

    @Query("SELECT d FROM DeviceAnalytics d WHERE d.date = :date ORDER BY d.totalSessions DESC")
    List<DeviceAnalytics> findTopDevicesByDate(@Param("date") LocalDate date);

    @Query("SELECT d.deviceType, SUM(d.totalSessions) as total FROM DeviceAnalytics d WHERE d.date BETWEEN :startDate AND :endDate GROUP BY d.deviceType ORDER BY total DESC")
    List<Object[]> sumSessionsByDeviceType(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
