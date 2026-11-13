package br.com.oficina.os.infrastructure.persistence.saga;

import br.com.oficina.os.domain.saga.SagaOrdemServico;
import br.com.oficina.os.domain.saga.SagaRepository;
import br.com.oficina.os.domain.saga.StatusSaga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SagaRepositoryJpa extends JpaRepository<SagaOrdemServico, UUID>, SagaRepository {

    @Override
    default SagaOrdemServico salvar(SagaOrdemServico saga) {
        return save(saga);
    }

    @Override
    default Optional<SagaOrdemServico> buscarPorOsId(UUID osId) {
        return findByOsId(osId);
    }

    @Override
    default List<SagaOrdemServico> listarExpiradas(LocalDateTime agora) {
        return findByStatusAndExpiraEmBefore(StatusSaga.EM_ANDAMENTO, agora);
    }

    Optional<SagaOrdemServico> findByOsId(UUID osId);

    List<SagaOrdemServico> findByStatusAndExpiraEmBefore(StatusSaga status, LocalDateTime limite);
}
