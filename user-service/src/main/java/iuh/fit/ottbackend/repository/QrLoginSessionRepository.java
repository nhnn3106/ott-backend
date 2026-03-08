package iuh.fit.ottbackend.repository;

import feign.Param;
import iuh.fit.ottbackend.entity.QrCode;
import iuh.fit.ottbackend.entity.QrLoginSession;
import iuh.fit.ottbackend.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QrLoginSessionRepository extends JpaRepository<QrLoginSession, String> {
    Optional<QrLoginSession> findByQrCode(QrCode qrCode);

    @Modifying
    @Query("UPDATE QrLoginSession q SET q.session = null WHERE q.session = :session")
    void nullifySessionReference(@Param("session") UserSession session);
}
