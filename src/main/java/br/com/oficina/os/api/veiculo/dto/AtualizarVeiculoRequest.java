package br.com.oficina.os.api.veiculo.dto;

public record AtualizarVeiculoRequest(
        String marca,
        String modelo,
        Integer ano,
        String cor
) {}
