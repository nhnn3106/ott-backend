package iuh.fit.ottbackend.repository;

import iuh.fit.ottbackend.entity.User;
import iuh.fit.ottbackend.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    // Tìm session theo user
    List<UserSession> findByUser(User user);

    // Tìm session theo session token
    Optional<UserSession> findBySessionToken(String sessionToken);

    // Tìm session theo device ID và user
    Optional<UserSession> findByDeviceIdAndUser(String deviceId, User user);

    // Tìm active sessions của user
    @Query("SELECT s FROM UserSession s WHERE s.user = :user AND s.expiresAt > :now")
    List<UserSession> findActiveSessionsByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    // Tìm sessions đã hết hạn
    @Query("SELECT s FROM UserSession s WHERE s.expiresAt < :now")
    List<UserSession> findExpiredSessions(@Param("now") LocalDateTime now);

    // Xóa sessions cũ
    void deleteByExpiresAtBefore(LocalDateTime dateTime);

    // Đếm số active sessions của user
    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.user = :user AND s.expiresAt > :now")
    long countActiveSessionsByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    // Tìm sessions theo device type
    List<UserSession> findByUserAndDeviceType(User user, String deviceType);

    Optional<UserSession> findByDeviceIdAndUserAndIsActive(String deviceId, User user, Boolean isActive);
}
