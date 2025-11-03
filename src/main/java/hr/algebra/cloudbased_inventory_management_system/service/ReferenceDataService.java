package hr.algebra.cloudbased_inventory_management_system.service;

import hr.algebra.cloudbased_inventory_management_system.dto.ReferenceDataUpdateRequest;
import hr.algebra.cloudbased_inventory_management_system.entity.ReferenceData;
import hr.algebra.cloudbased_inventory_management_system.entity.ReferenceDataType;
import hr.algebra.cloudbased_inventory_management_system.repository.ItemRepository;
import hr.algebra.cloudbased_inventory_management_system.repository.ReferenceDataRepository;
import hr.algebra.cloudbased_inventory_management_system.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReferenceDataService {

    private final ReferenceDataRepository referenceDataRepository;
    private final ItemRepository itemRepository;
    private final StockMovementRepository stockMovementRepository;

    @Transactional(readOnly = true)
    public List<String> getValues(ReferenceDataType type) {
        List<String> storedValues = referenceDataRepository.findByTypeOrderByValueAsc(type).stream()
                .map(ReferenceData::getValue)
                .toList();
        if (!storedValues.isEmpty()) {
            return sanitizeAndSort(storedValues);
        }
        return switch (type) {
            case UNIT -> sanitizeAndSort(itemRepository.findDistinctUnits());
            case CATEGORY -> sanitizeAndSort(itemRepository.findDistinctCategories());
            case REASON_CODE -> sanitizeAndSort(stockMovementRepository.findDistinctReasonCodes());
        };
    }

    @Transactional
    public List<String> updateValues(ReferenceDataType type, ReferenceDataUpdateRequest request) {
        Objects.requireNonNull(request, "Request must not be null");
        LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
        if (request.values() != null) {
            for (String value : request.values()) {
                String sanitized = normalizeValue(value);
                if (!StringUtils.hasText(sanitized)) {
                    continue;
                }
                String key = sanitized.toLowerCase(Locale.ROOT);
                normalized.putIfAbsent(key, sanitized);
            }
        }

        if (normalized.isEmpty()) {
            referenceDataRepository.deleteByType(type);
            return List.of();
        }

        Map<String, ReferenceData> existing = referenceDataRepository.findByType(type).stream()
                .collect(Collectors.toMap(entry -> entry.getValue().toLowerCase(Locale.ROOT), entry -> entry));

        List<ReferenceData> toSave = new ArrayList<>();
        Set<String> processedKeys = new HashSet<>();
        for (String entryKey : normalized.keySet()) {
            String displayValue = normalized.get(entryKey);
            ReferenceData existingEntry = existing.get(entryKey);
            if (existingEntry != null) {
                processedKeys.add(entryKey);
                if (!existingEntry.getValue().equals(displayValue)) {
                    existingEntry.setValue(displayValue);
                    toSave.add(existingEntry);
                }
            } else {
                ReferenceData newEntry = new ReferenceData();
                newEntry.setType(type);
                newEntry.setValue(displayValue);
                toSave.add(newEntry);
            }
        }

        List<ReferenceData> toDelete = existing.entrySet().stream()
                .filter(entry -> !processedKeys.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        if (!toDelete.isEmpty()) {
            referenceDataRepository.deleteAllInBatch(toDelete);
        }
        if (!toSave.isEmpty()) {
            referenceDataRepository.saveAll(toSave);
        }

        return sanitizeAndSort(referenceDataRepository.findByTypeOrderByValueAsc(type).stream()
                .map(ReferenceData::getValue)
                .toList());
    }

    private List<String> sanitizeAndSort(List<String> values) {
        return values.stream()
                .map(this::normalizeValue)
                .filter(StringUtils::hasText)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)),
                        treeSet -> treeSet.stream().toList()
                ));
    }

    private String normalizeValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}
