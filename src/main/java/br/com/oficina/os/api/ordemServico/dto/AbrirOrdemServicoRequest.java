package br.com.oficina.os.api.ordemServico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AbrirOrdemServicoRequest(
        @NotBlank String cpfCliente,
        @NotBlank String placaVeiculo,
        @Size(max = 1000) String sintomaRelatado
) {
}
