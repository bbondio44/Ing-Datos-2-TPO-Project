package ar.edu.uade.logistica.desktop.http;

import ar.edu.uade.logistica.contracts.ApiError;
import ar.edu.uade.logistica.contracts.PageResponse;
import ar.edu.uade.logistica.contracts.auth.LoginRequest;
import ar.edu.uade.logistica.contracts.auth.LoginResponse;
import ar.edu.uade.logistica.contracts.auth.SessionResponse;
import ar.edu.uade.logistica.contracts.client.ClientRequest;
import ar.edu.uade.logistica.contracts.client.ClientResponse;
import ar.edu.uade.logistica.contracts.organization.OrganizationRequest;
import ar.edu.uade.logistica.contracts.organization.OrganizationResponse;
import ar.edu.uade.logistica.contracts.master.LocationRequest;
import ar.edu.uade.logistica.contracts.master.LocationResponse;
import ar.edu.uade.logistica.contracts.master.VehicleRequest;
import ar.edu.uade.logistica.contracts.master.VehicleResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ApiClient {
    private final String baseUrl;
    private final HttpClient client;
    private final ObjectMapper mapper;
    private volatile String token;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public void setToken(String token) { this.token = token; }

    public CompletableFuture<LoginResponse> login(String email, String password) {
        return send("POST", "/api/v1/auth/login", new LoginRequest(email, password), LoginResponse.class, false);
    }

    public CompletableFuture<PageResponse<ClientResponse>> clients(String search) {
        String encoded = URLEncoder.encode(search == null ? "" : search, StandardCharsets.UTF_8);
        return send("GET", "/api/v1/clientes?buscar=" + encoded + "&pagina=0&tamanio=100", null,
                new TypeReference<>() {}, true);
    }

    public CompletableFuture<ClientResponse> createClient(ClientRequest request) {
        return send("POST", "/api/v1/clientes", request, ClientResponse.class, true);
    }

    public CompletableFuture<ClientResponse> updateClient(UUID id, ClientRequest request) {
        return send("PUT", "/api/v1/clientes/" + id, request, ClientResponse.class, true);
    }

    public CompletableFuture<Void> deactivateClient(UUID id) {
        return send("DELETE", "/api/v1/clientes/" + id, null, Void.class, true);
    }

    public CompletableFuture<List<SessionResponse>> sessions() {
        return send("GET", "/api/v1/auth/sessions", null, new TypeReference<>() {}, true);
    }

    public CompletableFuture<Void> revokeSession(UUID id) {
        return send("DELETE", "/api/v1/auth/sessions/" + id, null, Void.class, true);
    }

    public CompletableFuture<PageResponse<OrganizationResponse>> organizations(String resource, String search) {
        String encoded = URLEncoder.encode(search == null ? "" : search, StandardCharsets.UTF_8);
        return send("GET", "/api/v1/" + resource + "?buscar=" + encoded + "&pagina=0&tamanio=100", null,
                new TypeReference<>() {}, true);
    }

    public CompletableFuture<OrganizationResponse> createOrganization(String resource, OrganizationRequest request) {
        return send("POST", "/api/v1/" + resource, request, OrganizationResponse.class, true);
    }

    public CompletableFuture<OrganizationResponse> updateOrganization(
            String resource, UUID id, OrganizationRequest request) {
        return send("PUT", "/api/v1/" + resource + "/" + id, request, OrganizationResponse.class, true);
    }

    public CompletableFuture<Void> deactivateOrganization(String resource, UUID id) {
        return send("DELETE", "/api/v1/" + resource + "/" + id, null, Void.class, true);
    }

    public CompletableFuture<PageResponse<LocationResponse>> locations(String search) {
        String encoded = URLEncoder.encode(search == null ? "" : search, StandardCharsets.UTF_8);
        return send("GET", "/api/v1/ubicaciones?buscar=" + encoded + "&pagina=0&tamanio=100", null,
                new TypeReference<>() {}, true);
    }

    public CompletableFuture<LocationResponse> createLocation(LocationRequest request) {
        return send("POST", "/api/v1/ubicaciones", request, LocationResponse.class, true);
    }

    public CompletableFuture<LocationResponse> updateLocation(UUID id, LocationRequest request) {
        return send("PUT", "/api/v1/ubicaciones/" + id, request, LocationResponse.class, true);
    }

    public CompletableFuture<Void> deleteLocation(UUID id) {
        return send("DELETE", "/api/v1/ubicaciones/" + id, null, Void.class, true);
    }

    public CompletableFuture<PageResponse<VehicleResponse>> vehicles(String search) {
        String encoded = URLEncoder.encode(search == null ? "" : search, StandardCharsets.UTF_8);
        return send("GET", "/api/v1/vehiculos?buscar=" + encoded + "&pagina=0&tamanio=100", null,
                new TypeReference<>() {}, true);
    }

    public CompletableFuture<VehicleResponse> createVehicle(VehicleRequest request) {
        return send("POST", "/api/v1/vehiculos", request, VehicleResponse.class, true);
    }

    public CompletableFuture<VehicleResponse> updateVehicle(UUID id, VehicleRequest request) {
        return send("PUT", "/api/v1/vehiculos/" + id, request, VehicleResponse.class, true);
    }

    public CompletableFuture<Void> deactivateVehicle(UUID id) {
        return send("DELETE", "/api/v1/vehiculos/" + id, null, Void.class, true);
    }

    public CompletableFuture<Void> logout() {
        return send("POST", "/api/v1/auth/logout", null, Void.class, true).thenApply(ignored -> null);
    }

    private <T> CompletableFuture<T> send(String method, String path, Object body, Class<T> responseType, boolean auth) {
        return sendRaw(method, path, body, auth).thenApply(response -> parse(response, responseType));
    }

    private <T> CompletableFuture<T> send(String method, String path, Object body,
                                           TypeReference<T> responseType, boolean auth) {
        return sendRaw(method, path, body, auth).thenApply(response -> parse(response, responseType));
    }

    private CompletableFuture<HttpResponse<String>> sendRaw(String method, String path, Object body, boolean auth) {
        try {
            HttpRequest.BodyPublisher publisher = body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body));
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .method(method, publisher);
            if (auth && token != null) builder.header("Authorization", "Bearer " + token);
            return client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() >= 400) throw error(response);
                        return response;
                    });
        } catch (JsonProcessingException exception) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("No se pudo preparar la solicitud", exception));
        }
    }

    private <T> T parse(HttpResponse<String> response, Class<T> type) {
        if (type == Void.class || response.body() == null || response.body().isBlank()) return null;
        try {
            return mapper.readValue(response.body(), type);
        } catch (JsonProcessingException exception) {
            throw new ApiException(response.statusCode(), "La respuesta del servidor no tiene el formato esperado");
        }
    }

    private <T> T parse(HttpResponse<String> response, TypeReference<T> type) {
        try {
            return mapper.readValue(response.body(), type);
        } catch (JsonProcessingException exception) {
            throw new ApiException(response.statusCode(), "La respuesta del servidor no tiene el formato esperado");
        }
    }

    private ApiException error(HttpResponse<String> response) {
        try {
            ApiError error = mapper.readValue(response.body(), ApiError.class);
            return new ApiException(response.statusCode(), error.mensaje());
        } catch (Exception ignored) {
            return new ApiException(response.statusCode(), "El servidor rechazó la operación (" + response.statusCode() + ")");
        }
    }
}

