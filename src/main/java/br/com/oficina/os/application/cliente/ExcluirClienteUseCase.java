package br.com.oficina.os.application.cliente;

import br.com.oficina.os.domain.cliente.ClienteRepository;
import br.com.oficina.os.domain.exception.EntidadeNaoEncontradaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExcluirClienteUseCase {
    private final ClienteRepository repository;

    public void executar(UUID id) {
        repository.buscarPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Cliente não encontrado"));

        repository.excluir(id);
    }
}
