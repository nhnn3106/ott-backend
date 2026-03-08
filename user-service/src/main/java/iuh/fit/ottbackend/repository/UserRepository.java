package iuh.fit.ottbackend.repository;

import feign.Param;
import iuh.fit.ottbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.googleId = :googleId ORDER BY u.createdAt DESC")
    List<User> findAllByGoogleId(@Param("googleId") String googleId);

    boolean existsByGoogleId(String googleId);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByPhoneAndDeletedAtIsNull(String phone);

    Optional<User> findByPhoneOrEmailAndDeletedAtIsNull(String phone, String email);


    // ✅ CHECK tồn tại - CHỈ đếm user ACTIVE
    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByPhoneAndDeletedAtIsNull(String phone);

    // Cho admin query
    List<User> findByDeletedAtIsNotNull(); // Danh sách user đã xóa

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NOT NULL AND u.deletedAt >= :since")
    List<User> findDeletedUsersSince(@Param("since") LocalDateTime since);

    Optional<User> findByGoogleIdAndDeletedAtIsNull(String googleId);

    @Query("SELECT u FROM User u WHERE u.googleId = :googleId AND u.deletedAt IS NULL")
    List<User> findAllByGoogleIdAndDeletedAtIsNull(@Param("googleId") String googleId);

    /**
     * Tìm account đã xóa > 30 ngày với phone cụ thể
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.deletedAt IS NOT NULL 
        AND u.deletedAt < :cutoffDate
        AND u.phone LIKE CONCAT(:phone, '_deleted_%')
        """)
    Optional<User> findExpiredDeletedUserByPhone(
            @Param("phone") String phone,
            @Param("cutoffDate") LocalDateTime cutoffDate
    );

    /**
     * Tìm account đã xóa > 30 ngày với email cụ thể
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.deletedAt IS NOT NULL 
        AND u.deletedAt < :cutoffDate
        AND u.email LIKE CONCAT(:email, '_deleted_%')
        """)
    Optional<User> findExpiredDeletedUserByEmail(
            @Param("email") String email,
            @Param("cutoffDate") LocalDateTime cutoffDate
    );

    /**
     * Tìm account đã xóa > 30 ngày với phone HOẶC email
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.deletedAt IS NOT NULL 
        AND u.deletedAt < :cutoffDate
        AND (u.phone LIKE CONCAT(:phone, '_deleted_%') 
             OR u.email LIKE CONCAT(:email, '_deleted_%'))
        """)
    List<User> findExpiredDeletedUsersByPhoneOrEmail(
            @Param("phone") String phone,
            @Param("email") String email,
            @Param("cutoffDate") LocalDateTime cutoffDate
    );


    /**
     * Tìm TẤT CẢ user (kể cả đã xóa) theo phone
     * Dùng cho thống kê
     */
    List<User> findAllByPhoneStartingWith(String phonePrefix);

    /**
     * Tìm TẤT CẢ user (kể cả đã xóa) theo email
     * Dùng cho thống kê
     */
    List<User> findAllByEmailStartingWith(String emailPrefix);

    /**
     * Đếm số lần user này đã tạo và xóa account
     * (Thống kê hành vi)
     */
    @Query("""
        SELECT COUNT(u) FROM User u 
        WHERE u.deletedAt IS NOT NULL 
        AND (u.phone LIKE CONCAT(:phone, '_deleted_%') 
             OR u.email LIKE CONCAT(:email, '_deleted_%'))
        """)
    long countDeletedAccountsByPhoneOrEmail(
            @Param("phone") String phone,
            @Param("email") String email
    );

    /**
     * Tìm user bằng email có suffix (deleted)
     * VD: email = "user@example.com_deleted_%"
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.deletedAt IS NOT NULL 
        AND u.email LIKE :emailPattern
        ORDER BY u.deletedAt DESC
        LIMIT 1
        """)
    User findByEmailWithSuffix(@Param("emailPattern") String emailPattern);

    /**
     * Tìm user bằng googleId có suffix (deleted)
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.deletedAt IS NOT NULL 
        AND u.googleId LIKE :googleIdPattern
        ORDER BY u.deletedAt DESC
        LIMIT 1
        """)
    User findByGoogleIdWithSuffix(@Param("googleIdPattern") String googleIdPattern);

    /**
     * Tìm tất cả user có googleId bắt đầu bằng... (kể cả deleted)
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.googleId LIKE :googleIdPrefix
        ORDER BY u.createdAt DESC
        """)
    List<User> findAllByGoogleIdStartingWith(@Param("googleIdPrefix") String googleIdPrefix);

    boolean existsByGoogleIdAndDeletedAtIsNull(String googleId);

    boolean existsByEmailAndDeletedAtIsNullAndIdNot(String email, String userId);
}