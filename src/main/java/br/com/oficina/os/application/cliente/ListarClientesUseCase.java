package br.com.oficina.os.application.cliente;

import br.com.oficina.os.api.cliente.dto.ClienteResponse;
import br.com.oficina.os.domain.cliente.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListarClientesUseCase {
    private final ClienteRepository repository;

    public List<ClienteResponse> executar() {
        return repository.listarTodos().stream()
                .map(ClienteMapper::toResponse)
                .toList();
    }
}
