package br.com.oficina.os.infrastructure.messaging;

import br.com.oficina.os.domain.mensageria.Mensagem;
import br.com.oficina.os.domain.mensageria.PublicadorMensagens;
import br.com.oficina.os.infrastructure.persistence.mensageria.OutboxMensagem;
import br.com.oficina.os.infrastructure.persistence.mensageria.OutboxMensagemJpa;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Grava a mensagem no outbox — exige transação ativa (MANDATORY) para garantir que o
 * comando nunca seja registrado sem a mudança de estado que o originou.
 */
@Component
@RequiredArgsConstructor
public class OutboxPublicador implements PublicadorMensagens {
    private final OutboxMensagemJpa outbox;
    private final JsonMapper jsonMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enviarComando(Mensagem comando) {
        gravar(TopologiaRabbit.EXCHANGE_COMANDOS, comando);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publicarEvento(Mensagem evento) {
        gravar(TopologiaRabbit.EXCHANGE_EVENTOS, evento);
    }

    private void gravar(String exchange, Mensagem mensagem) {
        outbox.save(new OutboxMensagem(mensagem.id(), exchange, mensagem.tipo(),
                jsonMapper.writeValueAsString(mensagem), mensagem.correlationId()));
    }
}
