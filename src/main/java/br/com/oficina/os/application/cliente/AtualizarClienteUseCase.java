package br.com.oficina.os.application.cliente;

import br.com.oficina.os.api.cliente.dto.AtualizarClienteRequest;
import br.com.oficina.os.api.cliente.dto.ClienteResponse;
import br.com.oficina.os.domain.cliente.Cliente;
import br.com.oficina.os.domain.cliente.ClienteRepository;
import br.com.oficina.os.domain.exception.EntidadeNaoEncontradaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AtualizarClienteUseCase {
    private final ClienteRepository repository;

    public ClienteResponse executar(UUID id, AtualizarClienteRequest request) {
        Cliente cliente = repository.buscarPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Cliente não encontrado"));

        cliente.atualizarContatos(request.telefone(), request.email());

        return ClienteMapper.toResponse(repository.salvar(cliente));
    }
}
