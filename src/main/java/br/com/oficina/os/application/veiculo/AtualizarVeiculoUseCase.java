package br.com.oficina.os.application.veiculo;

import br.com.oficina.os.api.veiculo.dto.AtualizarVeiculoRequest;
import br.com.oficina.os.api.veiculo.dto.VeiculoResponse;
import br.com.oficina.os.domain.exception.EntidadeNaoEncontradaException;
import br.com.oficina.os.domain.veiculo.Veiculo;
import br.com.oficina.os.domain.veiculo.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AtualizarVeiculoUseCase {
    private final VeiculoRepository repository;

    public VeiculoResponse executar(UUID id, AtualizarVeiculoRequest request) {
        Veiculo veiculo = repository.buscarPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Veículo não encontrado"));

        veiculo.atualizar(request.marca(), request.modelo(), request.ano(), request.cor());

        repository.salvar(veiculo);

        return VeiculoMapper.toResponse(veiculo);
    }
}
