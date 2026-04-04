package iuh.fit.se.analyticservice.service;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import iuh.fit.se.analyticservice.client.UserServiceClient;
import iuh.fit.se.analyticservice.dto.DailyPostCountResponse;
import iuh.fit.se.analyticservice.dto.MessageTypesResponse;
import iuh.fit.se.analyticservice.dto.OverviewResponse;
import iuh.fit.se.analyticservice.dto.RecentNewUserDTO;
import iuh.fit.se.analyticservice.dto.UserDetailDTO;
import iuh.fit.se.analyticservice.entity.RawUserEvent;
import iuh.fit.se.analyticservice.repository.RawMessageEventRepository;
import iuh.fit.se.analyticservice.repository.RawPostEventRepository;
import iuh.fit.se.analyticservice.repository.RawUserEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAnalyticsService {

    private final RawUserEventRepository rawUserEventRepository;
    private final RawMessageEventRepository rawMessageEventRepository;
    private final RawPostEventRepository rawPostEventRepository;
    private final UserServiceClient userServiceClient;

    public OverviewResponse getOverview() {
        long totalUsers = rawUserEventRepository.count();
        long totalMessages = rawMessageEventRepository.count();
        long totalPosts = rawPostEventRepository.count();
        return new OverviewResponse(totalUsers, totalMessages, totalPosts);
    }

    public List<RecentNewUserDTO> getRecentUsers() {
        List<RawUserEvent> recentEvents = rawUserEventRepository.findTop5ByOrderByTimestampDesc();
        List<RecentNewUserDTO> result = new ArrayList<>();

        try {
            for (RawUserEvent event : recentEvents) {
                UserDetailDTO user = userServiceClient.getUserById(event.getUserId());
                result.add(new RecentNewUserDTO(
                        event.getUserId(),
                        user != null ? user.getEmail() : null,
                        user != null ? user.getFullName() : null
                ));
            }
            return result;
        } catch (Exception ex) {
            log.warn("user-service unavailable, return empty recent users list", ex);
            return new ArrayList<>();
        }
    }

    public MessageTypesResponse getMessageTypes() {
        long text = 0;
        long image = 0;
        long voice = 0;

        List<Object[]> rows = rawMessageEventRepository.countByMessageType();
        for (Object[] row : rows) {
            String type = row[0] != null ? row[0].toString().toLowerCase(Locale.ROOT) : "";
            long count = ((Number) row[1]).longValue();

            switch (type) {
                case "text" -> text = count;
                case "image" -> image = count;
                case "voice" -> voice = count;
                default -> {
                    // ignore unknown type to keep API simple for demo
                }
            }
        }

        return new MessageTypesResponse(text, image, voice);
    }

    public List<DailyPostCountResponse> getPostDaily7Days() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate fromDate = today.minusDays(6);
        Instant from = fromDate.atStartOfDay().toInstant(ZoneOffset.UTC);

        Map<LocalDate, Long> countByDate = new HashMap<>();
        for (Object[] row : rawPostEventRepository.countPostsByDateFrom(from)) {
            LocalDate date;
            if (row[0] instanceof Date sqlDate) {
                date = sqlDate.toLocalDate();
            } else {
                date = LocalDate.parse(String.valueOf(row[0]));
            }
            long count = ((Number) row[1]).longValue();
            countByDate.put(date, count);
        }

        List<DailyPostCountResponse> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = fromDate.plusDays(i);
            result.add(new DailyPostCountResponse(date, countByDate.getOrDefault(date, 0L)));
        }
        return result;
    }
}
