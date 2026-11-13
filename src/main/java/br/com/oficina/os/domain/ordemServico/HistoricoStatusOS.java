package br.com.oficina.os.domain.ordemServico;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@NoArgsConstructor
public class HistoricoStatusOS {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOS status;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(length = 500)
    private String observacao;

    public HistoricoStatusOS(StatusOS status, String observacao) {
        this.status = status;
        this.observacao = observacao;
        this.dataHora = LocalDateTime.now();
    }

    public StatusOS getStatus() {
        return status;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getObservacao() {
        return observacao;
    }
}
