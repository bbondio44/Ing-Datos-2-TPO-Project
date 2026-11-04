package ar.edu.uade.logistica.observability;

import ar.edu.uade.logistica.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseAuditContext {
    private final JdbcTemplate jdbcTemplate;
    private final HttpServletRequest request;

    public DatabaseAuditContext(JdbcTemplate jdbcTemplate, HttpServletRequest request) {
        this.jdbcTemplate = jdbcTemplate;
        this.request = request;
    }

    public void applyToCurrentTransaction() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof AuthenticatedUser user)) return;
        String correlationId = String.valueOf(request.getAttribute(CorrelationIdFilter.ATTRIBUTE));
        jdbcTemplate.queryForObject("select set_config('app.usuario_id', ?, true)",
                String.class, user.userId().toString());
        jdbcTemplate.queryForObject("select set_config('app.correlacion_id', ?, true)",
                String.class, correlationId);
    }
}


