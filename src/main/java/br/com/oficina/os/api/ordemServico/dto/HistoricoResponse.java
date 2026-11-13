package br.com.oficina.os.api.ordemServico.dto;

import br.com.oficina.os.domain.ordemServico.StatusOS;

import java.time.LocalDateTime;

public record HistoricoResponse(StatusOS status, LocalDateTime dataHora, String observacao) {
}
