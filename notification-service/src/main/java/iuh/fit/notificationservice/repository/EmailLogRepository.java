package iuh.fit.notificationservice.repository;

import iuh.fit.notificationservice.dto.EmailLog;
import iuh.fit.notificationservice.dto.enums.EmailStatus;
import iuh.fit.notificationservice.dto.enums.EmailType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, String> {
    List<EmailLog> findByUserIdOrderByCreatedAtDesc(String userId);
    List<EmailLog> findByStatus(EmailStatus status);
    boolean existsByEmailToAndEmailTypeAndStatus(String emailTo, EmailType emailType, EmailStatus status);
}
