package br.com.oficina.os.api.ordemServico.dto;

import br.com.oficina.os.domain.saga.Compensacao;
import br.com.oficina.os.domain.saga.EtapaSaga;
import br.com.oficina.os.domain.saga.StatusSaga;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record SagaResponse(
        UUID id,
        UUID osId,
        EtapaSaga etapa,
        StatusSaga status,
        UUID orcamentoId,
        UUID pagamentoId,
        String motivoFalha,
        Set<Compensacao> compensacoesPendentes,
        LocalDateTime expiraEm,
        List<Evento> eventos
) {
    public record Evento(String tipo, String descricao, LocalDateTime dataHora) {
    }
}
