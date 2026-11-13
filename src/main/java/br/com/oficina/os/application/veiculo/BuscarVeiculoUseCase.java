package br.com.oficina.os.application.veiculo;

import br.com.oficina.os.api.veiculo.dto.VeiculoResponse;
import br.com.oficina.os.domain.exception.EntidadeNaoEncontradaException;
import br.com.oficina.os.domain.veiculo.Placa;
import br.com.oficina.os.domain.veiculo.Veiculo;
import br.com.oficina.os.domain.veiculo.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BuscarVeiculoUseCase {
    private final VeiculoRepository repository;

    public VeiculoResponse executar(UUID id) {
        Veiculo veiculo = repository.buscarPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Veículo não encontrado"));

        return VeiculoMapper.toResponse(veiculo);
    }

    public VeiculoResponse executar(String placa) {
        Veiculo veiculo = repository.buscarPorPlaca(new Placa(placa))
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Veículo não encontrado"));

        return VeiculoMapper.toResponse(veiculo);
    }
}
