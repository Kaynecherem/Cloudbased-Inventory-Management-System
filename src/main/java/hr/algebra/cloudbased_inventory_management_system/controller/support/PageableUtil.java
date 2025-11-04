package hr.algebra.cloudbased_inventory_management_system.controller.support;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PageableUtil {

    private PageableUtil() {
    }

    public static Pageable buildPageable(Integer page,
                                         Integer size,
                                         String sort,
                                         int defaultPage,
                                         int defaultSize,
                                         int maxSize,
                                         Sort defaultSort,
                                         Set<String> allowedSortProperties) {
        int pageNumber = page != null && page >= 0 ? page : defaultPage;
        int pageSize = size != null && size > 0 ? Math.min(size, maxSize) : defaultSize;
        Sort resolvedSort = resolveSort(sort, defaultSort, allowedSortProperties);
        return PageRequest.of(pageNumber, pageSize, resolvedSort);
    }

    public static Sort resolveSort(String sort,
                                   Sort defaultSort,
                                   Set<String> allowedSortProperties) {
        if (!StringUtils.hasText(sort)) {
            return defaultSort == null ? Sort.unsorted() : defaultSort;
        }

        Map<String, String> allowed = normalizeAllowed(allowedSortProperties);
        List<Sort.Order> orders = new ArrayList<>();
        String[] segments = sort.split("[;|]");
        if (segments.length == 0) {
            segments = new String[]{sort};
        }
        for (String segment : segments) {
            if (!StringUtils.hasText(segment)) {
                continue;
            }
            String[] parts = segment.split(",");
            String property = parts[0].trim();
            if (!StringUtils.hasText(property)) {
                continue;
            }
            String normalizedProperty = property.toLowerCase(Locale.ROOT);
            String canonicalProperty = allowed.get(normalizedProperty);
            if (canonicalProperty == null) {
                continue;
            }
            Sort.Direction direction = Sort.Direction.ASC;
            if (parts.length > 1) {
                String dir = parts[1].trim();
                if (dir.equalsIgnoreCase("desc")) {
                    direction = Sort.Direction.DESC;
                } else if (dir.equalsIgnoreCase("asc")) {
                    direction = Sort.Direction.ASC;
                }
            }
            orders.add(new Sort.Order(direction, canonicalProperty));
        }

        if (orders.isEmpty()) {
            return defaultSort == null ? Sort.unsorted() : defaultSort;
        }
        return Sort.by(orders);
    }

    private static Map<String, String> normalizeAllowed(Set<String> allowedSortProperties) {
        if (allowedSortProperties == null || allowedSortProperties.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        for (String property : allowedSortProperties) {
            if (!StringUtils.hasText(property)) {
                continue;
            }
            String trimmed = property.trim();
            normalized.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
        }
        return normalized;
    }
}

