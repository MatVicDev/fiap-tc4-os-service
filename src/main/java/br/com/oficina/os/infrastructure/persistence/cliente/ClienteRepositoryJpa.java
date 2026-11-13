package br.com.oficina.os.infrastructure.persistence.cliente;

import br.com.oficina.os.domain.cliente.Cliente;
import br.com.oficina.os.domain.cliente.ClienteRepository;
import br.com.oficina.os.domain.cliente.Cpf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClienteRepositoryJpa extends JpaRepository<Cliente, UUID>, ClienteRepository {

    @Override
    default Cliente salvar(Cliente cliente) {
        return save(cliente);
    }

    @Override
    default Optional<Cliente> buscarPorId(UUID id) {
        return findById(id);
    }

    @Override
    default Optional<Cliente> buscarPorCpf(Cpf cpf) {
        return findByCpf_Numero(cpf.getNumero());
    }

    @Override
    default void excluir(UUID id) {
        deleteById(id);
    }

    @Override
    default List<Cliente> listarTodos() {
        return findAll();
    }

    @Override
    default boolean existePorCpf(Cpf cpf) {
        return existsByCpf_Numero(cpf.getNumero());
    }

    boolean existsByCpf_Numero(String numero);

    Optional<Cliente> findByCpf_Numero(String numero);
}
