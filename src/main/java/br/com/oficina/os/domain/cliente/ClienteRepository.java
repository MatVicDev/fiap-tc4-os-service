package br.com.oficina.os.domain.cliente;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository {

    Cliente salvar(Cliente cliente);

    Optional<Cliente> buscarPorId(UUID id);

    Optional<Cliente> buscarPorCpf(Cpf cpf);

    List<Cliente> listarTodos();

    boolean existePorCpf(Cpf cpf);

    void excluir(UUID id);
}
