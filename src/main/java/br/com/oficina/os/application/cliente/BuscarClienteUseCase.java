package br.com.oficina.os.application.cliente;

import br.com.oficina.os.api.cliente.dto.ClienteResponse;
import br.com.oficina.os.domain.cliente.Cliente;
import br.com.oficina.os.domain.cliente.ClienteRepository;
import br.com.oficina.os.domain.cliente.Cpf;
import br.com.oficina.os.domain.exception.EntidadeNaoEncontradaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BuscarClienteUseCase {
    private final ClienteRepository repository;

    public ClienteResponse executar(UUID id) {
        Cliente cliente = repository.buscarPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Cliente não identificado"));

        return ClienteMapper.toResponse(cliente);
    }

    public ClienteResponse executar(Cpf cpf) {
        Cliente cliente = repository.buscarPorCpf(cpf)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Cliente não identificado"));

        return ClienteMapper.toResponse(cliente);
    }
}
