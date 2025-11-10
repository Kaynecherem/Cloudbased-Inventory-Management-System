package hr.algebra.cloudbased_inventory_management_system.config;

import hr.algebra.cloudbased_inventory_management_system.service.AuditContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final AuditContext auditContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String requestId = resolveRequestId(request);
        request.setAttribute(REQUEST_ID_HEADER, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            String user = auditContext.getCurrentAuditor();
            String path = request.getRequestURI();
            String queryString = request.getQueryString();
            if (StringUtils.hasText(queryString)) {
                path = path + "?" + queryString;
            }
            LOGGER.info("requestId={} method={} path={} status={} durationMs={} user={}",
                    requestId,
                    request.getMethod(),
                    path,
                    response.getStatus(),
                    duration,
                    user);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (StringUtils.hasText(requestId)) {
            return requestId.trim();
        }
        return UUID.randomUUID().toString();
    }
}

