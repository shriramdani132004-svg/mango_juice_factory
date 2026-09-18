package com.smartfactory;

import com.smartfactory.dto.LoginRequest;
import com.smartfactory.entity.Material;
import com.smartfactory.enums.MaterialCategory;
import com.smartfactory.enums.MaterialType;
import com.smartfactory.repository.MaterialRepository;
import com.smartfactory.repository.UserRepository;
import com.smartfactory.security.JwtUtil;
import com.smartfactory.security.UserPrincipal;
import com.smartfactory.entity.User;
import com.smartfactory.enums.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class MaterialTypeIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private static String adminToken;

    @BeforeAll
    static void setupToken(@Autowired UserRepository userRepository,
                           @Autowired PasswordEncoder passwordEncoder,
                           @Autowired JwtUtil jwtUtil) {
        createIfMissing(userRepository, passwordEncoder, "admin", "admin123",
            "System Administrator", Role.ADMIN, "admin@smartfactory.com");
        User admin = userRepository.findByUsername("admin").orElseThrow();
        adminToken = jwtUtil.generateToken(new UserPrincipal(admin));
    }

    @Test
    @Order(1)
    void materialWithRawTypeLoadsCorrectly() {
        Material m = new Material("Fresh Mangoes", "TEST-RAW", MaterialType.RAW, "kg");
        Material saved = materialRepository.save(m);
        materialRepository.flush();

        Optional<Material> retrieved = materialRepository.findById(saved.getId());
        assertTrue(retrieved.isPresent(), "Material with RAW type must be retrievable");
        assertEquals(MaterialType.RAW, retrieved.get().getMaterialType());
        assertEquals("RAW", retrieved.get().getMaterialType().name());
    }

    @Test
    @Order(2)
    void materialWithPackagingTypeLoadsCorrectly() {
        Material m = new Material("PET Bottles", "TEST-PKG", MaterialType.PACKAGING, "pcs");
        Material saved = materialRepository.save(m);
        materialRepository.flush();

        Optional<Material> retrieved = materialRepository.findById(saved.getId());
        assertTrue(retrieved.isPresent(), "Material with PACKAGING type must be retrievable");
        assertEquals(MaterialType.PACKAGING, retrieved.get().getMaterialType());
        assertEquals("PACKAGING", retrieved.get().getMaterialType().name());
    }

    @Test
    @Order(3)
    void materialWithFinishedTypeLoadsCorrectly() {
        Material m = new Material("Mango Beverage 500mL", "TEST-FIN", MaterialType.FINISHED, "btl");
        Material saved = materialRepository.save(m);
        materialRepository.flush();

        Optional<Material> retrieved = materialRepository.findById(saved.getId());
        assertTrue(retrieved.isPresent(), "Material with FINISHED type must be retrievable");
        assertEquals(MaterialType.FINISHED, retrieved.get().getMaterialType());
        assertEquals("FINISHED", retrieved.get().getMaterialType().name());
    }

    @Test
    @Order(4)
    void materialCategoryIsSeparateFromMaterialType() {
        Material m = new Material("Test Material", "TEST-SEP", MaterialType.RAW, "kg");
        m.setCategory("RAW_MATERIAL");
        Material saved = materialRepository.save(m);
        materialRepository.flush();

        Optional<Material> retrieved = materialRepository.findById(saved.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(MaterialType.RAW, retrieved.get().getMaterialType(),
            "materialType column must map to MaterialType enum");
        assertEquals("RAW_MATERIAL", retrieved.get().getCategory(),
            "category column must remain a separate string field");
        assertNotEquals(retrieved.get().getMaterialType().name(), retrieved.get().getCategory(),
            "MaterialType.RAW != category RAW_MATERIAL — they represent different concepts");
    }

    @Test
    @Order(5)
    void createProductionOrderMaterialType1Raw() throws Exception {
        Material m = new Material("Test Mangoes for Order", "TEST-MAT1", MaterialType.RAW, "kg");
        Material saved = materialRepository.save(m);
        materialRepository.flush();

        Optional<Material> retrieved = materialRepository.findById(saved.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(MaterialType.RAW, retrieved.get().getMaterialType(),
            "Material ID with type RAW must load without IllegalArgumentException");
    }

    @Test
    @Order(6)
    void enumNamesMatchDatabaseValues() {
        assertEquals("RAW", MaterialType.RAW.name());
        assertEquals("PACKAGING", MaterialType.PACKAGING.name());
        assertEquals("FINISHED", MaterialType.FINISHED.name());

        assertEquals(3, MaterialType.values().length, "MaterialType must have exactly 3 values");
    }

    @Test
    @Order(7)
    void findByMaterialTypeWorksForAllTypes() {
        Material m1 = new Material("Filter RAW", "FIND-RAW", MaterialType.RAW, "kg");
        Material m2 = new Material("Filter PKG", "FIND-PKG", MaterialType.PACKAGING, "pcs");
        Material m3 = new Material("Filter FIN", "FIND-FIN", MaterialType.FINISHED, "btl");
        materialRepository.saveAll(java.util.List.of(m1, m2, m3));
        materialRepository.flush();

        assertFalse(materialRepository.findByMaterialType(MaterialType.RAW).isEmpty(),
            "Must find materials with type RAW");
        assertFalse(materialRepository.findByMaterialType(MaterialType.PACKAGING).isEmpty(),
            "Must find materials with type PACKAGING");
        assertFalse(materialRepository.findByMaterialType(MaterialType.FINISHED).isEmpty(),
            "Must find materials with type FINISHED");
    }

    @Test
    @Order(8)
    void materialTypeEndpointReturnsCorrectType() throws Exception {
        Material m = new Material("Endpoint Test RAW", "EP-RAW", MaterialType.RAW, "kg");
        materialRepository.save(m);

        mockMvc.perform(get("/api/materials/type/RAW")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));
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
}
