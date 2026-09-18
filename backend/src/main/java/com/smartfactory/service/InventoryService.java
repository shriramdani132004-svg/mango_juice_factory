package com.smartfactory.service;

import com.smartfactory.dto.*;
import com.smartfactory.entity.Inventory;
import com.smartfactory.entity.InventoryTransaction;
import com.smartfactory.entity.Material;
import com.smartfactory.enums.InventoryStatus;
import com.smartfactory.enums.InventoryTransactionType;
import com.smartfactory.exception.InsufficientStockException;
import com.smartfactory.exception.ResourceNotFoundException;
import com.smartfactory.repository.InventoryRepository;
import com.smartfactory.repository.InventoryTransactionRepository;
import com.smartfactory.repository.MaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final MaterialRepository materialRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            InventoryTransactionRepository transactionRepository,
                            MaterialRepository materialRepository) {
        this.inventoryRepository = inventoryRepository;
        this.transactionRepository = transactionRepository;
        this.materialRepository = materialRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryDto> getAllInventory() {
        return inventoryRepository.findAll().stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public InventoryDto getInventoryById(Long id) {
        Inventory inv = inventoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found: " + id));
        return toDto(inv);
    }

    @Transactional(readOnly = true)
    public InventoryDto getInventoryByMaterialId(Long materialId) {
        Inventory inv = inventoryRepository.findByMaterialId(materialId)
            .orElseThrow(() -> new ResourceNotFoundException("No inventory for material: " + materialId));
        return toDto(inv);
    }

    @Transactional(readOnly = true)
    public InventoryDto getInventoryByMaterialCode(String code) {
        Inventory inv = inventoryRepository.findByMaterialCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("No inventory for material code: " + code));
        return toDto(inv);
    }

    @Transactional(readOnly = true)
    public List<InventoryDto> getLowStockItems() {
        return inventoryRepository.findLowStockItems().stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public InventoryCheckResponse checkAvailability(InventoryCheckRequest request) {
        Material material = materialRepository.findById(request.materialId())
            .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + request.materialId()));

        Inventory inv = inventoryRepository.findByMaterialId(request.materialId())
            .orElse(null);

        BigDecimal available = inv != null ? inv.getAvailableQuantity() : BigDecimal.ZERO;
        int comparison = available.compareTo(request.requiredQuantity());

        String status;
        String explanation;
        if (comparison >= 0) {
            status = "GREEN";
            explanation = "Sufficient stock available";
        } else if (available.compareTo(BigDecimal.ZERO) > 0) {
            status = "RED";
            explanation = "Insufficient stock: have " + available + " " + request.unit() + ", need " + request.requiredQuantity() + " " + request.unit();
        } else {
            status = "RED";
            explanation = "No stock available";
        }

        return new InventoryCheckResponse(
            material.getId(),
            material.getName(),
            material.getMaterialCode(),
            request.requiredQuantity(),
            available,
            request.unit(),
            status,
            explanation
        );
    }

    public void addStock(Long materialId, BigDecimal quantity, String reason, String referenceType, Long referenceId) {
        Material material = materialRepository.findById(materialId)
            .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + materialId));

        Inventory inv = inventoryRepository.findByMaterialId(materialId)
            .orElse(new Inventory(material, BigDecimal.ZERO, material.getUnit(), null));

        BigDecimal previous = inv.getCurrentQuantity();
        inv.setCurrentQuantity(previous.add(quantity));
        inv.updateStatus();
        if (reason != null && reason.contains("restock")) {
            inv.setLastRestockedAt(java.time.LocalDateTime.now());
        }
        inventoryRepository.save(inv);

        recordTransaction(material, InventoryTransactionType.RECEIPT, quantity, material.getUnit(),
            previous, inv.getCurrentQuantity(), referenceType, referenceId, reason);
    }

    public void reserveStock(Long materialId, BigDecimal quantity, String referenceType, Long referenceId) {
        Material material = materialRepository.findById(materialId)
            .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + materialId));

        Inventory inv = inventoryRepository.findByMaterialId(materialId)
            .orElseThrow(() -> new ResourceNotFoundException("No inventory for material: " + materialId));

        BigDecimal available = inv.getAvailableQuantity();
        if (available.compareTo(quantity) < 0) {
            throw new InsufficientStockException(
                "Cannot reserve " + quantity + " " + inv.getUnit() +
                ". Available: " + available + " " + inv.getUnit() +
                " (total: " + inv.getCurrentQuantity() + ", already reserved: " + inv.getReservedQuantity() + ")");
        }

        BigDecimal previousReserved = inv.getReservedQuantity();
        inv.setReservedQuantity(previousReserved.add(quantity));
        inv.updateStatus();
        inventoryRepository.save(inv);

        recordTransaction(material, InventoryTransactionType.RESERVATION, quantity, inv.getUnit(),
            previousReserved, inv.getReservedQuantity(), referenceType, referenceId, "Stock reserved");
    }

    public void releaseReservation(Long materialId, BigDecimal quantity, String referenceType, Long referenceId) {
        Material material = materialRepository.findById(materialId)
            .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + materialId));

        Inventory inv = inventoryRepository.findByMaterialId(materialId)
            .orElseThrow(() -> new ResourceNotFoundException("No inventory for material: " + materialId));

        if (inv.getReservedQuantity().compareTo(quantity) < 0) {
            throw new InsufficientStockException(
                "Cannot release " + quantity + " reserved. Only " + inv.getReservedQuantity() + " reserved.");
        }

        BigDecimal previousReserved = inv.getReservedQuantity();
        inv.setReservedQuantity(previousReserved.subtract(quantity));
        inv.updateStatus();
        inventoryRepository.save(inv);

        recordTransaction(material, InventoryTransactionType.RELEASE, quantity, inv.getUnit(),
            previousReserved, inv.getReservedQuantity(), referenceType, referenceId, "Reservation released");
    }

    public void consumeStock(Long materialId, BigDecimal quantity, String referenceType, Long referenceId) {
        Material material = materialRepository.findById(materialId)
            .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + materialId));

        Inventory inv = inventoryRepository.findByMaterialId(materialId)
            .orElseThrow(() -> new ResourceNotFoundException("No inventory for material: " + materialId));

        if (inv.getCurrentQuantity().compareTo(quantity) < 0) {
            throw new InsufficientStockException(
                "Cannot consume " + quantity + " " + inv.getUnit() + ". Available: " + inv.getCurrentQuantity());
        }

        BigDecimal previous = inv.getCurrentQuantity();
        inv.setCurrentQuantity(previous.subtract(quantity));
        if (inv.getReservedQuantity().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal newReserved = inv.getReservedQuantity().subtract(quantity);
            inv.setReservedQuantity(newReserved.max(BigDecimal.ZERO));
        }
        inv.updateStatus();
        inventoryRepository.save(inv);

        recordTransaction(material, InventoryTransactionType.CONSUMPTION, quantity, inv.getUnit(),
            previous, inv.getCurrentQuantity(), referenceType, referenceId, "Stock consumed");
    }

    public void recordProduction(Long materialId, BigDecimal quantity, String referenceType, Long referenceId) {
        Material material = materialRepository.findById(materialId)
            .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + materialId));

        Inventory inv = inventoryRepository.findByMaterialId(materialId)
            .orElse(new Inventory(material, BigDecimal.ZERO, material.getUnit(), null));

        BigDecimal previous = inv.getCurrentQuantity();
        inv.setCurrentQuantity(previous.add(quantity));
        inv.updateStatus();
        inventoryRepository.save(inv);

        recordTransaction(material, InventoryTransactionType.PRODUCTION, quantity, inv.getUnit(),
            previous, inv.getCurrentQuantity(), referenceType, referenceId, "Production output");
    }

    public void recordWaste(Long materialId, BigDecimal quantity, String reason, String referenceType, Long referenceId) {
        Material material = materialRepository.findById(materialId)
            .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + materialId));

        Inventory inv = inventoryRepository.findByMaterialId(materialId)
            .orElseThrow(() -> new ResourceNotFoundException("No inventory for material: " + materialId));

        if (inv.getCurrentQuantity().compareTo(quantity) < 0) {
            throw new InsufficientStockException("Cannot record waste of " + quantity + ". Available: " + inv.getCurrentQuantity());
        }

        BigDecimal previous = inv.getCurrentQuantity();
        inv.setCurrentQuantity(previous.subtract(quantity));
        inv.updateStatus();
        inventoryRepository.save(inv);

        recordTransaction(material, InventoryTransactionType.WASTE, quantity, inv.getUnit(),
            previous, inv.getCurrentQuantity(), referenceType, referenceId, reason);
    }

    public void recordRejection(Long materialId, BigDecimal quantity, String reason, String referenceType, Long referenceId) {
        Material material = materialRepository.findById(materialId)
            .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + materialId));

        Inventory inv = inventoryRepository.findByMaterialId(materialId)
            .orElseThrow(() -> new ResourceNotFoundException("No inventory for material: " + materialId));

        if (inv.getCurrentQuantity().compareTo(quantity) < 0) {
            throw new InsufficientStockException("Cannot record rejection of " + quantity + ". Available: " + inv.getCurrentQuantity());
        }

        BigDecimal previous = inv.getCurrentQuantity();
        inv.setCurrentQuantity(previous.subtract(quantity));
        inv.updateStatus();
        inventoryRepository.save(inv);

        recordTransaction(material, InventoryTransactionType.REJECTION, quantity, inv.getUnit(),
            previous, inv.getCurrentQuantity(), referenceType, referenceId, reason);
    }

    @Transactional(readOnly = true)
    public List<InventoryTransactionDto> getTransactionsByMaterial(Long materialId) {
        return transactionRepository.findByMaterialIdOrderByCreatedAtDesc(materialId).stream()
            .map(this::toTransactionDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryTransactionDto> getRecentTransactions(int limit) {
        return transactionRepository.findRecentTransactions(
            PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))).stream()
            .map(this::toTransactionDto)
            .toList();
    }

    private void recordTransaction(Material material, InventoryTransactionType type, BigDecimal quantity,
                                    String unit, BigDecimal previous, BigDecimal resulting,
                                    String referenceType, Long referenceId, String reason) {
        InventoryTransaction txn = new InventoryTransaction(material, type, quantity, unit, previous, resulting);
        txn.setReferenceType(referenceType);
        txn.setReferenceId(referenceId);
        txn.setReason(reason);
        transactionRepository.save(txn);
    }

    private InventoryDto toDto(Inventory inv) {
        return new InventoryDto(
            inv.getId(),
            inv.getMaterial().getId(),
            inv.getMaterial().getName(),
            inv.getMaterial().getMaterialCode(),
            inv.getMaterial().getMaterialType() != null ? inv.getMaterial().getMaterialType().name() : "UNKNOWN",
            inv.getCurrentQuantity(),
            inv.getReservedQuantity(),
            inv.getAvailableQuantity(),
            inv.getMinimumThreshold(),
            inv.getUnit(),
            inv.getLocation(),
            inv.getStatus().name()
        );
    }

    private InventoryTransactionDto toTransactionDto(InventoryTransaction txn) {
        return new InventoryTransactionDto(
            txn.getId(),
            txn.getMaterial().getId(),
            txn.getMaterial().getName(),
            txn.getTransactionType().name(),
            txn.getQuantity(),
            txn.getUnit(),
            txn.getPreviousQuantity(),
            txn.getResultingQuantity(),
            txn.getReferenceType(),
            txn.getReferenceId(),
            txn.getReason(),
            txn.getNotes(),
            txn.getCreatedAt() != null ? txn.getCreatedAt().toString() : null
        );
    }
}
