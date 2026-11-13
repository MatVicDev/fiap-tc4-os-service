package br.com.oficina.os.infrastructure.messaging;

import br.com.oficina.os.application.saga.OrquestradorSagaOS;
import br.com.oficina.os.domain.mensageria.Mensagem;
import br.com.oficina.os.infrastructure.observability.CorrelationIdFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Consome os eventos dos participantes da Saga e os entrega ao orquestrador. Exceções
 * propagam para o container do Spring AMQP, que aplica o retry configurado e, esgotadas
 * as tentativas, rejeita a mensagem para a DLQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaEventosListener {
    private final OrquestradorSagaOS orquestrador;
    private final JsonMapper jsonMapper;

    @RabbitListener(queues = TopologiaRabbit.FILA_EVENTOS_SAGA)
    public void receber(Message message) {
        Mensagem evento = jsonMapper.readValue(message.getBody(), Mensagem.class);
        try {
            if (evento.correlationId() != null) {
                MDC.put(CorrelationIdFilter.MDC_KEY, evento.correlationId());
            }
            MDC.put("osId", String.valueOf(evento.osId()));
            orquestrador.processar(evento);
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_KEY);
            MDC.remove("osId");
        }
    }
}
