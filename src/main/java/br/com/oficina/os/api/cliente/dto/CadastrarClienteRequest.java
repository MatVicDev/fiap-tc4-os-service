package br.com.oficina.os.api.cliente.dto;

import jakarta.validation.constraints.NotBlank;

public record CadastrarClienteRequest(

        @NotBlank
        String nome,

        @NotBlank
        String cpf,

        @NotBlank
        String telefone,

        String email
) {}
