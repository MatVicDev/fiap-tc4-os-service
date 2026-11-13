package br.com.oficina.os.domain.saga;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Linha do log de execução da Saga — tudo que o orquestrador recebeu ou enviou,
 * exposto em GET /ordens-servico/{id}/saga para rastrear o fluxo distribuído.
 */
@Embeddable
@NoArgsConstructor
public class EventoSaga {

    @Column(nullable = false, length = 60)
    private String tipo;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    public EventoSaga(String tipo, String descricao) {
        this.tipo = tipo;
        this.descricao = descricao;
        this.dataHora = LocalDateTime.now();
    }

    public String getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
}
