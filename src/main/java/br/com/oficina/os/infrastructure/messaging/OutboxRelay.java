package br.com.oficina.os.infrastructure.messaging;

import br.com.oficina.os.infrastructure.persistence.mensageria.OutboxMensagem;
import br.com.oficina.os.infrastructure.persistence.mensageria.OutboxMensagemJpa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

/**
 * Publica no RabbitMQ as mensagens pendentes do outbox, aguardando o publisher confirm
 * do broker antes de marcá-las como publicadas. Em caso de falha a mensagem fica
 * pendente e é retentada no próximo ciclo (entrega at-least-once; consumidores são
 * idempotentes pelo id da mensagem).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {
    private static final int LOTE = 50;

    private final OutboxMensagemJpa outbox;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelayString = "${outbox.relay.intervalo-ms:500}", initialDelayString = "${outbox.relay.intervalo-ms:500}")
    @Transactional
    public void publicarPendentes() {
        for (OutboxMensagem pendente : outbox.findByPublicadaEmIsNullOrderByCriadaEmAsc(Limit.of(LOTE))) {
            try {
                rabbitTemplate.invoke(ops -> {
                    ops.send(pendente.getExchange(), pendente.getRoutingKey(), paraMensagemAmqp(pendente));
                    ops.waitForConfirmsOrDie(5_000);
                    return null;
                });
                pendente.marcarPublicada();
                log.info("Mensagem {} publicada em {} com routing key {}", pendente.getId(), pendente.getExchange(), pendente.getRoutingKey());
            } catch (RuntimeException e) {
                pendente.registrarFalha();
                log.warn("Falha ao publicar mensagem {} (tentativa {}): {}", pendente.getId(), pendente.getTentativas(), e.getMessage());
                break; // preserva a ordem: não publica as seguintes antes desta
            }
        }
    }

    static Message paraMensagemAmqp(OutboxMensagem pendente) {
        return MessageBuilder.withBody(pendente.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(pendente.getId().toString())
                .setType(pendente.getRoutingKey())
                .setHeader(TopologiaRabbit.HEADER_CORRELATION_ID, pendente.getCorrelationId())
                .setDeliveryMode(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT)
                .build();
    }
}
