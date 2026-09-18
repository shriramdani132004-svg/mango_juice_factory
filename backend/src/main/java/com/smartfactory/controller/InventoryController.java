package com.smartfactory.controller;

import com.smartfactory.dto.*;
import com.smartfactory.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<List<InventoryDto>> getAllInventory() {
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryDto> getInventoryById(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.getInventoryById(id));
    }

    @GetMapping("/material/{materialId}")
    public ResponseEntity<InventoryDto> getInventoryByMaterialId(@PathVariable Long materialId) {
        return ResponseEntity.ok(inventoryService.getInventoryByMaterialId(materialId));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryDto>> getLowStockItems() {
        return ResponseEntity.ok(inventoryService.getLowStockItems());
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<InventoryTransactionDto>> getRecentTransactions(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(inventoryService.getRecentTransactions(limit));
    }

    @GetMapping("/transactions/material/{materialId}")
    public ResponseEntity<List<InventoryTransactionDto>> getTransactionsByMaterial(@PathVariable Long materialId) {
        return ResponseEntity.ok(inventoryService.getTransactionsByMaterial(materialId));
    }

    @PostMapping("/check")
    public ResponseEntity<InventoryCheckResponse> checkAvailability(@RequestBody InventoryCheckRequest request) {
        return ResponseEntity.ok(inventoryService.checkAvailability(request));
    }

    @PostMapping("/add")
    public ResponseEntity<String> addStock(
            @RequestParam Long materialId,
            @RequestParam BigDecimal quantity,
            @RequestParam(required = false) String reason) {
        inventoryService.addStock(materialId, quantity, reason, null, null);
        return ResponseEntity.ok("Stock added successfully");
    }

    @PostMapping("/reserve")
    public ResponseEntity<String> reserveStock(
            @RequestParam Long materialId,
            @RequestParam BigDecimal quantity) {
        inventoryService.reserveStock(materialId, quantity, null, null);
        return ResponseEntity.ok("Stock reserved successfully");
    }

    @PostMapping("/release")
    public ResponseEntity<String> releaseReservation(
            @RequestParam Long materialId,
            @RequestParam BigDecimal quantity) {
        inventoryService.releaseReservation(materialId, quantity, null, null);
        return ResponseEntity.ok("Reservation released successfully");
    }

    @PostMapping("/consume")
    public ResponseEntity<String> consumeStock(
            @RequestParam Long materialId,
            @RequestParam BigDecimal quantity) {
        inventoryService.consumeStock(materialId, quantity, null, null);
        return ResponseEntity.ok("Stock consumed successfully");
    }

    @PostMapping("/produce")
    public ResponseEntity<String> recordProduction(
            @RequestParam Long materialId,
            @RequestParam BigDecimal quantity) {
        inventoryService.recordProduction(materialId, quantity, null, null);
        return ResponseEntity.ok("Production recorded successfully");
    }

    @PostMapping("/waste")
    public ResponseEntity<String> recordWaste(
            @RequestParam Long materialId,
            @RequestParam BigDecimal quantity,
            @RequestParam(required = false) String reason) {
        inventoryService.recordWaste(materialId, quantity, reason, null, null);
        return ResponseEntity.ok("Waste recorded successfully");
    }

    @PostMapping("/reject")
    public ResponseEntity<String> recordRejection(
            @RequestParam Long materialId,
            @RequestParam BigDecimal quantity,
            @RequestParam(required = false) String reason) {
        inventoryService.recordRejection(materialId, quantity, reason, null, null);
        return ResponseEntity.ok("Rejection recorded successfully");
    }
}
