package ar.edu.uade.logistica.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class IoTSimulator {
    private IoTSimulator() { }

    public static void main(String[] args) throws Exception {
        String sensorId = required("SENSOR_ID");
        String token = required("API_TOKEN");
        String baseUrl = System.getenv().getOrDefault("API_BASE_URL", "http://localhost:8080");
        long intervalMillis = Long.parseLong(System.getenv().getOrDefault("SIM_INTERVAL_MS", "3000"));
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        HttpClient client = HttpClient.newHttpClient();

        while (!Thread.currentThread().isInterrupted()) {
            Map<String, Object> reading = Map.of(
                    "idLectura", UUID.randomUUID(),
                    "idSensor", UUID.fromString(sensorId),
                    "timestamp", Instant.now(),
                    "temperatura", ThreadLocalRandom.current().nextDouble(2, 9),
                    "humedad", ThreadLocalRandom.current().nextDouble(35, 70),
                    "vibracion", ThreadLocalRandom.current().nextDouble(0, 1.5),
                    "bateria", ThreadLocalRandom.current().nextDouble(35, 100));
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/iot/lecturas"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(reading)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.printf("%s lectura=%s estado=%d%n", Instant.now(), reading.get("idLectura"), response.statusCode());
            Thread.sleep(intervalMillis);
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Falta la variable " + name);
        return value;
    }
}
