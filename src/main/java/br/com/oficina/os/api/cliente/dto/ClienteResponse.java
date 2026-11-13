package br.com.oficina.os.api.cliente.dto;

import br.com.oficina.os.domain.cliente.StatusCliente;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClienteResponse(
        UUID id,
        String nome,
        String cpf,
        String telefone,
        String email,
        StatusCliente status,
        LocalDateTime dataCadastro
) {}
