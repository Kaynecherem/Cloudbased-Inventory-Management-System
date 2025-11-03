package hr.algebra.cloudbased_inventory_management_system.controller;

import hr.algebra.cloudbased_inventory_management_system.dto.LowStockAlertResponse;
import hr.algebra.cloudbased_inventory_management_system.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping("/low-stock")
    public List<LowStockAlertResponse> getLowStockAlerts() {
        return alertService.getLowStockAlerts();
    }

    @PostMapping("/dismiss")
    public ResponseEntity<Void> dismissAlerts() {
        return ResponseEntity.noContent().build();
    }
}
