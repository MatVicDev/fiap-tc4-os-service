package br.com.oficina.os.domain.veiculo;

import br.com.oficina.os.domain.cliente.Cpf;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VeiculoRepository {

    Veiculo salvar(Veiculo veiculo);

    Optional<Veiculo> buscarPorPlaca(Placa placa);

    Optional<Veiculo> buscarPorId(UUID id);

    List<Veiculo> listarTodos();

    List<Veiculo> listarPorCpfProprietario(Cpf cpf);

    boolean existePorPlaca(Placa placa);

    void excluir(UUID id);
}