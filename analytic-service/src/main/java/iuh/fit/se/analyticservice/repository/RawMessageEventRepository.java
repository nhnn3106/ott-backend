package iuh.fit.se.analyticservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import iuh.fit.se.analyticservice.entity.RawMessageEvent;

public interface RawMessageEventRepository extends JpaRepository<RawMessageEvent, String> {

    @Query("SELECT LOWER(m.messageType), COUNT(m) FROM RawMessageEvent m GROUP BY LOWER(m.messageType)")
    List<Object[]> countByMessageType();
}
