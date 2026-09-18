package com.smartfactory.service;

import com.smartfactory.dto.*;
import com.smartfactory.entity.*;
import com.smartfactory.enums.Role;
import com.smartfactory.exception.ResourceNotFoundException;
import com.smartfactory.exception.ValidationException;
import com.smartfactory.repository.*;
import com.smartfactory.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional
public class ProductionOrderService {

    private static final Logger log = LoggerFactory.getLogger(ProductionOrderService.class);

    private static final String[] PHASE_NAMES = {
        "Receive & Inspect Mangoes",
        "Wash & Sort",
        "Peel & Pulp",
        "Filter",
        "Blend",
        "Pasteurize",
        "Quality Inspection",
        "Fill & Cap",
        "Label & Package",
        "Warehouse"
    };

    private static final String[] PHASE_TYPES = {
        "RECEIVE_AND_INSPECT", "WASH_AND_SORT", "PEEL_AND_PULP",
        "FILTER", "BLEND", "PASTEURIZE",
        "QUALITY_INSPECTION", "FILL_AND_CAP", "LABEL_AND_PACKAGE", "WAREHOUSE"
    };

    private static final String[] MACHINE_CAPABILITIES = {
        "RECEIVING", "WASHING", "PEELING", "FILTERING",
        "BLENDING", "PASTEURIZING", "QUALITY_INSPECTION",
        "FILLING", "LABELING", "WAREHOUSING"
    };

    private static final int[] DURATION_MINUTES = {15, 20, 30, 20, 30, 25, 15, 25, 20, 15};
    private static final BigDecimal[] WASTE_PERCENTAGES = {
        new BigDecimal("0.02"), new BigDecimal("0.03"), new BigDecimal("0.15"),
        new BigDecimal("0.02"), new BigDecimal("0.01"), new BigDecimal("0.005"),
        new BigDecimal("0.03"), new BigDecimal("0.01"), new BigDecimal("0.005"),
        new BigDecimal("0.002")
    };

    private final ProductionOrderRepository orderRepository;
    private final ProductionOrderRequirementRepository requirementRepository;
    private final BatchRepository batchRepository;
    private final ProductionPhaseRepository phaseRepository;
    private final ProductRepository productRepository;
    private final RecipeRepository recipeRepository;
    private final MaterialRepository materialRepository;
    private final InventoryRepository inventoryRepository;
    private final MachineRepository machineRepository;
    private final RequirementEngine requirementEngine;
    private final MachineCapacityService machineCapacityService;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public ProductionOrderService(ProductionOrderRepository orderRepository,
                                   ProductionOrderRequirementRepository requirementRepository,
                                   BatchRepository batchRepository,
                                   ProductionPhaseRepository phaseRepository,
                                   ProductRepository productRepository,
                                   RecipeRepository recipeRepository,
                                   MaterialRepository materialRepository,
                                   InventoryRepository inventoryRepository,
                                   MachineRepository machineRepository,
                                   RequirementEngine requirementEngine,
                                   MachineCapacityService machineCapacityService,
                                   AuditService auditService,
                                   UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.requirementRepository = requirementRepository;
        this.batchRepository = batchRepository;
        this.phaseRepository = phaseRepository;
        this.productRepository = productRepository;
        this.recipeRepository = recipeRepository;
        this.materialRepository = materialRepository;
        this.inventoryRepository = inventoryRepository;
        this.machineRepository = machineRepository;
        this.requirementEngine = requirementEngine;
        this.machineCapacityService = machineCapacityService;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    public OrderDetailDto createOrder(CreateOrderRequest request) {
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.productId()));

        List<Recipe> recipes = recipeRepository.findActiveRecipesByProductId(request.productId());
        if (recipes.isEmpty()) {
            throw new ValidationException("No active recipe found for product: " + product.getName());
        }
        Recipe recipe = recipes.get(0);

        Material inputMaterial = materialRepository.findById(1L)
            .orElseThrow(() -> new ResourceNotFoundException("Input material not found"));

        String orderNumber = generateOrderNumber();

