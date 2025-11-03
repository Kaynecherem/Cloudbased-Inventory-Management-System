package hr.algebra.cloudbased_inventory_management_system.service;

import hr.algebra.cloudbased_inventory_management_system.dto.TopMoverItem;
import hr.algebra.cloudbased_inventory_management_system.dto.UsageReportItem;
import hr.algebra.cloudbased_inventory_management_system.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;

    private final StockMovementRepository stockMovementRepository;

    @Transactional(readOnly = true)
    public List<UsageReportItem> getUsageReport(Instant from, Instant to, Integer limit) {
        InstantRange range = validateRange(from, to);
        Pageable pageable = PageRequest.of(0, resolveLimit(limit));
        return stockMovementRepository.findUsageReport(range.from(), range.to(), pageable);
    }

    @Transactional(readOnly = true)
    public List<TopMoverItem> getTopMovers(Instant from, Instant to, Integer limit) {
        InstantRange range = validateRange(from, to);
        Pageable pageable = PageRequest.of(0, resolveLimit(limit));
        return stockMovementRepository.findTopMovers(range.from(), range.to(), pageable);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> exportCsv(String type, Instant from, Instant to, Integer limit) {
        ReportType reportType = ReportType.from(type);
        List<String[]> rows = switch (reportType) {
            case USAGE -> buildUsageRows(getUsageReport(from, to, limit));
            case TOP_MOVERS -> buildTopMoverRows(getTopMovers(from, to, limit));
        };

        String filename = "report-" + reportType.fileNameSuffix();
        byte[] payload = buildCsv(rows).getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(payload.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(new ByteArrayResource(payload));
    }

    private List<String[]> buildUsageRows(List<UsageReportItem> items) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Item ID", "SKU", "Name", "Total In", "Total Out", "Net Usage"});
        for (UsageReportItem item : items) {
            rows.add(new String[]{
                    String.valueOf(item.getItemId()),
                    emptyIfNull(item.getSku()),
                    emptyIfNull(item.getName()),
                    item.getTotalIn().toPlainString(),
                    item.getTotalOut().toPlainString(),
                    item.getNetUsage().toPlainString()
            });
        }
        return rows;
    }

    private List<String[]> buildTopMoverRows(List<TopMoverItem> items) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Item ID", "SKU", "Name", "Total Movement", "Total In", "Total Out"});
        for (TopMoverItem item : items) {
            rows.add(new String[]{
                    String.valueOf(item.getItemId()),
                    emptyIfNull(item.getSku()),
                    emptyIfNull(item.getName()),
                    item.getTotalMovement().toPlainString(),
                    item.getTotalIn().toPlainString(),
                    item.getTotalOut().toPlainString()
            });
        }
        return rows;
    }

    private String buildCsv(List<String[]> rows) {
        StringBuilder builder = new StringBuilder();
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(escape(row[i]));
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private String escape(String value) {
        String sanitized = emptyIfNull(value);
        if (!sanitized.contains(",") && !sanitized.contains("\"")) {
            return sanitized;
        }
        return '"' + sanitized.replace("\"", "\"\"") + '"';
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private InstantRange validateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(BAD_REQUEST, "From date must be before to date");
        }
        return new InstantRange(from, to);
    }

    private enum ReportType {
        USAGE("usage", "usage.csv"),
        TOP_MOVERS("top-movers", "top-movers.csv");

        private final String type;
        private final String fileNameSuffix;

        ReportType(String type, String fileNameSuffix) {
            this.type = type;
            this.fileNameSuffix = fileNameSuffix;
        }

        static ReportType from(String raw) {
            if (!StringUtils.hasText(raw)) {
                throw new ResponseStatusException(BAD_REQUEST, "Report type is required");
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            for (ReportType value : values()) {
                if (value.type.equals(normalized)) {
                    return value;
                }
            }
            throw new ResponseStatusException(BAD_REQUEST, "Unsupported report type: " + raw);
        }

        String fileNameSuffix() {
            return fileNameSuffix;
        }
    }

    private record InstantRange(Instant from, Instant to) {
    }
}
