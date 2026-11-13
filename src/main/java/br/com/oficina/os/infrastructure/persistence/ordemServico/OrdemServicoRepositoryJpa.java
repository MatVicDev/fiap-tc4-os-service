package br.com.oficina.os.infrastructure.persistence.ordemServico;

import br.com.oficina.os.domain.ordemServico.OrdemServico;
import br.com.oficina.os.domain.ordemServico.OrdemServicoRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrdemServicoRepositoryJpa extends JpaRepository<OrdemServico, UUID>, OrdemServicoRepository {

    @Override
    default OrdemServico salvar(OrdemServico os) {
        return save(os);
    }

    @Override
    default Optional<OrdemServico> buscarPorId(UUID id) {
        return findById(id);
    }

    @Override
    default List<OrdemServico> listarTodos() {
        return findAll();
    }

    @Override
    default List<OrdemServico> listarPorCliente(UUID clienteId) {
        return findByClienteId(clienteId);
    }

    List<OrdemServico> findByClienteId(UUID clienteId);
}
