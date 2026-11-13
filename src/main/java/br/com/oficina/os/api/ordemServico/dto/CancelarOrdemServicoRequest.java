package br.com.oficina.os.api.ordemServico.dto;

import jakarta.validation.constraints.Size;

public record CancelarOrdemServicoRequest(@Size(max = 300) String motivo) {
}
