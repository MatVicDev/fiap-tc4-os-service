package br.com.oficina.os.application.veiculo;

import br.com.oficina.os.domain.exception.EntidadeNaoEncontradaException;
import br.com.oficina.os.domain.veiculo.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExcluirVeiculoUseCase {
    private final VeiculoRepository repository;

    public void executar(UUID id) {
        repository.buscarPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Veículo não encontrado"));

        repository.excluir(id);
    }
}
