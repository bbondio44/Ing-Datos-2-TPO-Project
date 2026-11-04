package ar.edu.uade.logistica.desktop;

import ar.edu.uade.logistica.contracts.auth.LoginResponse;
import ar.edu.uade.logistica.desktop.http.ApiClient;

public final class AppContext {
    private static final AppContext INSTANCE = new AppContext();
    private final ApiClient apiClient;
    private LoginResponse session;

    private AppContext() {
        String baseUrl = System.getenv().getOrDefault("API_BASE_URL", "http://localhost:8080");
        this.apiClient = new ApiClient(baseUrl);
    }

    public static AppContext get() { return INSTANCE; }
    public ApiClient api() { return apiClient; }
    public LoginResponse session() { return session; }

    public void setSession(LoginResponse session) {
        this.session = session;
        this.apiClient.setToken(session == null ? null : session.token());
    }
}

