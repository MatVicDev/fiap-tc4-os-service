package br.com.oficina.os.application.cliente;

import br.com.oficina.os.api.cliente.dto.ClienteResponse;
import br.com.oficina.os.domain.cliente.Cliente;

public class ClienteMapper {
    public static ClienteResponse toResponse(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNome(),
                cliente.getCpfNumero(),
                cliente.getTelefone(),
                cliente.getEmail(),
                cliente.getStatus(),
                cliente.getDataCadastro());
    }
}
