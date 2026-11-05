package ar.edu.uade.logistica.common;

import ar.edu.uade.logistica.contracts.ApiError;
import ar.edu.uade.logistica.observability.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return response(HttpStatus.BAD_REQUEST, "VALIDACION", "Revise los datos ingresados", fields, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> malformedJson(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "JSON_INVALIDO",
                "El cuerpo de la solicitud no contiene JSON UTF-8 válido", Map.of(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ApiError> credentials(BadCredentialsException exception, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> denied(AccessDeniedException exception, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", "No tiene permiso para realizar esta acción", Map.of(), request);
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> routeNotFound(NoResourceFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "RUTA_NO_ENCONTRADA",
                "La ruta solicitada no existe", Map.of(), request);
    }

    @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class,
            UncategorizedSQLException.class})
    ResponseEntity<ApiError> conflict(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "CONFLICTO_DATOS",
                "La operación entra en conflicto con un dato existente o una regla de integridad", Map.of(), request);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> invalidParameter(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO",
                "Falta un parámetro o tiene un formato inválido", Map.of(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ApiError> badRequest(BadRequestException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    ResponseEntity<ApiError> unavailable(ServiceUnavailableException exception, HttpServletRequest request) {
        LOGGER.warn("{} correlationId={}", exception.getMessage(),
                request.getAttribute(CorrelationIdFilter.ATTRIBUTE), exception);
        return response(HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_NO_DISPONIBLE",
                exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Error no controlado. correlationId={}",
                request.getAttribute(CorrelationIdFilter.ATTRIBUTE), exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                "No se pudo completar la operación", Map.of(), request);
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message,
                                               Map<String, String> fields, HttpServletRequest request) {
        String correlationId = String.valueOf(request.getAttribute(CorrelationIdFilter.ATTRIBUTE));
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(), status.value(), code, message, correlationId, fields));
    }
}

