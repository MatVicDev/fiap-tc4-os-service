package br.com.oficina.os.domain.saga;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SagaRepository {

    SagaOrdemServico salvar(SagaOrdemServico saga);

    Optional<SagaOrdemServico> buscarPorOsId(UUID osId);

    List<SagaOrdemServico> listarExpiradas(LocalDateTime agora);
}
