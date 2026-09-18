package com.smartfactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfactory.entity.*;
import com.smartfactory.enums.*;
import com.smartfactory.repository.*;
import com.smartfactory.security.JwtUtil;
import com.smartfactory.security.UserPrincipal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Persistence Audit: Database/Backend Consistency Tests")
class PersistenceAuditTests {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MaterialRepository materialRepository;
    @Autowired ProductRepository productRepository;
    @Autowired RecipeRepository recipeRepository;
    @Autowired InventoryRepository inventoryRepository;
    @Autowired MachineRepository machineRepository;
    @Autowired MachineTypeRepository machineTypeRepository;
    @Autowired ProductionLineRepository productionLineRepository;
    @Autowired FactoryRepository factoryRepository;
    @Autowired UserRepository userRepository;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired InventoryTransactionRepository inventoryTransactionRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtUtil jwtUtil;

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

    @Test @Order(1)
    @DisplayName("1. MaterialType RAW loads correctly")
    void materialTypeRawLoadsCorrectly() {
        Material m = new Material("Audit Raw", "AUD-T1", MaterialType.RAW, "kg");
        Material saved = materialRepository.save(m);
        materialRepository.flush();
        Material retrieved = materialRepository.findById(saved.getId()).orElseThrow();
        assertEquals(MaterialType.RAW, retrieved.getMaterialType());
        assertEquals("RAW", retrieved.getMaterialType().name());
    }

    @Test @Order(2)
    @DisplayName("2. MaterialType PACKAGING loads correctly")
    void materialTypePackagingLoadsCorrectly() {
        Material m = new Material("Audit Pkg", "AUD-T2", MaterialType.PACKAGING, "pcs");
        Material saved = materialRepository.save(m);
        materialRepository.flush();
        Material retrieved = materialRepository.findById(saved.getId()).orElseThrow();
        assertEquals(MaterialType.PACKAGING, retrieved.getMaterialType());
    }

    @Test @Order(3)
    @DisplayName("3. MaterialType FINISHED loads correctly")
    void materialTypeFinishedLoadsCorrectly() {
        Material m = new Material("Audit Finished", "AUD-T3", MaterialType.FINISHED, "btl");
        Material saved = materialRepository.save(m);
        materialRepository.flush();
        Material retrieved = materialRepository.findById(saved.getId()).orElseThrow();
        assertEquals(MaterialType.FINISHED, retrieved.getMaterialType());
    }

    @Test @Order(4)
    @DisplayName("4. MaterialCategory remains separate from MaterialType")
    void materialCategorySeparate() {
        Material m = new Material("Audit Sep", "AUD-T4", MaterialType.RAW, "kg");
        m.setCategory("RAW_MATERIAL");
        Material saved = materialRepository.save(m);
        materialRepository.flush();
        Material retrieved = materialRepository.findById(saved.getId()).orElseThrow();
        assertEquals(MaterialType.RAW, retrieved.getMaterialType());
        assertEquals("RAW_MATERIAL", retrieved.getCategory());
        assertNotEquals(retrieved.getMaterialType().name(), retrieved.getCategory());
    }

    @Test @Order(5)
    @DisplayName("5. MaterialType enum has exactly the DB-compatible values")
    void materialTypeEnumExact() {
        assertEquals(3, MaterialType.values().length);
        assertEquals("RAW", MaterialType.RAW.name());
        assertEquals("PACKAGING", MaterialType.PACKAGING.name());
        assertEquals("FINISHED", MaterialType.FINISHED.name());
    }

