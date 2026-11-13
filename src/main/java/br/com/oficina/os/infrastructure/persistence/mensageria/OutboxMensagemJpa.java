package br.com.oficina.os.infrastructure.persistence.mensageria;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxMensagemJpa extends JpaRepository<OutboxMensagem, UUID> {

    List<OutboxMensagem> findByPublicadaEmIsNullOrderByCriadaEmAsc(Limit limit);
}
