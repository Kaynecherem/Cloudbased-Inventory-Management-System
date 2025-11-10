package hr.algebra.cloudbased_inventory_management_system.service;

import hr.algebra.cloudbased_inventory_management_system.dto.TopMoverItem;
import hr.algebra.cloudbased_inventory_management_system.dto.UsageReportItem;
import hr.algebra.cloudbased_inventory_management_system.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;
    private static final int DEFAULT_RANGE_DAYS = 30;
    private static final BigDecimal SECONDS_PER_DAY = BigDecimal.valueOf(86_400);

    private final StockMovementRepository stockMovementRepository;

    @Transactional(readOnly = true)
    public List<UsageReportItem> getUsageReport(Instant from, Instant to, Integer limit) {
        InstantRange range = resolveRange(from, to);
        Pageable pageable = PageRequest.of(0, resolveLimit(limit));
        BigDecimal windowDays = calculateWindowDays(range);
        return stockMovementRepository.findUsageReport(range.from(), range.to(), windowDays, pageable);
    }

    @Transactional(readOnly = true)
    public List<TopMoverItem> getTopMovers(Instant from, Instant to, Integer limit, String orderBy) {
        InstantRange range = resolveRange(from, to);
        Pageable pageable = PageRequest.of(0, resolveLimit(limit));
        TopMoverSort sort = TopMoverSort.from(orderBy);
        return switch (sort) {
            case OUT -> stockMovementRepository.findTopMoversByOut(range.from(), range.to(), pageable);
            case MOVEMENT -> stockMovementRepository.findTopMoversByMovement(range.from(), range.to(), pageable);
        };
    }

    @Transactional(readOnly = true)
    public ResponseEntity<StreamingResponseBody> exportCsv(String type, Instant from, Instant to, Integer limit, String orderBy) {
        ReportType reportType = ReportType.from(type);
        List<String[]> rows = switch (reportType) {
            case USAGE -> buildUsageRows(getUsageReport(from, to, limit));
            case TOP_MOVERS -> buildTopMoverRows(getTopMovers(from, to, limit, orderBy));
        };

        String filename = "report-" + reportType.fileNameSuffix();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        StreamingResponseBody body = outputStream -> {
            try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                writeCsv(rows, writer);
            }
        };

        return ResponseEntity.ok()
                .headers(headers)
                .body(body);
    }

    private List<String[]> buildUsageRows(List<UsageReportItem> items) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Item ID", "SKU", "Name", "Total In", "Total Out", "Net Usage", "Average Out / Day"});
        for (UsageReportItem item : items) {
            rows.add(new String[]{
                    String.valueOf(item.getItemId()),
                    emptyIfNull(item.getSku()),
                    emptyIfNull(item.getName()),
                    item.getTotalIn().toPlainString(),
                    item.getTotalOut().toPlainString(),
                    item.getNetUsage().toPlainString(),
                    item.getAverageOutPerDay().toPlainString()
            });
        }
        return rows;
    }

    private List<String[]> buildTopMoverRows(List<TopMoverItem> items) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Item ID", "SKU", "Name", "Total In", "Total Out", "Total Movement"});
        for (TopMoverItem item : items) {
            rows.add(new String[]{
                    String.valueOf(item.getItemId()),
                    emptyIfNull(item.getSku()),
                    emptyIfNull(item.getName()),
                    item.getTotalIn().toPlainString(),
                    item.getTotalOut().toPlainString(),
                    item.getTotalMovement().toPlainString()
            });
        }
        return rows;
    }

    private void writeCsv(List<String[]> rows, OutputStreamWriter writer) throws IOException {
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) {
                    writer.write(',');
                }
                writer.write(escape(row[i]));
            }
            writer.write('\n');
        }
        writer.flush();
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

    private InstantRange resolveRange(Instant from, Instant to) {
        Instant effectiveTo = to != null ? to : Instant.now();
        Instant effectiveFrom = from != null ? from : effectiveTo.minus(Duration.ofDays(DEFAULT_RANGE_DAYS));
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new ResponseStatusException(BAD_REQUEST, "From date must be before to date");
        }
        return new InstantRange(effectiveFrom, effectiveTo);
    }

    private BigDecimal calculateWindowDays(InstantRange range) {
        Duration duration = Duration.between(range.from(), range.to());
        if (duration.isNegative()) {
            duration = duration.negated();
        }
        if (duration.isZero()) {
            return BigDecimal.ONE;
        }
        BigDecimal seconds = BigDecimal.valueOf(duration.getSeconds());
        if (duration.getNano() != 0) {
            BigDecimal fractionalSeconds = BigDecimal.valueOf(duration.getNano())
                    .divide(BigDecimal.valueOf(1_000_000_000L), 6, RoundingMode.HALF_UP);
            seconds = seconds.add(fractionalSeconds);
        }
        BigDecimal days = seconds.divide(SECONDS_PER_DAY, 6, RoundingMode.HALF_UP);
        if (days.compareTo(BigDecimal.ONE) < 0) {
            return BigDecimal.ONE;
        }
        return days;
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

    private enum TopMoverSort {
        MOVEMENT("movement"),
        OUT("out");

        private final String key;

        TopMoverSort(String key) {
            this.key = key;
        }

        static TopMoverSort from(String raw) {
            if (!StringUtils.hasText(raw)) {
                return MOVEMENT;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            for (TopMoverSort value : values()) {
                if (value.key.equals(normalized)) {
                    return value;
                }
            }
            throw new ResponseStatusException(BAD_REQUEST, "Unsupported order: " + raw);
        }
    }

    private record InstantRange(Instant from, Instant to) {
    }
}
