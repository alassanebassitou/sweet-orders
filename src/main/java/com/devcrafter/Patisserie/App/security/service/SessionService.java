package com.devcrafter.Patisserie.App.security.service;

import com.devcrafter.Patisserie.App.Exceptions.SessionCreationFailedException;
import com.devcrafter.Patisserie.App.models.SessionUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

import static com.devcrafter.Patisserie.App.utils.AppConstants.PREFIX;
import static com.devcrafter.Patisserie.App.utils.AppConstants.USER_SESSION;

@Service
@Slf4j
//@RequiredArgsConstructor
public class SessionService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.session.ttl-hours:8}")
    private long ttlHours;

    public SessionService(
            @Qualifier("sessionRedisTemplate")
            RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public String createSession(SessionUser user) {
        try {
            String sessionId = UUID.randomUUID().toString();
            String userJson  = objectMapper.writeValueAsString(user);

            log.info("User json: {}", userJson);

            redisTemplate.opsForValue().set(
                    PREFIX + sessionId,
                    userJson,
                    Duration.ofHours(ttlHours)
            );
            redisTemplate.opsForValue().set(
                     USER_SESSION + user.getGoogleSub(),
                    sessionId,
                    Duration.ofHours(ttlHours)
            );
            log.info("Session created for: {} and role: {}", user.getEmail(), user.getRole());
            return sessionId;

        } catch (Exception e) {
            log.error("Failed to create session: {}", e.getMessage());
            throw new SessionCreationFailedException("Session creation failed");
        }
    }

    public SessionUser getSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return null;

        String userJson = redisTemplate.opsForValue()
                .get(PREFIX + sessionId);

        if (userJson == null) {
            log.warn("No session found for: {}", sessionId);
            return null;
        }

        try {
            return objectMapper.readValue(userJson, SessionUser.class);
        } catch (Exception e) {
            log.error("Failed to deserialize session: {}", e.getMessage());
            return null;
        }
    }

    public void refreshSession(String sessionId, SessionUser user) {
        redisTemplate.expire(
                PREFIX + sessionId, Duration.ofHours(ttlHours));
        redisTemplate.expire(
                USER_SESSION + user.getGoogleSub(),
                Duration.ofHours(ttlHours));
    }

    public void deleteSession(String sessionId) {
        SessionUser user = getSession(sessionId);
        if (user != null) {
            redisTemplate.delete(USER_SESSION + user.getGoogleSub());
        }
        redisTemplate.delete(PREFIX + sessionId);
    }

    public void deleteAllSessionsForUser(String googleSub) {
        String sessionId = redisTemplate.opsForValue()
                .get(USER_SESSION + googleSub);
        if (sessionId != null) {
            redisTemplate.delete(PREFIX + sessionId);
            redisTemplate.delete(USER_SESSION + googleSub);
        }
    }
}
