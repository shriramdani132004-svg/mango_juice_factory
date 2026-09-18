package com.smartfactory;

import com.smartfactory.entity.User;
import com.smartfactory.enums.Role;
import com.smartfactory.repository.UserRepository;
import com.smartfactory.security.JwtUtil;
import com.smartfactory.security.UserPrincipal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SmartFactoryApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeAll
    static void setupOnce(@Autowired UserRepository userRepository,
                          @Autowired PasswordEncoder passwordEncoder) {
        createIfMissing(userRepository, passwordEncoder, "admin", "admin123", "System Administrator", Role.ADMIN, "admin@smartfactory.dev");
        createIfMissing(userRepository, passwordEncoder, "prod_worker", "password123", "Production Worker 1", Role.PRODUCTION_WORKER, "prod@smartfactory.dev");
        createIfMissing(userRepository, passwordEncoder, "maint_worker", "password123", "Maintenance Worker 1", Role.MAINTENANCE_WORKER, "maint@smartfactory.dev");
        createIfMissing(userRepository, passwordEncoder, "quality_worker", "password123", "Quality Worker 1", Role.QUALITY_WORKER, "quality@smartfactory.dev");
    }

    private static void createIfMissing(UserRepository repo, PasswordEncoder encoder,
                                         String username, String password, String displayName,
                                         Role role, String email) {
        if (!repo.existsByUsername(username)) {
            User user = new User(username, encoder.encode(password), displayName, role);
            user.setEmail(email);
            repo.save(user);
        }
    }

    @Test
    void contextLoads() {
        assertNotNull(userRepository);
        assertNotNull(jwtUtil);
        assertNotNull(passwordEncoder);
    }

    @Test
    void passwordIsHashed() {
        User admin = userRepository.findByUsername("admin").orElseThrow();
        assertTrue(admin.getPasswordHash().startsWith("$2a$"));
        assertNotEquals("admin123", admin.getPasswordHash());
        assertTrue(passwordEncoder.matches("admin123", admin.getPasswordHash()));
    }

    @Test
    void jwtContainsCorrectClaims() {
        User admin = userRepository.findByUsername("admin").orElseThrow();
        UserPrincipal principal = new UserPrincipal(admin);

        String token = jwtUtil.generateToken(principal);

        assertEquals("admin", jwtUtil.extractUsername(token));
        String role = jwtUtil.extractClaim(token, claims -> claims.get("role", String.class));
        assertEquals("ADMIN", role);
        Long userId = jwtUtil.extractClaim(token, claims -> claims.get("userId", Long.class));
        assertEquals(admin.getId(), userId);
    }

    @Test
    void jwtValidationWorks() {
        User admin = userRepository.findByUsername("admin").orElseThrow();
        UserPrincipal principal = new UserPrincipal(admin);

        String token = jwtUtil.generateToken(principal);
        assertTrue(jwtUtil.validateToken(token, principal));
    }

    @Test
    void jwtRejectionForInvalidToken() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }

    @Test
    void jwtRejectionForEmptyToken() {
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    void productionWorkerHasCorrectRole() {
        User pw = userRepository.findByUsername("prod_worker").orElseThrow();
        assertEquals(Role.PRODUCTION_WORKER, pw.getRole());
        assertEquals("ROLE_PRODUCTION_WORKER", pw.getRole().getAuthority());
    }

    @Test
    void maintenanceWorkerHasCorrectRole() {
        User mw = userRepository.findByUsername("maint_worker").orElseThrow();
        assertEquals(Role.MAINTENANCE_WORKER, mw.getRole());
        assertEquals("ROLE_MAINTENANCE_WORKER", mw.getRole().getAuthority());
    }

    @Test
    void qualityWorkerHasCorrectRole() {
        User qw = userRepository.findByUsername("quality_worker").orElseThrow();
        assertEquals(Role.QUALITY_WORKER, qw.getRole());
        assertEquals("ROLE_QUALITY_WORKER", qw.getRole().getAuthority());
    }

    @Test
    void allFourRolesExist() {
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        List<User> prodWorkers = userRepository.findByRole(Role.PRODUCTION_WORKER);
        List<User> maintWorkers = userRepository.findByRole(Role.MAINTENANCE_WORKER);
        List<User> qualityWorkers = userRepository.findByRole(Role.QUALITY_WORKER);

        assertFalse(admins.isEmpty(), "Should have at least one admin");
        assertFalse(prodWorkers.isEmpty(), "Should have at least one production worker");
        assertFalse(maintWorkers.isEmpty(), "Should have at least one maintenance worker");
        assertFalse(qualityWorkers.isEmpty(), "Should have at least one quality worker");
    }

    @Test
    void userRepositoryWorks() {
        assertTrue(userRepository.count() >= 4);
    }
}
