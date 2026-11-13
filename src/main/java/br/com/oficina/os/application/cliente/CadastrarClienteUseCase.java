package br.com.oficina.os.application.cliente;

import br.com.oficina.os.api.cliente.dto.CadastrarClienteRequest;
import br.com.oficina.os.api.cliente.dto.ClienteResponse;
import br.com.oficina.os.domain.cliente.Cliente;
import br.com.oficina.os.domain.cliente.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CadastrarClienteUseCase {
    private final ClienteRepository repository;

    public ClienteResponse executar(CadastrarClienteRequest request) {
        Cliente cliente = new Cliente(
                request.nome(),
                request.cpf(),
                request.telefone(),
                request.email());

        repository.salvar(cliente);

        return ClienteMapper.toResponse(cliente);
    }
}
