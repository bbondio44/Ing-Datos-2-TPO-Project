package ar.edu.uade.logistica.security;

import ar.edu.uade.logistica.contracts.ApiError;
import ar.edu.uade.logistica.observability.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    public JsonSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        write(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                "AUTENTICACION_REQUERIDA", "La sesión no existe, venció o fue revocada");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        write(request, response, HttpServletResponse.SC_FORBIDDEN,
                "ACCESO_DENEGADO", "No tiene permiso para realizar esta acción");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, int status,
                       String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Object correlation = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        objectMapper.writeValue(response.getOutputStream(), new ApiError(
                Instant.now(), status, code, message,
                correlation == null ? "" : correlation.toString(), Map.of()));
    }
}
