package br.com.oficina.os.infrastructure.persistence.mensageria;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transactional Outbox: a mensagem é gravada na mesma transação da mudança de estado
 * e publicada no RabbitMQ depois, por {@code OutboxRelay}. Isso evita perder um
 * comando da Saga se o broker estiver indisponível no momento do commit.
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "outbox_mensagens", indexes = @Index(name = "idx_outbox_pendentes", columnList = "publicadaEm,criadaEm"))
public class OutboxMensagem {

    @Id
    private UUID id;

    @Column(nullable = false, length = 60)
    private String exchange;

    @Column(nullable = false, length = 80)
    private String routingKey;

    @Lob
    @Column(nullable = false)
    private String payload;

    private String correlationId;

    @Column(nullable = false)
    private LocalDateTime criadaEm;

    private LocalDateTime publicadaEm;

    private int tentativas;

    public OutboxMensagem(UUID id, String exchange, String routingKey, String payload, String correlationId) {
        this.id = id;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.payload = payload;
        this.correlationId = correlationId;
        this.criadaEm = LocalDateTime.now();
    }

    public void marcarPublicada() {
        this.publicadaEm = LocalDateTime.now();
    }

    public void registrarFalha() {
        this.tentativas++;
    }
}
