package br.com.oficina.os.support;

import br.com.oficina.os.domain.saga.SagaOrdemServico;
import br.com.oficina.os.domain.saga.SagaRepository;

import java.time.LocalDateTime;
import java.util.*;

public class FakeSagaRepository implements SagaRepository {
    private final Map<UUID, SagaOrdemServico> porOs = new LinkedHashMap<>();

    @Override
    public SagaOrdemServico salvar(SagaOrdemServico saga) {
        porOs.put(saga.getOsId(), saga);
        return saga;
    }

    @Override
    public Optional<SagaOrdemServico> buscarPorOsId(UUID osId) {
        return Optional.ofNullable(porOs.get(osId));
    }

    @Override
    public List<SagaOrdemServico> listarExpiradas(LocalDateTime agora) {
        return porOs.values().stream().filter(s -> s.isExpirada(agora)).toList();
    }
}
