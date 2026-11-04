package ar.edu.uade.logistica.client;

import ar.edu.uade.logistica.contracts.client.ClientRequest;
import ar.edu.uade.logistica.observability.DatabaseAuditContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {
    @Mock ClientRepository repository;
    @Mock DatabaseAuditContext auditContext;

    @Test
    void createsARealClientEntityAndAppliesDatabaseAuditContext() {
        ClientService service = new ClientService(repository, auditContext);
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            ClientEntity entity = invocation.getArgument(0);
            entity.prePersist();
            return entity;
        });

        var response = service.create(new ClientRequest(
                "Transportes Sur", "30710123451", "ops@sur.test", null, null, "Argentina"));

        verify(auditContext).applyToCurrentTransaction();
        ArgumentCaptor<ClientEntity> captor = ArgumentCaptor.forClass(ClientEntity.class);
        verify(repository).save(captor.capture());
        assertThat(response.id()).isNotNull();
        assertThat(captor.getValue().getBusinessName()).isEqualTo("Transportes Sur");
        assertThat(response.state()).isEqualTo("ACTIVO");
    }
}
