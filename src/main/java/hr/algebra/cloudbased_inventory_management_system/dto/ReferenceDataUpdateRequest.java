package hr.algebra.cloudbased_inventory_management_system.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReferenceDataUpdateRequest(@NotNull List<String> values) {
}