        ProductionOrder order = new ProductionOrder(
            orderNumber, product, recipe, inputMaterial,
            request.mangoQuantityKg(), request.bottleSizeMl(), request.priority()
        );
        order.setDeadline(request.deadline());

        BigDecimal outputQty = calculateOutputQuantity(recipe, request.mangoQuantityKg());
        order.setEstimatedOutputQty(outputQty);
        order.setEstimatedOutputUnit("btl");
        order.setStatus("DRAFT");

        order = orderRepository.save(order);

        UserPrincipal principal = getCurrentPrincipal();
        if (principal != null) {
            User user = userRepository.findByUsername(principal.getUsername()).orElse(null);
            if (user != null) order.setCreatedBy(user);
        }
        order = orderRepository.save(order);

        log.info("Created production order {} for {} kg mangoes -> {} bottles",
            orderNumber, request.mangoQuantityKg(), outputQty);

        return getOrderDetail(order.getId());
    }

    public ProductionPlanResponse runRequirementsCheck(Long orderId) {
        ProductionOrder order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Production order not found: " + orderId));

        List<RequirementItemDto> requirements = requirementEngine.calculateRequirements(order);

        for (RequirementItemDto req : requirements) {
            ProductionOrderRequirement entity = new ProductionOrderRequirement(
                order,
                req.requirementType(),
                req.itemName(),
                req.requiredQuantity(),
                req.requiredUnit(),
                req.availableQuantity(),
                req.status(),
                req.mandatory(),
                req.explanation()
            );
            entity.setItemType(req.itemType());
            entity.setItemCode(req.itemCode());
            if (req.materialId() != null) {
                Material mat = materialRepository.findById(req.materialId()).orElse(null);
                if (mat != null) entity.setMaterial(mat);
            }
            requirementRepository.save(entity);
        }

        order.setStatus("REQUIREMENTS_CHECK");
        orderRepository.save(order);

        List<PhasePreviewDto> phases = generatePhasePreviews(order);

        BigDecimal wastePct = calculateTotalWastePercentage(order);
        int duration = calculateTotalDuration(order);
        BigDecimal productionRate = calculateProductionRate(order);
        BigDecimal expectedBottles = order.getEstimatedOutputQty();

        String overallStatus = requirements.stream()
            .anyMatch(r -> "RED".equals(r.status())) ? "BLOCKED" : "READY";

        return new ProductionPlanResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getProduct().getName(),
            order.getInputQuantity(),
            "kg",
            order.getInputQuantity(),
            expectedBottles,
            productionRate,
            duration,
            calculateBatchCount(order),
            wastePct,
            phases,
            overallStatus,
            requirements
        );
    }

    public OrderDetailDto approveOrder(Long orderId) {
        ProductionOrder order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Production order not found: " + orderId));

        if (!"REQUIREMENTS_CHECK".equals(order.getStatus())) {
            throw new ValidationException("Order must be in REQUIREMENTS_CHECK status to approve. Current: " + order.getStatus());
        }

        List<RequirementItemDto> requirements = requirementEngine.calculateRequirements(order);
        boolean hasRed = requirements.stream().anyMatch(r -> "RED".equals(r.status()));
        if (hasRed) {
            throw new ValidationException("Cannot approve order with RED requirements — resolve material or machine shortages first");
        }

        order.setStatus("READY");
        order = orderRepository.save(order);

        createBatches(order);
        createPhases(order);

        log.info("Production order {} approved and batches/phases created", order.getOrderNumber());

        return getOrderDetail(order.getId());
    }

    public List<OrderListItemDto> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(this::toListDto)
            .toList();
    }

    public OrderDetailDto getOrderDetail(Long orderId) {
        ProductionOrder order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Production order not found: " + orderId));

        List<RequirementItemDto> requirements = requirementRepository.findByOrderIdOrderByRequirementTypeAsc(orderId).stream()
            .map(this::toRequirementDto)
            .toList();

        List<PhasePreviewDto> phases = new ArrayList<>();
        List<Batch> batches = batchRepository.findByOrderId(orderId);
        if (!batches.isEmpty()) {
            phases = phaseRepository.findByBatchIdOrderByPhaseNumberAsc(batches.get(0).getId()).stream()
                .map(this::toPhasePreviewDto)
                .toList();
        }

        if (phases.isEmpty()) {
            phases = generatePhasePreviews(order);
        }

        String overallStatus = "READY";
        if ("DRAFT".equals(order.getStatus())) overallStatus = "DRAFT";
        else if ("REQUIREMENTS_CHECK".equals(order.getStatus())) {
            boolean hasRed = requirements.stream().anyMatch(r -> "RED".equals(r.status()));
            overallStatus = hasRed ? "BLOCKED" : "READY";
        }

        return new OrderDetailDto(
            order.getId(),
            order.getOrderNumber(),
            order.getProduct().getId(),
            order.getProduct().getName(),
            order.getProduct().getProductCode(),
            order.getRecipe().getId(),
            order.getRecipe().getName(),
            order.getInputQuantity(),
            "kg",
            order.getBottleSizeMl(),
            order.getPriority(),
            order.getDeadline() != null ? order.getDeadline().toString() : null,
            order.getStatus(),
            order.getEstimatedOutputQty(),
            order.getEstimatedOutputUnit(),
            order.getEstimatedProductionRate(),
            order.getEstimatedDurationMinutes(),
            requirements,
            phases,
            overallStatus,
            order.getCreatedAt() != null ? order.getCreatedAt().toString() : null
        );
    }

    public void cancelOrder(Long orderId) {
        ProductionOrder order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Production order not found: " + orderId));

        if ("COMPLETED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new ValidationException("Cannot cancel order in status: " + order.getStatus());
        }

        order.setStatus("CANCELLED");
        orderRepository.save(order);
    }

    private List<PhasePreviewDto> generatePhasePreviews(ProductionOrder order) {
        List<PhasePreviewDto> phases = new ArrayList<>();
        BigDecimal inputQty = order.getInputQuantity();

        for (int i = 0; i < 10; i++) {
            phases.add(new PhasePreviewDto(
                null,
                null,
                i + 1,
                PHASE_NAMES[i],
                PHASE_TYPES[i],
                i == 0 ? "READY" : "LOCKED",
                MACHINE_CAPABILITIES[i],
                DURATION_MINUTES[i],
                BigDecimal.valueOf(200)
            ));
        }
        return phases;
    }

    private void createBatches(ProductionOrder order) {
        BigDecimal batchSizeKg = order.getRecipe().getBatchSize();
        if (batchSizeKg == null || batchSizeKg.compareTo(BigDecimal.ZERO) == 0) {
            batchSizeKg = order.getRecipe().getOutputQuantity() != null
                ? order.getRecipe().getOutputQuantity() : new BigDecimal("1000.00");
        }

        int batchCount = calculateBatchCount(order);
        BigDecimal qtyPerBatch = order.getInputQuantity().divide(BigDecimal.valueOf(batchCount), 4, RoundingMode.HALF_UP);

        for (int i = 1; i <= batchCount; i++) {
            String batchCode = order.getOrderNumber() + "-B" + String.format("%03d", i);
            Batch batch = new Batch(batchCode, order, order.getInputMaterial(), qtyPerBatch);
            batch.setStatus("PENDING");
            batchRepository.save(batch);
        }
    }

    private void createPhases(ProductionOrder order) {
        List<Batch> batches = batchRepository.findByOrderId(order.getId());

        for (Batch batch : batches) {
            BigDecimal batchInput = batch.getInputQuantity();

            for (int i = 0; i < 10; i++) {
                BigDecimal wasteRate = WASTE_PERCENTAGES[i];
                BigDecimal wasteQty = batchInput.multiply(wasteRate).setScale(4, RoundingMode.HALF_UP);
                BigDecimal outputQty = batchInput.subtract(wasteQty).setScale(4, RoundingMode.HALF_UP);

                ProductionPhase phase = new ProductionPhase(
                    batch,
                    i + 1,
                    PHASE_TYPES[i],
                    MACHINE_CAPABILITIES[i],
                    DURATION_MINUTES[i],
                    BigDecimal.valueOf(200)
                );
                phase.setInputQuantity(batchInput);
                phase.setOutputQuantity(outputQty);
                phase.setWasteQuantity(wasteQty);
                phase.setStatus(i == 0 ? "READY" : "LOCKED");

                phaseRepository.save(phase);

                batchInput = outputQty;
            }

            batch.setCurrentPhase(1);
            batch.setStatus("PENDING");
            batchRepository.save(batch);
        }
    }

    private int calculateBatchCount(ProductionOrder order) {
        BigDecimal batchSizeKg = order.getRecipe().getBatchSize();
        if (batchSizeKg == null || batchSizeKg.compareTo(BigDecimal.ZERO) == 0) {
            batchSizeKg = order.getRecipe().getOutputQuantity() != null
                ? order.getRecipe().getOutputQuantity() : new BigDecimal("1000.00");
        }
        return order.getInputQuantity().divide(batchSizeKg, 0, RoundingMode.CEILING).intValue();
    }

    private BigDecimal calculateOutputQuantity(Recipe recipe, BigDecimal inputQty) {
        BigDecimal batchSize = recipe.getBatchSize();
        if (batchSize == null || batchSize.compareTo(BigDecimal.ZERO) == 0) {
            batchSize = recipe.getOutputQuantity() != null ? recipe.getOutputQuantity() : new BigDecimal("1000.00");
        }

        BigDecimal scaleFactor = inputQty.divide(batchSize, 6, RoundingMode.HALF_UP);
        BigDecimal outputPerBatch = recipe.getOutputQuantity() != null ? recipe.getOutputQuantity() : new BigDecimal("1000.00");
        BigDecimal yieldPct = recipe.getYieldPercentage() != null ? recipe.getYieldPercentage() : new BigDecimal("100.00");

        return outputPerBatch.multiply(scaleFactor)
            .multiply(yieldPct).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotalWastePercentage(ProductionOrder order) {
        BigDecimal totalWaste = BigDecimal.ZERO;
        for (BigDecimal w : WASTE_PERCENTAGES) {
            totalWaste = totalWaste.add(w);
        }
        return totalWaste.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    private int calculateTotalDuration(ProductionOrder order) {
        int total = 0;
        for (int d : DURATION_MINUTES) total += d;
        return total;
    }

    private BigDecimal calculateProductionRate(ProductionOrder order) {
        return BigDecimal.valueOf(200);
    }

    private String generateOrderNumber() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = orderRepository.count() + 1;
        return "ORD-" + dateStr + "-" + String.format("%04d", count);
    }

    private UserPrincipal getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
            return (UserPrincipal) auth.getPrincipal();
        }
        return null;
    }

    private RequirementItemDto toRequirementDto(ProductionOrderRequirement r) {
        return new RequirementItemDto(
            r.getId(),
            r.getRequirementType(),
            r.getItemName(),
            r.getItemType(),
            r.getItemCode(),
            r.getMaterial() != null ? r.getMaterial().getId() : null,
            r.getRequiredQuantity(),
            r.getRequiredUnit(),
            r.getAvailableQuantity(),
            r.getStatus(),
            r.getMandatory(),
            r.getExplanation()
        );
    }

    private PhasePreviewDto toPhasePreviewDto(ProductionPhase p) {
        return new PhasePreviewDto(
            p.getId(),
            p.getBatch() != null ? p.getBatch().getId() : null,
            p.getPhaseNumber(),
            PHASE_NAMES[p.getPhaseNumber() - 1],
            p.getPhaseType(),
            p.getStatus(),
            p.getRequiredMachineCapability(),
            p.getEstimatedDurationMinutes(),
            p.getProductionRate()
        );
    }

    private OrderListItemDto toListDto(ProductionOrder order) {
        return new OrderListItemDto(
            order.getId(),
            order.getOrderNumber(),
            order.getProduct().getName(),
            order.getProduct().getProductCode(),
            order.getInputQuantity(),
            "kg",
            order.getBottleSizeMl(),
            order.getPriority(),
            order.getStatus(),
            order.getEstimatedOutputQty(),
            order.getEstimatedOutputUnit(),
            order.getEstimatedDurationMinutes(),
            order.getCreatedAt() != null ? order.getCreatedAt().toString() : null
        );
    }
}
