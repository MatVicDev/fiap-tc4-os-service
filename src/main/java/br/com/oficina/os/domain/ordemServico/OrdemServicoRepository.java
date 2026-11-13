package br.com.oficina.os.domain.ordemServico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdemServicoRepository {

    OrdemServico salvar(OrdemServico os);

    Optional<OrdemServico> buscarPorId(UUID id);

    List<OrdemServico> listarTodos();

    List<OrdemServico> listarPorCliente(UUID clienteId);
}
