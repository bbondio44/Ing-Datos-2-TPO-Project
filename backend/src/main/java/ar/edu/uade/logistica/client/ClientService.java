package ar.edu.uade.logistica.client;

import ar.edu.uade.logistica.common.NotFoundException;
import ar.edu.uade.logistica.contracts.PageResponse;
import ar.edu.uade.logistica.contracts.client.ClientRequest;
import ar.edu.uade.logistica.contracts.client.ClientResponse;
import ar.edu.uade.logistica.observability.DatabaseAuditContext;
import ar.edu.uade.logistica.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ClientService {
    private final ClientRepository repository;
    private final DatabaseAuditContext auditContext;

    public ClientService(ClientRepository repository, DatabaseAuditContext auditContext) {
        this.repository = repository;
        this.auditContext = auditContext;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientResponse> list(String search, int page, int size, AuthenticatedUser user) {
        if (!user.permissions().contains("CLIENTES_LEER_TODOS")) {
            if (user.clientId() == null) throw new AccessDeniedException("No tiene permiso para consultar clientes");
            ClientResponse own = get(user.clientId(), user);
            return new PageResponse<>(java.util.List.of(own), 0, 1, 1, 1);
        }
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("businessName").ascending());
        Page<ClientEntity> result = search == null || search.isBlank()
                ? repository.findAll(pageable)
                : repository.findByBusinessNameContainingIgnoreCaseOrCuitContaining(search.strip(), search.strip(), pageable);
        return new PageResponse<>(result.map(this::map).getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ClientResponse get(UUID id, AuthenticatedUser user) {
        authorizeRead(id, user);
        return map(repository.findById(id).orElseThrow(() -> new NotFoundException("El cliente no existe")));
    }

    @Transactional
    public ClientResponse create(ClientRequest request) {
        auditContext.applyToCurrentTransaction();
        return map(repository.save(new ClientEntity(request.businessName().strip(), request.cuit(), request.email(),
                request.phone(), request.address(), request.country().strip())));
    }

    @Transactional
    public ClientResponse update(UUID id, ClientRequest request) {
        auditContext.applyToCurrentTransaction();
        ClientEntity entity = repository.findById(id).orElseThrow(() -> new NotFoundException("El cliente no existe"));
        entity.update(request.businessName().strip(), request.cuit(), request.email(), request.phone(),
                request.address(), request.country().strip());
        return map(entity);
    }

    @Transactional
    public void deactivate(UUID id) {
        auditContext.applyToCurrentTransaction();
        ClientEntity entity = repository.findById(id).orElseThrow(() -> new NotFoundException("El cliente no existe"));
        entity.deactivate();
    }

    @Transactional
    public void activate(UUID id) {
        auditContext.applyToCurrentTransaction();
        ClientEntity entity = repository.findById(id).orElseThrow(() -> new NotFoundException("El cliente no existe"));
        entity.activate();
    }

    private void authorizeRead(UUID id, AuthenticatedUser user) {
        if (!user.permissions().contains("CLIENTES_LEER_TODOS") && !id.equals(user.clientId())) {
            throw new NotFoundException("El cliente no existe");
        }
    }

    private ClientResponse map(ClientEntity entity) {
        return new ClientResponse(entity.getId(), entity.getBusinessName(), entity.getCuit(), entity.getEmail(),
                entity.getPhone(), entity.getAddress(), entity.getCountry(), entity.getState(), entity.getCreatedAt());
    }
}

