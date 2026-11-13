package br.com.oficina.os.support;

import br.com.oficina.os.domain.ordemServico.OrdemServico;
import br.com.oficina.os.domain.ordemServico.OrdemServicoRepository;

import java.util.*;

public class FakeOrdemServicoRepository implements OrdemServicoRepository {
    private final Map<UUID, OrdemServico> dados = new LinkedHashMap<>();

    @Override
    public OrdemServico salvar(OrdemServico os) {
        dados.put(os.getId(), os);
        return os;
    }

    @Override
    public Optional<OrdemServico> buscarPorId(UUID id) {
        return Optional.ofNullable(dados.get(id));
    }

    @Override
    public List<OrdemServico> listarTodos() {
        return new ArrayList<>(dados.values());
    }

    @Override
    public List<OrdemServico> listarPorCliente(UUID clienteId) {
        return dados.values().stream().filter(os -> os.getClienteId().equals(clienteId)).toList();
    }
}
