package br.com.oficina.os.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static br.com.oficina.os.infrastructure.messaging.TopologiaRabbit.*;

@Configuration
public class RabbitConfig {

    @Bean
    TopicExchange exchangeComandos() {
        return ExchangeBuilder.topicExchange(EXCHANGE_COMANDOS).durable(true).build();
    }

    @Bean
    TopicExchange exchangeEventos() {
        return ExchangeBuilder.topicExchange(EXCHANGE_EVENTOS).durable(true).build();
    }

    @Bean
    TopicExchange exchangeDlx() {
        return ExchangeBuilder.topicExchange(EXCHANGE_DLX).durable(true).build();
    }

    /**
     * Mensagens que falharem após as tentativas de retry vão para a DLQ (via DLX), em vez
     * de serem descartadas ou ficarem em loop — podem ser inspecionadas e reprocessadas.
     */
    @Bean
    Queue filaEventosSaga() {
        return QueueBuilder.durable(FILA_EVENTOS_SAGA)
                .deadLetterExchange(EXCHANGE_DLX)
                .deadLetterRoutingKey(FILA_EVENTOS_SAGA_DLQ)
                .build();
    }

    @Bean
    Queue filaEventosSagaDlq() {
        return QueueBuilder.durable(FILA_EVENTOS_SAGA_DLQ).build();
    }

    @Bean
    Binding eventosExecution(Queue filaEventosSaga, TopicExchange exchangeEventos) {
        return BindingBuilder.bind(filaEventosSaga).to(exchangeEventos).with("execution.#");
    }

    @Bean
    Binding eventosBilling(Queue filaEventosSaga, TopicExchange exchangeEventos) {
        return BindingBuilder.bind(filaEventosSaga).to(exchangeEventos).with("billing.#");
    }

    @Bean
    Binding dlqEventosSaga(Queue filaEventosSagaDlq, TopicExchange exchangeDlx) {
        return BindingBuilder.bind(filaEventosSagaDlq).to(exchangeDlx).with(FILA_EVENTOS_SAGA_DLQ);
    }
}
