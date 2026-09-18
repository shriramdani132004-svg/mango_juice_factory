package com.smartfactory.controller;

import com.smartfactory.dto.*;
import com.smartfactory.service.MachineCapacityService;
import com.smartfactory.service.ProductionOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/production-orders")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProductionOrderController {

    private final ProductionOrderService productionOrderService;
    private final MachineCapacityService machineCapacityService;

    public ProductionOrderController(ProductionOrderService productionOrderService,
                                      MachineCapacityService machineCapacityService) {
        this.productionOrderService = productionOrderService;
        this.machineCapacityService = machineCapacityService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_WORKER')")
    public ResponseEntity<OrderDetailDto> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderDetailDto order = productionOrderService.createOrder(request);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_WORKER')")
    public ResponseEntity<List<OrderListItemDto>> getAllOrders() {
        return ResponseEntity.ok(productionOrderService.getAllOrders());
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_WORKER')")
    public ResponseEntity<OrderDetailDto> getOrderDetail(@PathVariable Long orderId) {
        return ResponseEntity.ok(productionOrderService.getOrderDetail(orderId));
    }

    @PostMapping("/{orderId}/requirements-check")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_WORKER')")
    public ResponseEntity<ProductionPlanResponse> runRequirementsCheck(@PathVariable Long orderId) {
        return ResponseEntity.ok(productionOrderService.runRequirementsCheck(orderId));
    }

    @PostMapping("/{orderId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_WORKER')")
    public ResponseEntity<OrderDetailDto> approveOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(productionOrderService.approveOrder(orderId));
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_WORKER')")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId) {
        productionOrderService.cancelOrder(orderId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/machine-capacities")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION_WORKER')")
    public ResponseEntity<List<MachineCapacityResponse>> getMachineCapacities() {
        return ResponseEntity.ok(machineCapacityService.getAllPhaseCapacities());
    }
}
