package br.com.oficina.os.api.cliente.dto;

public record AtualizarClienteRequest(
        String telefone,
        String email
) {}
