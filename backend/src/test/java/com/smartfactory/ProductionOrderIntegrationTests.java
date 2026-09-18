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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductionOrderIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private ProductRepository productRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private MachineRepository machineRepository;
    @Autowired private MachineTypeRepository machineTypeRepository;
    @Autowired private ProductionLineRepository productionLineRepository;
    @Autowired private FactoryRepository factoryRepository;

    private static String adminToken;
    private static String prodWorkerToken;
    private static String maintWorkerToken;
    private static String qualityWorkerToken;

    @BeforeAll
    static void setupOnce(@Autowired UserRepository userRepository,
                          @Autowired PasswordEncoder passwordEncoder,
                          @Autowired JwtUtil jwtUtil,
                          @Autowired ProductRepository productRepository,
                          @Autowired RecipeRepository recipeRepository,
                          @Autowired MaterialRepository materialRepository,
                          @Autowired InventoryRepository inventoryRepository,
                          @Autowired MachineRepository machineRepository,
                          @Autowired MachineTypeRepository machineTypeRepository,
                          @Autowired ProductionLineRepository productionLineRepository,
                          @Autowired FactoryRepository factoryRepository) {

        createIfMissing(userRepository, passwordEncoder, "admin", "admin123", "System Administrator", Role.ADMIN, "admin@smartfactory.dev");
        createIfMissing(userRepository, passwordEncoder, "prod_worker", "password123", "Production Worker 1", Role.PRODUCTION_WORKER, "prod@smartfactory.dev");
        createIfMissing(userRepository, passwordEncoder, "maint_worker", "password123", "Maintenance Worker 1", Role.MAINTENANCE_WORKER, "maint@smartfactory.dev");
        createIfMissing(userRepository, passwordEncoder, "quality_worker", "password123", "Quality Worker 1", Role.QUALITY_WORKER, "quality@smartfactory.dev");

        adminToken = generateToken(userRepository, jwtUtil, "admin");
        prodWorkerToken = generateToken(userRepository, jwtUtil, "prod_worker");
        maintWorkerToken = generateToken(userRepository, jwtUtil, "maint_worker");
        qualityWorkerToken = generateToken(userRepository, jwtUtil, "quality_worker");

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

            addInventory(inventoryRepository, mangoes, new BigDecimal("50000.00"), "kg");
            addInventory(inventoryRepository, water, new BigDecimal("100000.00"), "L");
            addInventory(inventoryRepository, sugar, new BigDecimal("8000.00"), "kg");
            addInventory(inventoryRepository, bottles, new BigDecimal("200000.00"), "pcs");
            addInventory(inventoryRepository, caps, new BigDecimal("200000.00"), "pcs");

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

    private static void addInventory(InventoryRepository repo, Material material, BigDecimal qty, String unit) {
        Inventory inv = new Inventory(material, qty, unit, "Test Warehouse");
        repo.save(inv);
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/production-orders - create order with valid request")
    void createProductionOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
            1L, new BigDecimal("5000.00"), 500, 8, null
        );

        mockMvc.perform(post("/api/production-orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.orderNumber").isNotEmpty())
            .andExpect(jsonPath("$.productName").value("Mango Beverage"))
            .andExpect(jsonPath("$.inputQuantity").value(5000.00))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.estimatedOutputQty").isNumber())
            .andExpect(jsonPath("$.estimatedOutputUnit").value("btl"))
            .andExpect(jsonPath("$.overallStatus").value("DRAFT"));
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/production-orders - production worker can create order")
    void prodWorkerCanCreateOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
            1L, new BigDecimal("3000.00"), 500, 5, null
        );

        mockMvc.perform(post("/api/production-orders")
                .header("Authorization", "Bearer " + prodWorkerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productName").value("Mango Beverage"));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/production-orders - maintenance worker cannot create order")
    void maintWorkerCannotCreateOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
            1L, new BigDecimal("1000.00"), 500, 3, null
        );

        mockMvc.perform(post("/api/production-orders")
                .header("Authorization", "Bearer " + maintWorkerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/production-orders - quality worker cannot create order")
    void qualityWorkerCannotCreateOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
            1L, new BigDecimal("1000.00"), 500, 3, null
        );

        mockMvc.perform(post("/api/production-orders")
                .header("Authorization", "Bearer " + qualityWorkerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/production-orders - missing product returns error")
    void createOrderMissingProduct() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
            999L, new BigDecimal("5000.00"), 500, 8, null
        );

        mockMvc.perform(post("/api/production-orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/production-orders - zero quantity returns error")
    void createOrderZeroQuantity() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
            1L, BigDecimal.ZERO, 500, 8, null
        );

        mockMvc.perform(post("/api/production-orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/production-orders - list all orders")
    void listAllOrders() throws Exception {
        mockMvc.perform(get("/api/production-orders")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/production-orders/{id} - get order detail with correct values")
    void getOrderDetail() throws Exception {
        mockMvc.perform(get("/api/production-orders/1")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderNumber").isNotEmpty())
            .andExpect(jsonPath("$.productName").value("Mango Beverage"))
            .andExpect(jsonPath("$.productCode").value("PROD-MANGO"))
            .andExpect(jsonPath("$.recipeName").isNotEmpty())
            .andExpect(jsonPath("$.inputQuantity").value(5000.00))
            .andExpect(jsonPath("$.bottleSizeMl").value(500))
            .andExpect(jsonPath("$.priority").value(8))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.phases").isArray());
    }

    @Test
    @Order(9)
    @DisplayName("POST /api/production-orders/{id}/requirements-check - runs requirement calculation")
    void runRequirementsCheck() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/production-orders/1/requirements-check")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(1))
            .andExpect(jsonPath("$.orderNumber").isNotEmpty())
            .andExpect(jsonPath("$.productName").value("Mango Beverage"))
            .andExpect(jsonPath("$.inputQuantity").value(5000.00))
            .andExpect(jsonPath("$.batchCount").isNumber())
            .andExpect(jsonPath("$.wastePercentage").isNumber())
            .andExpect(jsonPath("$.phases").isArray())
            .andExpect(jsonPath("$.phases.length()").value(10))
            .andExpect(jsonPath("$.requirements").isArray())
            .andExpect(jsonPath("$.requirements.length()").value(15))
            .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode requirements = root.get("requirements");
        assertTrue(requirements.size() >= 15, "Should have at least 15 requirements (5 materials + 10 machines)");

        boolean hasMaterial = false;
        boolean hasMachine = false;
        for (JsonNode req : requirements) {
            String type = req.get("requirementType").asText();
            if ("MATERIAL".equals(type)) hasMaterial = true;
            if ("MACHINE".equals(type)) hasMachine = true;
        }
        assertTrue(hasMaterial, "Should have MATERIAL requirements");
        assertTrue(hasMachine, "Should have MACHINE requirements");
    }

    @Test
    @Order(10)
    @DisplayName("GET /api/production-orders/1 - after requirements check, status is REQUIREMENTS_CHECK")
    void orderStatusAfterRequirementsCheck() throws Exception {
        mockMvc.perform(get("/api/production-orders/1")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REQUIREMENTS_CHECK"))
            .andExpect(jsonPath("$.requirements.length()").value(15))
            .andExpect(jsonPath("$.requirements[0].requirementType").exists())
            .andExpect(jsonPath("$.requirements[0].itemName").exists())
            .andExpect(jsonPath("$.requirements[0].status").exists())
            .andExpect(jsonPath("$.requirements[0].mandatory").exists())
            .andExpect(jsonPath("$.requirements[0].explanation").exists());
    }

    @Test
    @Order(11)
    @DisplayName("POST /api/production-orders/{id}/approve - approve order transitions to READY")
    void approveOrder() throws Exception {
        mockMvc.perform(post("/api/production-orders/1/approve")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    @Order(12)
    @DisplayName("GET /api/production-orders/1 - after approval, phases have LOCKED/READY statuses")
    void phasesAfterApproval() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/production-orders/1")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phases").isArray())
            .andExpect(jsonPath("$.phases.length()").value(10))
            .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode phases = root.get("phases");
        JsonNode firstPhase = phases.get(0);
        assertEquals("READY", firstPhase.get("status").asText());

        for (int i = 1; i < 10; i++) {
            assertEquals("LOCKED", phases.get(i).get("status").asText());
        }
    }

    @Test
    @Order(13)
    @DisplayName("POST /api/production-orders/{id}/approve - cannot approve non-REQUIREMENTS_CHECK order")
    void cannotApproveNonRequirementCheckOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
            1L, new BigDecimal("2000.00"), 500, 5, null
        );
        String body = objectMapper.writeValueAsString(request);
        MvcResult createResult = mockMvc.perform(post("/api/production-orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long orderId = created.get("id").asLong();

        mockMvc.perform(post("/api/production-orders/" + orderId + "/approve")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Order(14)
    @DisplayName("POST /api/production-orders/{id}/cancel - cancel order works")
    void cancelOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
            1L, new BigDecimal("2000.00"), 500, 3, null
        );
        MvcResult createResult = mockMvc.perform(post("/api/production-orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long orderId = created.get("id").asLong();

        mockMvc.perform(post("/api/production-orders/" + orderId + "/cancel")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/production-orders/" + orderId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @Order(15)
    @DisplayName("GET /api/production-orders/machine-capacities - returns capacity for all phases")
    void getMachineCapacities() throws Exception {
        mockMvc.perform(get("/api/production-orders/machine-capacities")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(10))
            .andExpect(jsonPath("$[0].capability").value("RECEIVING"))
            .andExpect(jsonPath("$[0].phaseType").value("RECEIVE_AND_INSPECT"))
            .andExpect(jsonPath("$[0].totalMachines").isNumber())
            .andExpect(jsonPath("$[0].availableMachines").isNumber())
            .andExpect(jsonPath("$[0].status").value("GREEN"));
    }

    @Test
    @Order(16)
    @DisplayName("POST /api/production-orders - unauthenticated request returns 401")
    void unauthenticatedRequestReturns401() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
            1L, new BigDecimal("5000.00"), 500, 8, null
        );

        mockMvc.perform(post("/api/production-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(17)
    @DisplayName("POST /api/production-orders - invalid token returns 401")
    void invalidTokenReturns401() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
            1L, new BigDecimal("5000.00"), 500, 8, null
        );

        mockMvc.perform(post("/api/production-orders")
                .header("Authorization", "Bearer invalidtoken123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(18)
    @DisplayName("POST /api/production-orders/1/requirements-check - requirements have GREEN/YELLOW/RED status")
    void requirementsHaveCorrectStatuses() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
            1L, new BigDecimal("5000.00"), 500, 5, null
        );
        MvcResult createResult = mockMvc.perform(post("/api/production-orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long orderId = created.get("id").asLong();

        MvcResult result = mockMvc.perform(post("/api/production-orders/" + orderId + "/requirements-check")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode requirements = root.get("requirements");

        for (JsonNode req : requirements) {
            String status = req.get("status").asText();
            assertTrue(status.equals("GREEN") || status.equals("YELLOW") || status.equals("RED"),
                "Status must be GREEN, YELLOW, or RED but was: " + status);
        }
    }

    @Test
    @Order(19)
    @DisplayName("GET /api/production-orders/1 - 5000kg order calculates ~2 batches correctly")
    void batchCalculationFor5000Kg() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/production-orders/1/requirements-check")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        int batchCount = root.get("batchCount").asInt();
        assertEquals(2, batchCount, "5000 kg / 2500 kg batchSize = 2 batches");

        double expectedBottles = root.get("expectedBottleCount").asDouble();
        assertTrue(expectedBottles > 0, "Expected bottle count should be > 0");
    }

    @Test
    @Order(20)
    @DisplayName("POST /api/production-orders - negative quantity returns error")
    void negativeQuantityReturnsError() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
            1L, new BigDecimal("-100.00"), 500, 8, null
        );

        mockMvc.perform(post("/api/production-orders")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
