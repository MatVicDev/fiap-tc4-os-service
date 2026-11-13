package br.com.oficina.os.api.veiculo.dto;

import jakarta.validation.constraints.NotBlank;

public record CadastrarVeiculoRequest(

        @NotBlank
        String marca,

        @NotBlank
        String modelo,

        Integer ano,
        String cor,

        @NotBlank
        String placa,

        @NotBlank
        String cpfProprietario
) {}
