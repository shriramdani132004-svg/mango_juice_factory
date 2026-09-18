package com.smartfactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfactory.dto.LoginRequest;
import com.smartfactory.entity.User;
import com.smartfactory.enums.Role;
import com.smartfactory.repository.UserRepository;
import com.smartfactory.security.JwtUtil;
import com.smartfactory.security.UserPrincipal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private static String adminToken;
    private static String prodWorkerToken;
    private static String maintWorkerToken;
    private static String qualityWorkerToken;

    @BeforeAll
    static void setupOnce(@Autowired UserRepository userRepository,
                          @Autowired PasswordEncoder passwordEncoder,
                          @Autowired JwtUtil jwtUtil) {
        createIfMissing(userRepository, passwordEncoder, "admin", "admin123", "System Administrator", Role.ADMIN, "admin@smartfactory.dev");
        createIfMissing(userRepository, passwordEncoder, "prod_worker", "password123", "Production Worker 1", Role.PRODUCTION_WORKER, "prod@smartfactory.dev");
        createIfMissing(userRepository, passwordEncoder, "maint_worker", "password123", "Maintenance Worker 1", Role.MAINTENANCE_WORKER, "maint@smartfactory.dev");
        createIfMissing(userRepository, passwordEncoder, "quality_worker", "password123", "Quality Worker 1", Role.QUALITY_WORKER, "quality@smartfactory.dev");

        adminToken = generateToken(userRepository, jwtUtil, "admin");
        prodWorkerToken = generateToken(userRepository, jwtUtil, "prod_worker");
        maintWorkerToken = generateToken(userRepository, jwtUtil, "maint_worker");
        qualityWorkerToken = generateToken(userRepository, jwtUtil, "quality_worker");
    }

    private static void createIfMissing(UserRepository repo, PasswordEncoder encoder,
                                         String username, String password, String displayName,
                                         Role role, String email) {
        Optional<User> existing = repo.findByUsername(username);
        if (existing.isEmpty()) {
            User user = new User(username, encoder.encode(password), displayName, role);
            user.setEmail(email);
            repo.save(user);
        }
    }

    private static String generateToken(UserRepository repo, JwtUtil jwtUtil, String username) {
        User user = repo.findByUsername(username).orElseThrow();
        UserPrincipal principal = new UserPrincipal(user);
        return jwtUtil.generateToken(principal);
    }

    @Test
    @Order(1)
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @Order(2)
    void loginEndpointIsPublic() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.token").isNotEmpty())
            .andExpect(jsonPath("$.data.user.username").value("admin"))
            .andExpect(jsonPath("$.data.user.role").value("ADMIN"));
    }

    @Test
    @Order(3)
    void invalidLoginReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("admin", "wrongpassword"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @Order(4)
    void nonExistentUserLoginReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("nonexistent", "password"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    void protectedEndpointRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    void protectedEndpointRejectsInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer invalidtoken123"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    void authenticatedUserCanAccessMeEndpoint() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.username").value("admin"))
            .andExpect(jsonPath("$.data.role").value("ADMIN"))
            .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @Order(8)
    void passwordNeverReturnedInMeEndpoint() throws Exception {
        String response = mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertFalse(response.contains("password123"), "Password should not appear");
        assertFalse(response.contains("$2a$"), "Password hash should not appear");
    }

    @Test
    @Order(9)
    void adminCanAccessAdminOnlyUsersEndpoint() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(10)
    void productionWorkerCannotAccessAdminUsersEndpoint() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + prodWorkerToken))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(11)
    void maintenanceWorkerCannotAccessAdminUsersEndpoint() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + maintWorkerToken))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(12)
    void qualityWorkerCannotAccessAdminUsersEndpoint() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + qualityWorkerToken))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(13)
    void adminCanCreateUser() throws Exception {
        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"new_worker\",\"password\":\"pass123\",\"displayName\":\"New Worker\",\"email\":\"new@smartfactory.dev\",\"role\":\"PRODUCTION_WORKER\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.username").value("new_worker"));
    }

    @Test
    @Order(14)
    void productionWorkerCannotCreateUser() throws Exception {
        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + prodWorkerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"new_worker2\",\"password\":\"pass123\",\"displayName\":\"New Worker\",\"role\":\"PRODUCTION_WORKER\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(15)
    void productionWorkerCanAccessMaterials() throws Exception {
        mockMvc.perform(get("/api/materials")
                .header("Authorization", "Bearer " + prodWorkerToken))
            .andExpect(status().isOk());
    }

    @Test
    @Order(16)
    void maintenanceWorkerCanAccessInventory() throws Exception {
        mockMvc.perform(get("/api/inventory")
                .header("Authorization", "Bearer " + maintWorkerToken))
            .andExpect(status().isOk());
    }

    @Test
    @Order(17)
    void qualityWorkerCanAccessProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                .header("Authorization", "Bearer " + qualityWorkerToken))
            .andExpect(status().isOk());
    }

    @Test
    @Order(19)
    void allWorkersCanAccessRecipes() throws Exception {
        mockMvc.perform(get("/api/recipes")
                .header("Authorization", "Bearer " + prodWorkerToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/recipes")
                .header("Authorization", "Bearer " + maintWorkerToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/recipes")
                .header("Authorization", "Bearer " + qualityWorkerToken))
            .andExpect(status().isOk());
    }

    @Test
    @Order(20)
    void malformedBearerTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer "))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(21)
    void userDtoExcludesPasswordFields() throws Exception {
        String response = mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertFalse(response.contains("password"), "Response should not contain password field");
        assertFalse(response.contains("passwordHash"), "Response should not contain passwordHash field");
    }

    @Test
    @Order(22)
    void loginReturnsAllExpectedFields() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"))))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertTrue(response.contains("token"));
        assertTrue(response.contains("Bearer"));
        assertTrue(response.contains("username"));
        assertTrue(response.contains("displayName"));
        assertTrue(response.contains("role"));
        assertTrue(response.contains("active"));
        assertFalse(response.contains("password"));
    }

    @Test
    @Order(23)
    void allWorkerRolesCanLogin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("prod_worker", "password123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.role").value("PRODUCTION_WORKER"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("maint_worker", "password123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.role").value("MAINTENANCE_WORKER"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("quality_worker", "password123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.role").value("QUALITY_WORKER"));
    }

    @Test
    @Order(24)
    void adminCanDeactivateAndActivateUser() throws Exception {
        User user = userRepository.findByUsername("prod_worker").orElseThrow();

        mockMvc.perform(post("/api/users/" + user.getId() + "/deactivate")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("prod_worker", "password123"))))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/users/" + user.getId() + "/activate")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("prod_worker", "password123"))))
            .andExpect(status().isOk());
    }
}
