package iuh.fit.se.analyticservice.repository;

<<<<<<< Updated upstream
=======
import java.time.LocalDateTime;
import java.util.List;
>>>>>>> Stashed changes
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import iuh.fit.se.analyticservice.entity.ContentViolationLog;

public interface ContentViolationLogRepository extends JpaRepository<ContentViolationLog, UUID> {

    boolean existsByViolationId(String violationId);
<<<<<<< Updated upstream
=======

    List<ContentViolationLog> findTop10ByOrderByDetectedAtDesc();

    long countByDetectedAtGreaterThanEqualAndDetectedAtLessThan(LocalDateTime from, LocalDateTime to);
>>>>>>> Stashed changes
}
