package ar.edu.uade.logistica.client;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cliente", schema = "tp_logistica")
public class ClientEntity {
    @Id
    @Column(name = "id_cliente", nullable = false)
    private UUID id;
    @Column(name = "razon_social", nullable = false, length = 160)
    private String businessName;
    @Column(length = 11)
    private String cuit;
    @Column(length = 254)
    private String email;
    @Column(name = "telefono", length = 40)
    private String phone;
    @Column(name = "direccion", length = 250)
    private String address;
    @Column(name = "pais", nullable = false, length = 80)
    private String country;
    @Column(name = "estado", nullable = false, length = 20)
    private String state;
    @Column(name = "fecha_alta", nullable = false)
    private Instant createdAt;

    protected ClientEntity() {
    }

    public ClientEntity(String businessName, String cuit, String email, String phone, String address, String country) {
        this.businessName = businessName;
        this.cuit = blankToNull(cuit);
        this.email = blankToNull(email);
        this.phone = blankToNull(phone);
        this.address = blankToNull(address);
        this.country = country;
        this.state = "ACTIVO";
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public void update(String businessName, String cuit, String email, String phone, String address, String country) {
        this.businessName = businessName;
        this.cuit = blankToNull(cuit);
        this.email = blankToNull(email);
        this.phone = blankToNull(phone);
        this.address = blankToNull(address);
        this.country = country;
    }

    public void deactivate() { this.state = "INACTIVO"; }
    public void activate() { this.state = "ACTIVO"; }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.strip(); }
    public UUID getId() { return id; }
    public String getBusinessName() { return businessName; }
    public String getCuit() { return cuit; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getCountry() { return country; }
    public String getState() { return state; }
    public Instant getCreatedAt() { return createdAt; }
}

