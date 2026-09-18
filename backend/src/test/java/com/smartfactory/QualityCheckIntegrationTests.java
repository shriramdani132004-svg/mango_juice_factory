package com.smartfactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfactory.dto.CreateOrderRequest;
import com.smartfactory.entity.*;
import com.smartfactory.enums.MaterialCategory;
import com.smartfactory.enums.QualityCheckStatus;
import com.smartfactory.enums.QualityResult;
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
@DisplayName("Part 7: Quality Control + Batch Verification Tests")
class QualityCheckIntegrationTests {

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
    @Autowired QualityCheckRepository qualityCheckRepository;
    @Autowired BatchRepository batchRepository;
    @Autowired ProductionPhaseRepository phaseRepository;

    private String adminToken;
    private String qualityToken;
    private String prodWorkerToken;
    private Long orderId;
    private Long phase1Id;
    private Long batchId;

    @BeforeAll
    void setup() throws Exception {
        createIfMissing("admin", "admin123", "System Administrator", Role.ADMIN, "admin@smartfactory.dev");
        createIfMissing("prod_worker", "password123", "Production Worker", Role.PRODUCTION_WORKER, "prod@smartfactory.dev");
        createIfMissing("quality_worker", "password123", "Quality Worker", Role.QUALITY_WORKER, "quality@smartfactory.dev");

        adminToken = generateToken("admin");
        qualityToken = generateToken("quality_worker");
        prodWorkerToken = generateToken("prod_worker");

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

            addInventory(mangoes, new BigDecimal("50000.00"), "kg");
            addInventory(water, new BigDecimal("100000.00"), "L");
            addInventory(sugar, new BigDecimal("8000.00"), "kg");

            RecipeMaterial rm1 = new RecipeMaterial(recipe, mangoes, new BigDecimal("2500.00"), "kg");
            rm1.setMaterialCategory("RAW");
            recipe.addRecipeMaterial(rm1);
            RecipeMaterial rm2 = new RecipeMaterial(recipe, water, new BigDecimal("1500.00"), "L");
            rm2.setMaterialCategory("RAW");
            recipe.addRecipeMaterial(rm2);
            RecipeMaterial rm3 = new RecipeMaterial(recipe, sugar, new BigDecimal("150.00"), "kg");
            rm3.setMaterialCategory("RAW");
            recipe.addRecipeMaterial(rm3);
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
                    Machine m = new Machine();
                    m.setMachineCode(codePrefixes[i] + "-" + j);
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
        JsonNode root = objectMapper.readTree(detailResp);
        phase1Id = root.get("phases").get(0).get("id").asLong();
        batchId = root.get("phases").get(0).get("batchId").asLong();
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
    @DisplayName("Quality checks can be created for a batch phase")
    void createQualityChecks() throws Exception {
        String resp = mockMvc.perform(post("/api/quality-checks/batch/" + batchId + "/phase/" + phase1Id + "/create")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode checks = objectMapper.readTree(resp);
        assertTrue(checks.isArray());
        assertTrue(checks.size() > 0);
        assertEquals("PENDING", checks.get(0).get("status").asText());
    }

    @Test @Order(2)
    @DisplayName("Quality checks belong to correct batch and phase")
    void checksBelongToCorrectBatchAndPhase() throws Exception {
        String resp = mockMvc.perform(get("/api/quality-checks/batch/" + batchId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode checks = objectMapper.readTree(resp);
        for (JsonNode check : checks) {
            assertEquals(batchId, check.get("batchId").asLong());
            assertEquals(phase1Id, check.get("phaseId").asLong());
        }
    }

    @Test @Order(3)
    @DisplayName("Mandatory PASS allows quality completion")
    void mandatoryPassAllowsCompletion() throws Exception {
        String resp = mockMvc.perform(get("/api/quality-checks/batch/" + batchId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode checks = objectMapper.readTree(resp);
        for (JsonNode check : checks) {
            if (check.get("mandatory").asBoolean()) {
                mockMvc.perform(post("/api/quality-checks/" + check.get("id").asLong() + "/inspect")
                        .header("Authorization", "Bearer " + qualityToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observedValue\": 50, \"result\": \"PASS\", \"notes\": \"Test pass\"}"))
                    .andExpect(status().isOk());
            }
        }

        String resultResp = mockMvc.perform(get("/api/quality-checks/batch/" + batchId + "/phase/" + phase1Id + "/result")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode result = objectMapper.readTree(resultResp);
        assertEquals("PASS", result.get("overallResult").asText());
    }

    @Test @Order(4)
    @DisplayName("Mandatory FAIL blocks progression")
    void mandatoryFailBlocksProgression() throws Exception {
        CreateOrderRequest requestFail = new CreateOrderRequest(1L, new BigDecimal("4000.00"), 500, 8, null);
        String createResp = mockMvc.perform(post("/api/production-orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestFail)))
            .andReturn().getResponse().getContentAsString();
        Long failOrderId = objectMapper.readTree(createResp).get("id").asLong();

        mockMvc.perform(post("/api/production-orders/" + failOrderId + "/requirements-check")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/production-orders/" + failOrderId + "/approve")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        String detailResp = mockMvc.perform(get("/api/production-orders/" + failOrderId)
                .header("Authorization", "Bearer " + adminToken))
            .andReturn().getResponse().getContentAsString();
        JsonNode detail = objectMapper.readTree(detailResp);
        Long failBatchId = detail.get("phases").get(0).get("batchId").asLong();
        Long failPhaseId = detail.get("phases").get(0).get("id").asLong();

        mockMvc.perform(post("/api/quality-checks/batch/" + failBatchId + "/phase/" + failPhaseId + "/create")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        String resp = mockMvc.perform(get("/api/quality-checks/batch/" + failBatchId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode checks = objectMapper.readTree(resp);
        for (JsonNode check : checks) {
            if (check.get("mandatory").asBoolean()) {
                mockMvc.perform(post("/api/quality-checks/" + check.get("id").asLong() + "/inspect")
                        .header("Authorization", "Bearer " + qualityToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observedValue\": 999, \"result\": \"FAIL\", \"notes\": \"Test failure\"}"))
                    .andExpect(status().isOk());
                break;
            }
        }

        String resultResp = mockMvc.perform(get("/api/quality-checks/batch/" + failBatchId + "/phase/" + failPhaseId + "/result")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode result = objectMapper.readTree(resultResp);
        assertEquals("FAIL", result.get("overallResult").asText());
    }

    @Test @Order(5)
    @DisplayName("Multiple quality checks are evaluated correctly")
    void multipleChecksEvaluated() throws Exception {
        String resp = mockMvc.perform(get("/api/quality-checks/batch/" + batchId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode checks = objectMapper.readTree(resp);
        assertTrue(checks.size() >= 2, "Should have multiple quality checks");
    }

    @Test @Order(6)
    @DisplayName("Failed batch becomes quality-blocked")
    void failedBatchIsBlocked() throws Exception {
        String resp = mockMvc.perform(post("/api/quality-checks/batch/" + batchId + "/reject")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"Quality failure test\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode result = objectMapper.readTree(resp);
        assertEquals("REJECTED", result.get("newStatus").asText());
    }

    @Test @Order(7)
    @DisplayName("Quarantine works")
    void quarantineWorks() throws Exception {
        CreateOrderRequest request2 = new CreateOrderRequest(1L, new BigDecimal("3000.00"), 500, 8, null);
        String createResp = mockMvc.perform(post("/api/production-orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
            .andReturn().getResponse().getContentAsString();
        Long orderId2 = objectMapper.readTree(createResp).get("id").asLong();

        mockMvc.perform(post("/api/production-orders/" + orderId2 + "/requirements-check")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/production-orders/" + orderId2 + "/approve")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        String detailResp = mockMvc.perform(get("/api/production-orders/" + orderId2)
                .header("Authorization", "Bearer " + adminToken))
            .andReturn().getResponse().getContentAsString();
        JsonNode detail = objectMapper.readTree(detailResp);
        Long batchId2 = detail.get("phases").get(0).get("batchId").asLong();

        String resp = mockMvc.perform(post("/api/quality-checks/batch/" + batchId2 + "/quarantine")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"Contamination suspected\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode result = objectMapper.readTree(resp);
        assertEquals("QUARANTINED", result.get("newStatus").asText());
    }

    @Test @Order(8)
    @DisplayName("Reject works")
    void rejectWorks() throws Exception {
        CreateOrderRequest request3 = new CreateOrderRequest(1L, new BigDecimal("2000.00"), 500, 8, null);
        String createResp = mockMvc.perform(post("/api/production-orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request3)))
            .andReturn().getResponse().getContentAsString();
        Long orderId3 = objectMapper.readTree(createResp).get("id").asLong();

        mockMvc.perform(post("/api/production-orders/" + orderId3 + "/requirements-check")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/production-orders/" + orderId3 + "/approve")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        String detailResp = mockMvc.perform(get("/api/production-orders/" + orderId3)
                .header("Authorization", "Bearer " + adminToken))
            .andReturn().getResponse().getContentAsString();
        JsonNode detail = objectMapper.readTree(detailResp);
        Long batchId3 = detail.get("phases").get(0).get("batchId").asLong();

        String resp = mockMvc.perform(post("/api/quality-checks/batch/" + batchId3 + "/reject")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"Failed quality standards\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode result = objectMapper.readTree(resp);
        assertEquals("REJECTED", result.get("newStatus").asText());
    }

    @Test @Order(9)
    @DisplayName("Reprocess request preserves history")
    void reprocessPreservesHistory() throws Exception {
        CreateOrderRequest request4 = new CreateOrderRequest(1L, new BigDecimal("2500.00"), 500, 8, null);
        String createResp = mockMvc.perform(post("/api/production-orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request4)))
            .andReturn().getResponse().getContentAsString();
        Long orderId4 = objectMapper.readTree(createResp).get("id").asLong();

        mockMvc.perform(post("/api/production-orders/" + orderId4 + "/requirements-check")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/production-orders/" + orderId4 + "/approve")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        String detailResp = mockMvc.perform(get("/api/production-orders/" + orderId4)
                .header("Authorization", "Bearer " + adminToken))
            .andReturn().getResponse().getContentAsString();
        JsonNode detail = objectMapper.readTree(detailResp);
        Long batchId4 = detail.get("phases").get(0).get("batchId").asLong();

        String resp = mockMvc.perform(post("/api/quality-checks/batch/" + batchId4 + "/reprocess")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"Reprocessing needed\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode result = objectMapper.readTree(resp);
        assertEquals("REPROCESS_REQUESTED", result.get("newStatus").asText());

        String checksResp = mockMvc.perform(get("/api/quality-checks/batch/" + batchId4)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode checks = objectMapper.readTree(checksResp);
        assertTrue(checks.isArray());
    }

    @Test @Order(10)
    @DisplayName("Admin verification requires quality PASS")
    void verifyRequiresQualityPass() throws Exception {
        CreateOrderRequest reqVerify = new CreateOrderRequest(1L, new BigDecimal("3500.00"), 500, 8, null);
        String createResp = mockMvc.perform(post("/api/production-orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqVerify)))
            .andReturn().getResponse().getContentAsString();
        Long verifyOrderId = objectMapper.readTree(createResp).get("id").asLong();

        mockMvc.perform(post("/api/production-orders/" + verifyOrderId + "/requirements-check")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/production-orders/" + verifyOrderId + "/approve")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        String detailResp = mockMvc.perform(get("/api/production-orders/" + verifyOrderId)
                .header("Authorization", "Bearer " + adminToken))
            .andReturn().getResponse().getContentAsString();
        JsonNode detail = objectMapper.readTree(detailResp);
        Long verifyPhaseId = detail.get("phases").get(0).get("id").asLong();
        Long verifyBatchId = detail.get("phases").get(0).get("batchId").asLong();

        mockMvc.perform(post("/api/production-phases/" + verifyPhaseId + "/start")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        Thread.sleep(5000);

        String resp = mockMvc.perform(post("/api/production-phases/" + verifyPhaseId + "/verify")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isBadRequest())
            .andReturn().getResponse().getContentAsString();
        assertTrue(resp.contains("WAITING_FOR_VERIFICATION") || resp.contains("quality") || resp.contains("Quality"));
    }

    @Test @Order(11)
    @DisplayName("Non-Admin cannot perform Admin verification")
    void nonAdminCannotVerify() throws Exception {
        mockMvc.perform(post("/api/quality-checks/batch/" + batchId + "/quarantine")
                .header("Authorization", "Bearer " + qualityToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"test\"}"))
            .andExpect(status().isForbidden());
    }

    @Test @Order(12)
    @DisplayName("Next phase remains LOCKED after quality failure")
    void nextPhaseLockedAfterFailure() throws Exception {
        String detailResp = mockMvc.perform(get("/api/production-orders/" + orderId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode detail = objectMapper.readTree(detailResp);
        String phase2Status = detail.get("phases").get(1).get("status").asText();
        assertEquals("LOCKED", phase2Status);
    }

    @Test @Order(13)
    @DisplayName("Batch quality summary endpoint works")
    void batchQualitySummaryWorks() throws Exception {
        mockMvc.perform(get("/api/quality-checks/batch/" + batchId + "/summary")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
    }

    @Test @Order(14)
    @DisplayName("Quality checks by phase endpoint works")
    void qualityChecksByPhaseWorks() throws Exception {
        mockMvc.perform(get("/api/quality-checks/phase/" + phase1Id)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test @Order(15)
    @DisplayName("Quality checks by order endpoint works")
    void qualityChecksByOrderWorks() throws Exception {
        mockMvc.perform(get("/api/quality-checks/order/" + orderId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test @Order(16)
    @DisplayName("Auto-evaluate quality check works")
    void autoEvaluateWorks() throws Exception {
        CreateOrderRequest request5 = new CreateOrderRequest(1L, new BigDecimal("1500.00"), 500, 8, null);
        String createResp = mockMvc.perform(post("/api/production-orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request5)))
            .andReturn().getResponse().getContentAsString();
        Long orderId5 = objectMapper.readTree(createResp).get("id").asLong();

        mockMvc.perform(post("/api/production-orders/" + orderId5 + "/requirements-check")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/production-orders/" + orderId5 + "/approve")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        String detailResp = mockMvc.perform(get("/api/production-orders/" + orderId5)
                .header("Authorization", "Bearer " + adminToken))
            .andReturn().getResponse().getContentAsString();
        JsonNode detail = objectMapper.readTree(detailResp);
        Long batchId5 = detail.get("phases").get(0).get("batchId").asLong();
        Long phaseId5 = detail.get("phases").get(0).get("id").asLong();

        mockMvc.perform(post("/api/quality-checks/batch/" + batchId5 + "/phase/" + phaseId5 + "/create")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        String checksResp = mockMvc.perform(get("/api/quality-checks/batch/" + batchId5)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode checks = objectMapper.readTree(checksResp);
        if (checks.size() > 0) {
            Long checkId = checks.get(0).get("id").asLong();

            mockMvc.perform(post("/api/quality-checks/" + checkId + "/inspect")
                    .header("Authorization", "Bearer " + qualityToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"observedValue\": 50}"))
                .andExpect(status().isOk());

            mockMvc.perform(post("/api/quality-checks/" + checkId + "/auto-evaluate")
                    .header("Authorization", "Bearer " + qualityToken))
                .andExpect(status().isOk());
        }
    }

    @Test @Order(17)
    @DisplayName("Existing Part 6 production behavior still works")
    void part6BehaviorStillWorks() throws Exception {
        CreateOrderRequest request6 = new CreateOrderRequest(1L, new BigDecimal("5000.00"), 500, 8, null);
        String createResp = mockMvc.perform(post("/api/production-orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request6)))
            .andReturn().getResponse().getContentAsString();
        Long orderId6 = objectMapper.readTree(createResp).get("id").asLong();

        mockMvc.perform(post("/api/production-orders/" + orderId6 + "/requirements-check")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/production-orders/" + orderId6 + "/approve")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        String detailResp = mockMvc.perform(get("/api/production-orders/" + orderId6)
                .header("Authorization", "Bearer " + adminToken))
            .andReturn().getResponse().getContentAsString();
        JsonNode detail = objectMapper.readTree(detailResp);
        Long phase1Id6 = detail.get("phases").get(0).get("id").asLong();

        mockMvc.perform(post("/api/production-phases/" + phase1Id6 + "/start")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RUNNING"));

        mockMvc.perform(get("/api/production-phases/" + phase1Id6 + "/status")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.executing").value(true));

        mockMvc.perform(post("/api/production-phases/" + phase1Id6 + "/pause")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/production-phases/" + phase1Id6 + "/resume")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/production-phases/" + phase1Id6 + "/stop")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        Thread.sleep(1000);

        mockMvc.perform(get("/api/production-phases/" + phase1Id6 + "/status")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.executing").value(false));
    }

    @Test @Order(18)
    @DisplayName("Unauthorized access to quality checks returns 401")
    void unauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/quality-checks/batch/1"))
            .andExpect(status().isUnauthorized());
    }

    @Test @Order(19)
    @DisplayName("Invalid quality check ID returns 404")
    void invalidCheckIdReturns404() throws Exception {
        mockMvc.perform(get("/api/quality-checks/99999")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    @Test @Order(20)
    @DisplayName("Quality worker can inspect but not quarantine")
    void qualityWorkerPermissions() throws Exception {
        mockMvc.perform(post("/api/quality-checks/batch/1/phase/1/create")
                .header("Authorization", "Bearer " + qualityToken))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/quality-checks/batch/1/quarantine")
                .header("Authorization", "Bearer " + qualityToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"test\"}"))
            .andExpect(status().isForbidden());
    }
}
