package br.com.oficina.os.infrastructure.messaging;

/**
 * Nomes da topologia RabbitMQ compartilhada pelos microsserviços. Cada serviço declara
 * as exchanges (idempotente) e apenas as próprias filas.
 */
public final class TopologiaRabbit {

    private TopologiaRabbit() {
    }

    public static final String EXCHANGE_COMANDOS = "oficina.saga.comandos";
    public static final String EXCHANGE_EVENTOS = "oficina.saga.eventos";
    public static final String EXCHANGE_DLX = "oficina.saga.dlx";

    /** Fila do orquestrador: recebe todos os eventos de execution e billing. */
    public static final String FILA_EVENTOS_SAGA = "os-service.saga-eventos";
    public static final String FILA_EVENTOS_SAGA_DLQ = FILA_EVENTOS_SAGA + ".dlq";

    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
}