    @Test @Order(6)
    @DisplayName("6. Invalid database values do not silently convert")
    void invalidValuesNotSilentlyConverted() {
        assertThrows(IllegalArgumentException.class, () -> MaterialType.valueOf("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> Role.valueOf("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> InventoryStatus.valueOf("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> InventoryTransactionType.valueOf("INVALID"));
    }

    @Test @Order(7)
    @DisplayName("7. InventoryStatus enum consistency")
    void inventoryStatusEnumConsistent() {
        assertEquals(5, InventoryStatus.values().length);
        assertEquals("IN_STOCK", InventoryStatus.IN_STOCK.name());
        assertEquals("LOW_STOCK", InventoryStatus.LOW_STOCK.name());
        assertEquals("OUT_OF_STOCK", InventoryStatus.OUT_OF_STOCK.name());
        assertEquals("RESERVED", InventoryStatus.RESERVED.name());
        assertEquals("QUARANTINED", InventoryStatus.QUARANTINED.name());
    }

    @Test @Order(8)
    @DisplayName("8. InventoryTransactionType enum consistency")
    void inventoryTransactionTypeEnumConsistent() {
        assertEquals(9, InventoryTransactionType.values().length);
    }

    @Test @Order(9)
    @DisplayName("9. QualityCheckStatus and QualityResult enum consistency")
    void qualityEnumsConsistent() {
        assertTrue(QualityCheckStatus.values().length > 0);
        assertTrue(QualityResult.values().length > 0);
        assertNotNull(QualityResult.PASS);
        assertNotNull(QualityResult.FAIL);
        assertNotNull(QualityResult.CONDITIONAL);
    }

    @Test @Order(10)
    @DisplayName("10. Audit log JSONB persistence works correctly")
    void auditJsonbPersistence() {
        AuditLog log = new AuditLog();
        log.setAction("AUDIT_JSONB_TEST");
        log.setEntityType("TEST");
        log.setNewValues("{\"key\":\"value\",\"nested\":{\"a\":1}}");
        AuditLog saved = auditLogRepository.save(log);
        auditLogRepository.flush();
        AuditLog retrieved = auditLogRepository.findById(saved.getId()).orElseThrow();
        assertNotNull(retrieved.getNewValues());
        assertTrue(retrieved.getNewValues().contains("key"));
        assertNull(retrieved.getOldValues());
    }

    @Test @Order(11)
    @DisplayName("11. Audit log null JSONB persists correctly")
    void auditJsonbNullPersistence() {
        AuditLog log = new AuditLog();
        log.setAction("AUDIT_NULL_JSONB");
        log.setEntityType("TEST");
        log.setOldValues(null);
        log.setNewValues(null);
        AuditLog saved = auditLogRepository.save(log);
        auditLogRepository.flush();
        AuditLog retrieved = auditLogRepository.findById(saved.getId()).orElseThrow();
        assertNull(retrieved.getOldValues());
        assertNull(retrieved.getNewValues());
    }

    @Test @Order(12)
    @DisplayName("12. User/role persistence is valid")
    void userRolePersistence() {
        assertTrue(userRepository.count() >= 1);
        User admin = userRepository.findByUsername("admin").orElseThrow();
        assertNotNull(admin.getUsername());
        assertTrue(admin.getPasswordHash().startsWith("$2a$"));
        assertEquals(Role.ADMIN, admin.getRole());
    }

    @Test @Order(13)
    @DisplayName("13. Production order default status is DRAFT")
    void productionOrderDefaultStatus() {
        Material m = new Material("Draft Test Mat", "AUD-DRAFT-MAT", MaterialType.RAW, "kg");
        materialRepository.save(m);
        Product p = new Product("Draft Test Product", "AUD-DRAFT-PROD", "test", 500);
        productRepository.save(p);
        Recipe r = new Recipe(p, "Draft Test Recipe", 1);
        r.setBatchSize(new BigDecimal("1000.00"));
        r.setOutputQuantity(new BigDecimal("1000.00"));
        r.setOutputUnit("btl");
        r.setYieldPercentage(new BigDecimal("100.00"));
        recipeRepository.save(r);
        ProductionOrder order = new ProductionOrder(
            "TEST-ORD-DRAFT", p, r, m,
            new BigDecimal("5000.00"), 500, 8);
        assertEquals("DRAFT", order.getStatus());
    }

    @Test @Order(14)
    @Transactional
    @DisplayName("14. Recipe materials have valid material references")
    void recipeMaterialsLoad() {
        Material m = new Material("Recipe Test Mat", "AUD-RECIPE-MAT", MaterialType.RAW, "kg");
        materialRepository.save(m);
        Product p = new Product("Recipe Test Product", "AUD-RECIPE-PROD", "test", 500);
        productRepository.save(p);
        Recipe r = new Recipe(p, "Recipe Test Recipe", 1);
        r.setBatchSize(new BigDecimal("1000.00"));
        r.setOutputQuantity(new BigDecimal("1000.00"));
        r.setOutputUnit("btl");
        r.setYieldPercentage(new BigDecimal("100.00"));
        recipeRepository.save(r);
        com.smartfactory.entity.RecipeMaterial rm = new com.smartfactory.entity.RecipeMaterial(r, m, new BigDecimal("500.00"), "kg");
        rm.setMaterialCategory("RAW");
        r.addRecipeMaterial(rm);
        recipeRepository.save(r);

        Recipe fullRecipe = recipeRepository.findByIdWithMaterials(r.getId()).orElseThrow();
        assertFalse(fullRecipe.getRecipeMaterials().isEmpty());
        for (com.smartfactory.entity.RecipeMaterial rmItem : fullRecipe.getRecipeMaterials()) {
            assertNotNull(rmItem.getMaterial());
            assertNotNull(rmItem.getMaterial().getMaterialType());
            assertNotNull(rmItem.getMaterialCategory());
        }
    }

    @Test @Order(15)
    @Transactional
    @DisplayName("15. Inventory loads with valid material references")
    void inventoryLoads() {
        Material m = new Material("Inv Test Mat", "AUD-INV-MAT", MaterialType.RAW, "kg");
        materialRepository.save(m);
        Inventory inv = new Inventory(m, new BigDecimal("100.00"), "kg", "WH-AUDIT");
        inventoryRepository.save(inv);
        inventoryRepository.flush();

        Inventory retrieved = inventoryRepository.findByMaterialId(m.getId()).orElseThrow();
        assertNotNull(retrieved.getMaterial());
        assertNotNull(retrieved.getMaterial().getMaterialType());
        assertNotNull(retrieved.getStatus());
    }

    @Test @Order(16)
    @DisplayName("16. Machines load with valid relationships")
    void machineRecordsLoad() {
        Factory factory = new Factory();
        factory.setName("Audit Machine Factory");
        factory.setLocation("Test Campus");
        factoryRepository.save(factory);

        ProductionLine line = new ProductionLine();
        line.setFactory(factory);
        line.setLineCode("AUD-LINE-M1");
        line.setName("Audit Machine Line");
        line.setCapacity(50);
        productionLineRepository.save(line);

        MachineType mt = new MachineType();
        mt.setName("Audit Receiver");
        mt.setCapability("RECEIVING");
        mt.setDefaultCapacity(new BigDecimal("1000.00"));
        mt.setDefaultProductionRate(new BigDecimal("500.00"));
        machineTypeRepository.save(mt);

        Machine machine = new Machine();
        machine.setMachineCode("aud-mach-1");
        machine.setName("Audit Machine 1");
        machine.setType(mt);
        machine.setLine(line);
        machine.setCapability("RECEIVING");
        machine.setCapacity(new BigDecimal("1000.00"));
        machine.setProductionRate(new BigDecimal("500.00"));
        machine.setStatus("IDLE");
        machine.setHealthScore(100);
        machine.setIsActive(true);
        machineRepository.save(machine);
        machineRepository.flush();

        assertTrue(machineRepository.count() > 0);
        Machine retrieved = machineRepository.findByMachineCode("aud-mach-1").orElseThrow();
        assertNotNull(retrieved.getType());
        assertNotNull(retrieved.getLine());
        assertNotNull(retrieved.getCapability());
    }

    @Test @Order(17)
    @DisplayName("17. findByMaterialType query works for all types")
    void findByMaterialTypeWorks() {
        Material raw = new Material("Query Test RAW", "AUD-Q1", MaterialType.RAW, "kg");
        Material pkg = new Material("Query Test PKG", "AUD-Q2", MaterialType.PACKAGING, "pcs");
        Material fin = new Material("Query Test FIN", "AUD-Q3", MaterialType.FINISHED, "btl");
        materialRepository.saveAll(java.util.List.of(raw, pkg, fin));
        materialRepository.flush();
        assertFalse(materialRepository.findByMaterialType(MaterialType.RAW).isEmpty());
        assertFalse(materialRepository.findByMaterialType(MaterialType.PACKAGING).isEmpty());
        assertFalse(materialRepository.findByMaterialType(MaterialType.FINISHED).isEmpty());
    }

    @Test @Order(18)
    @DisplayName("18. Material type endpoint returns correct type via API")
    void materialTypeEndpointWorks() throws Exception {
        Material m = new Material("Endpoint Test", "AUD-EP-MAT", MaterialType.RAW, "kg");
        materialRepository.save(m);
        mockMvc.perform(get("/api/materials/type/RAW")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
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
}
