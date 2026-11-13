package br.com.oficina.os.domain.mensageria;

import java.util.UUID;

/**
 * Inbox para idempotência: o RabbitMQ garante entrega "at-least-once", então um
 * mesmo evento pode chegar mais de uma vez e precisa ser ignorado na repetição.
 */
public interface MensagensProcessadas {

    /** @return true se a mensagem ainda não havia sido processada (e agora fica registrada). */
    boolean registrarSeNova(UUID mensagemId, String tipo);
}
