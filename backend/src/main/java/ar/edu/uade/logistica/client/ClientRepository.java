package ar.edu.uade.logistica.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClientRepository extends JpaRepository<ClientEntity, UUID> {
    Page<ClientEntity> findByBusinessNameContainingIgnoreCaseOrCuitContaining(String businessName, String cuit, Pageable pageable);
}

