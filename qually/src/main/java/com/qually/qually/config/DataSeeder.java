package com.qually.qually.config;

import com.qually.qually.models.User;
import com.qually.qually.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds initial PINs for users who have not yet been assigned one.
 *
 * <p>Runs automatically on every application startup via {@link PostConstruct}.
 * The method is idempotent — it skips any user whose {@code pin_hash} is
 * already set, so it is safe to run repeatedly without overwriting PINs that
 * users have already changed.</p>
 *
 * <p><strong>Seeding rule:</strong> the temporary PIN is the user's first name,
 * lowercased and trimmed (e.g. full name "María García" → PIN "maría").
 * {@code force_pin_change} is set to {@code true} so the user is redirected
 * to the Change PIN page on first login.</p>
 *
 * <p><strong>Removal:</strong> delete this class entirely when Microsoft Auth
 * replaces the PIN login flow. No other file references it — Spring picks it
 * up automatically via {@code @Component}.</p>
 */
@Component
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    @Transactional
    public void seedPins() {
        List<User> usersWithoutPin = userRepository.findAll().stream()
                .filter(u -> u.getPinHash() == null)
                .toList();

        if (usersWithoutPin.isEmpty()) {
            log.debug("DataSeeder: all users already have a PIN — nothing to seed");
            return;
        }

        log.info("DataSeeder: seeding temporary PINs for {} user(s)", usersWithoutPin.size());

        for (User user : usersWithoutPin) {
            String firstName = extractFirstName(user.getFullName());
            user.setPinHash(passwordEncoder.encode(firstName));
            user.setForcePinChange(true);
            userRepository.save(user);
            log.info("DataSeeder: seeded PIN for user {} ({})",
                    user.getUserId(), user.getUserEmail());
        }

        log.info("DataSeeder: PIN seeding complete");
    }

    /**
     * Extracts the first word of the full name, lowercased and trimmed.
     * Examples:
     * <ul>
     *   <li>"John Smith"   → "john"</li>
     *   <li>"María García" → "maría"</li>
     *   <li>"alice"        → "alice"</li>
     * </ul>
     *
     * @param fullName The user's full name as stored in the database.
     * @return The first name segment, lowercased. Falls back to the full
     *         name lowercased if no space is found.
     */
    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "changeme";
        String trimmed = fullName.trim();
        int spaceIdx = trimmed.indexOf(' ');
        String firstName = (spaceIdx > 0)
                ? trimmed.substring(0, spaceIdx)
                : trimmed;
        return firstName.toLowerCase();
    }
}