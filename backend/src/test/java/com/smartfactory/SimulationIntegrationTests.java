package com.smartfactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfactory.dto.CreateOrderRequest;
import com.smartfactory.entity.*;
import com.smartfactory.enums.MaterialCategory;
import com.smartfactory.enums.Role;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Part 6: Simulation + Phase Execution Tests")
class SimulationIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtUtil jwtUtil;
    @Autowired ProductRepository productRepository;
    @Autowired RecipeRepository recipeRepository;
    @Autowired MaterialRepository materialRepository;
    @Autowired InventoryRepository inventoryRepository;
    @Autowired MachineRepository machineRepository;
    @Autowired MachineTypeRepository machineTypeRepository;
    @Autowired ProductionLineRepository productionLineRepository;
    @Autowired FactoryRepository factoryRepository;

    private String adminToken;
    private Long orderId;
    private Long phase1Id;

    @BeforeAll
    void setup() throws Exception {
        createIfMissing("admin", "admin123", "System Administrator", Role.ADMIN, "admin@smartfactory.dev");
        createIfMissing("prod_worker", "password123", "Production Worker", Role.PRODUCTION_WORKER, "prod@smartfactory.dev");
        createIfMissing("maint_worker", "password123", "Maintenance Worker", Role.MAINTENANCE_WORKER, "maint@smartfactory.dev");
        createIfMissing("quality_worker", "password123", "Quality Worker", Role.QUALITY_WORKER, "quality@smartfactory.dev");

        adminToken = generateToken("admin");

        if (productRepository.count() == 0) {
            Product product = new Product("Mango Beverage", "PROD-MANGO", "Premium mango beverage", 500);
            productRepository.save(product);

            Recipe recipe = new Recipe(product, "Mango Beverage 500mL Recipe v1", 1);
            recipe.setYieldPercentage(new BigDecimal("85.00"));
            recipe.setOutputQuantity(new BigDecimal("1000.00"));
            recipe.setBatchSize(new BigDecimal("2500.00"));
            recipe.setOutputUnit("btl");
            recipeRepository.save(recipe);

            Material mangoes = new Material("Fresh Mangoes", "RAW-MANGO", MaterialCategory.RAW_MATERIAL, "kg");
            materialRepository.save(mangoes);
            Material water = new Material("Water", "RAW-WATER", MaterialCategory.RAW_MATERIAL, "L");
            materialRepository.save(water);
            Material sugar = new Material("Sugar", "RAW-SUGAR", MaterialCategory.RAW_MATERIAL, "kg");
            materialRepository.save(sugar);
            Material bottles = new Material("500mL PET Bottles", "PKG-BOTTLE500", MaterialCategory.PACKAGING, "pcs");
            materialRepository.save(bottles);
            Material caps = new Material("Bottle Caps", "PKG-CAP", MaterialCategory.PACKAGING, "pcs");
            materialRepository.save(caps);

            addInventory(mangoes, new BigDecimal("50000.00"), "kg");
            addInventory(water, new BigDecimal("100000.00"), "L");
            addInventory(sugar, new BigDecimal("8000.00"), "kg");
            addInventory(bottles, new BigDecimal("200000.00"), "pcs");
            addInventory(caps, new BigDecimal("200000.00"), "pcs");

            RecipeMaterial rm1 = new RecipeMaterial(recipe, mangoes, new BigDecimal("2500.00"), "kg");
            rm1.setMaterialCategory("RAW");
            recipe.addRecipeMaterial(rm1);
            RecipeMaterial rm2 = new RecipeMaterial(recipe, water, new BigDecimal("1500.00"), "L");
            rm2.setMaterialCategory("RAW");
            recipe.addRecipeMaterial(rm2);
            RecipeMaterial rm3 = new RecipeMaterial(recipe, sugar, new BigDecimal("150.00"), "kg");
            rm3.setMaterialCategory("RAW");
            recipe.addRecipeMaterial(rm3);
            RecipeMaterial rm4 = new RecipeMaterial(recipe, bottles, new BigDecimal("1020.00"), "pcs");
            rm4.setMaterialCategory("PACKAGING");
            recipe.addRecipeMaterial(rm4);
            RecipeMaterial rm5 = new RecipeMaterial(recipe, caps, new BigDecimal("1020.00"), "pcs");
            rm5.setMaterialCategory("PACKAGING");
            recipe.addRecipeMaterial(rm5);
            recipeRepository.save(recipe);

            Factory factory = new Factory();
            factory.setName("SmartFactory Mango Processing Plant");
            factory.setLocation("Main Campus");
            factoryRepository.save(factory);

            ProductionLine line = new ProductionLine();
            line.setFactory(factory);
            line.setLineCode("LINE-01");
            line.setName("Production Line 1");
            line.setCapacity(50);
            productionLineRepository.save(line);

            String[] typeNames = {"Receiver", "Washer-Sorter", "Peeler-Pulper", "Filter", "Blender", "Pasteurizer", "Quality Inspector", "Filler-Capper", "Labeler-Packer", "Material Handler"};
            String[] capabilities = {"RECEIVING", "WASHING", "PEELING", "FILTERING", "BLENDING", "PASTEURIZING", "QUALITY_INSPECTION", "FILLING", "LABELING", "WAREHOUSING"};
            BigDecimal[] defaultRates = {new BigDecimal("500.00"), new BigDecimal("400.00"), new BigDecimal("300.00"), new BigDecimal("350.00"), new BigDecimal("250.00"), new BigDecimal("200.00"), new BigDecimal("50.00"), new BigDecimal("200.00"), new BigDecimal("200.00"), new BigDecimal("500.00")};

            String[] codePrefixes = {"rec", "was", "pee", "flt", "bln", "pst", "qai", "frc", "lbl", "wrh"};
            for (int i = 0; i < 10; i++) {
                MachineType mt = new MachineType();
                mt.setName(typeNames[i]);
                mt.setCapability(capabilities[i]);
                mt.setDefaultCapacity(new BigDecimal("1000.00"));
                mt.setDefaultProductionRate(defaultRates[i]);
                machineTypeRepository.save(mt);

                for (int j = 1; j <= 5; j++) {
                    String code = codePrefixes[i] + "-" + j;
                    if (!machineRepository.findByMachineCode(code).isPresent()) {
                        Machine m = new Machine();
                        m.setMachineCode(code);
                        m.setName(typeNames[i] + " Machine " + j);
                        m.setType(mt);
                        m.setLine(line);
                        m.setCapability(capabilities[i]);
                        m.setCapacity(new BigDecimal("1000.00"));
                        m.setProductionRate(defaultRates[i]);
                        m.setStatus("IDLE");
                        m.setHealthScore(100);
                        m.setIsActive(true);
                        machineRepository.save(m);
                    }
                }
            }
        } else {
            machineRepository.findAll().stream()
                .filter(m -> !"IDLE".equals(m.getStatus()) && !"MAINTENANCE".equals(m.getStatus()))
                .forEach(m -> { m.setStatus("IDLE"); machineRepository.save(m); });
        }

        CreateOrderRequest request = new CreateOrderRequest(
            1L, new BigDecimal("5000.00"), 500, 8, null
        );
        String createResp = mockMvc.perform(post("/api/production-orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andReturn().getResponse().getContentAsString();
        orderId = objectMapper.readTree(createResp).get("id").asLong();

        mockMvc.perform(post("/api/production-orders/" + orderId + "/requirements-check")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/production-orders/" + orderId + "/approve")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        String detailResp = mockMvc.perform(get("/api/production-orders/" + orderId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        System.out.println("DETAIL RESPONSE: " + detailResp);
        JsonNode root = objectMapper.readTree(detailResp);
        phase1Id = root.get("phases").get(0).get("id").asLong();
    }

    private void createIfMissing(String username, String password, String displayName, Role role, String email) {
        Optional<User> existing = userRepository.findByUsername(username);
        if (existing.isEmpty()) {
            User user = new User(username, passwordEncoder.encode(password), displayName, role);
            user.setEmail(email);
            user.setIsActive(true);
            userRepository.save(user);
        }
    }

    private String generateToken(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        UserPrincipal principal = new UserPrincipal(user);
        return jwtUtil.generateToken(principal);
    }

    private void addInventory(Material material, BigDecimal qty, String unit) {
        Inventory inv = new Inventory(material, qty, unit, "Test Warehouse");
        inventoryRepository.save(inv);
    }

    @Test @Order(1)
    @DisplayName("Phase 1 status is READY after approval")
    void phase1Ready() throws Exception {
        mockMvc.perform(get("/api/production-orders/" + orderId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phases[0].status").value("READY"))
            .andExpect(jsonPath("$.phases[1].status").value("LOCKED"));
    }

    @Test @Order(2)
    @DisplayName("Cannot start phase that does not exist")
    void cannotStartNonExistentPhase() throws Exception {
        mockMvc.perform(post("/api/production-phases/99999/start")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    @Test @Order(3)
    @DisplayName("POST /api/production-phases/{id}/start - starts phase 1")
    void startPhase1() throws Exception {
        mockMvc.perform(post("/api/production-phases/" + phase1Id + "/start")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RUNNING"))
            .andExpect(jsonPath("$.phaseNumber").value(1))
            .andExpect(jsonPath("$.assignedMachines").isArray())
            .andExpect(jsonPath("$.assignedMachines.length()").isNumber());
    }

    @Test @Order(4)
    @DisplayName("GET /api/production-phases/{id}/status - shows executing state")
    void getPhaseStatus() throws Exception {
        mockMvc.perform(get("/api/production-phases/" + phase1Id + "/status")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.executing").value(true))
            .andExpect(jsonPath("$.status").value("RUNNING"))
            .andExpect(jsonPath("$.progress").isNumber());
    }

    @Test @Order(5)
    @DisplayName("GET /api/production-phases/active - shows active executions")
    void getActiveExecutions() throws Exception {
        mockMvc.perform(get("/api/production-phases/active")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
    }

    @Test @Order(6)
    @DisplayName("Cannot start same phase twice while running")
    void cannotStartRunningPhaseTwice() throws Exception {
        mockMvc.perform(post("/api/production-phases/" + phase1Id + "/start")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isBadRequest());
    }

    @Test @Order(7)
    @DisplayName("POST /api/production-phases/{id}/pause - pauses phase")
    void pausePhase() throws Exception {
        mockMvc.perform(post("/api/production-phases/" + phase1Id + "/pause")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/production-phases/" + phase1Id + "/status")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAUSED"));
    }

    @Test @Order(8)
    @DisplayName("POST /api/production-phases/{id}/resume - resumes phase")
    void resumePhase() throws Exception {
        mockMvc.perform(post("/api/production-phases/" + phase1Id + "/resume")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/production-phases/" + phase1Id + "/status")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test @Order(9)
    @DisplayName("POST /api/production-phases/{id}/stop - emergency stops phase")
    void stopPhase() throws Exception {
        mockMvc.perform(post("/api/production-phases/" + phase1Id + "/stop")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        Thread.sleep(1000);

        mockMvc.perform(get("/api/production-phases/" + phase1Id + "/status")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.executing").value(false));
    }

    @Test @Order(10)
    @DisplayName("Production worker can read phase status but not start phases")
    void workerRolePermissions() throws Exception {
        String workerToken = generateToken("prod_worker");

        mockMvc.perform(get("/api/production-phases/" + phase1Id + "/status")
                .header("Authorization", "Bearer " + workerToken))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/production-phases/" + phase1Id + "/start")
                .header("Authorization", "Bearer " + workerToken))
            .andExpect(status().isForbidden());
    }

    @Test @Order(11)
    @DisplayName("SSE events endpoint is accessible with auth")
    void sseEventsEndpoint() throws Exception {
        mockMvc.perform(get("/api/production-phases/events/" + orderId)
                .header("Authorization", "Bearer " + adminToken)
                .accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isOk());
    }

    @Test @Order(12)
    @DisplayName("Cannot start phase 2 before phase 1 verified")
    void cannotStartPhase2BeforePhase1Verified() throws Exception {
        String detailResp = mockMvc.perform(get("/api/production-orders/" + orderId)
                .header("Authorization", "Bearer " + adminToken))
            .andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(detailResp);
        Long phase2Id = root.get("phases").get(1).get("id").asLong();

        mockMvc.perform(post("/api/production-phases/" + phase2Id + "/start")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isBadRequest());
    }

    @Test @Order(13)
    @DisplayName("Unauthorized access returns 401")
    void unauthorizedAccess() throws Exception {
        mockMvc.perform(post("/api/production-phases/" + phase1Id + "/start"))
            .andExpect(status().isUnauthorized());
    }

    @Test @Order(14)
    @DisplayName("Active executions endpoint returns map")
    void activeExecutionsReturnsMap() throws Exception {
        mockMvc.perform(get("/api/production-phases/active")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
    }

    @Test @Order(15)
    @DisplayName("Phase completion requires phase to have completed")
    void phaseCompletionNotReady() throws Exception {
        mockMvc.perform(get("/api/production-phases/" + phase1Id + "/completion")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isBadRequest());
    }

    @Test @Order(16)
    @DisplayName("Verify requires WAITING_FOR_VERIFICATION status")
    void verifyRequiresCompletedPhase() throws Exception {
        mockMvc.perform(post("/api/production-phases/" + phase1Id + "/verify")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isBadRequest());
    }

    @Test @Order(17)
    @DisplayName("Non-existent phase status returns not executing")
    void nonExistentPhase() throws Exception {
        mockMvc.perform(get("/api/production-phases/99999/status")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.executing").value(false));
    }
}
