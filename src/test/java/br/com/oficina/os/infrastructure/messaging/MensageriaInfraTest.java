package br.com.oficina.os.infrastructure.messaging;

import br.com.oficina.os.application.saga.OrquestradorSagaOS;
import br.com.oficina.os.domain.mensageria.Mensagem;
import br.com.oficina.os.infrastructure.persistence.mensageria.MensagemProcessada;
import br.com.oficina.os.infrastructure.persistence.mensageria.MensagemProcessadaJpa;
import br.com.oficina.os.infrastructure.persistence.mensageria.MensagensProcessadasAdapter;
import br.com.oficina.os.infrastructure.persistence.mensageria.OutboxMensagem;
import br.com.oficina.os.infrastructure.persistence.mensageria.OutboxMensagemJpa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Limit;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Testes da infraestrutura de mensageria")
class MensageriaInfraTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("OutboxPublicador grava comandos e eventos nas exchanges corretas")
    void outboxPublicador() {
        OutboxMensagemJpa jpa = mock(OutboxMensagemJpa.class);
        OutboxPublicador publicador = new OutboxPublicador(jpa, jsonMapper);
        Mensagem cmd = Mensagem.nova("billing.gerar-orcamento", UUID.randomUUID(), "corr", Map.of("a", "b"));

        publicador.enviarComando(cmd);
        publicador.publicarEvento(Mensagem.nova("os.status-alterado", UUID.randomUUID(), null, null));

        ArgumentCaptor<OutboxMensagem> captor = ArgumentCaptor.forClass(OutboxMensagem.class);
        verify(jpa, times(2)).save(captor.capture());
        OutboxMensagem gravada = captor.getAllValues().get(0);
        assertThat(gravada.getExchange()).isEqualTo(TopologiaRabbit.EXCHANGE_COMANDOS);
        assertThat(gravada.getRoutingKey()).isEqualTo("billing.gerar-orcamento");
        assertThat(gravada.getPayload()).contains("\"a\":\"b\"");
        assertThat(gravada.getCorrelationId()).isEqualTo("corr");
        assertThat(captor.getAllValues().get(1).getExchange()).isEqualTo(TopologiaRabbit.EXCHANGE_EVENTOS);
    }

    @Test
    @DisplayName("OutboxRelay publica pendentes e marca como publicadas")
    void relayPublica() {
        OutboxMensagemJpa jpa = mock(OutboxMensagemJpa.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        OutboxMensagem m1 = novaPendente();
        OutboxMensagem m2 = novaPendente();
        when(jpa.findByPublicadaEmIsNullOrderByCriadaEmAsc(any(Limit.class))).thenReturn(List.of(m1, m2));

        new OutboxRelay(jpa, rabbit).publicarPendentes();

        verify(rabbit, times(2)).invoke(any());
        assertThat(m1.getPublicadaEm()).isNotNull();
        assertThat(m2.getPublicadaEm()).isNotNull();
    }

    @Test
    @DisplayName("OutboxRelay interrompe o lote na primeira falha, preservando a ordem")
    void relayFalha() {
        OutboxMensagemJpa jpa = mock(OutboxMensagemJpa.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        OutboxMensagem m1 = novaPendente();
        OutboxMensagem m2 = novaPendente();
        when(jpa.findByPublicadaEmIsNullOrderByCriadaEmAsc(any(Limit.class))).thenReturn(List.of(m1, m2));
        when(rabbit.invoke(any())).thenThrow(new AmqpException("broker fora"));

        new OutboxRelay(jpa, rabbit).publicarPendentes();

        verify(rabbit, times(1)).invoke(any());
        assertThat(m1.getPublicadaEm()).isNull();
        assertThat(m1.getTentativas()).isEqualTo(1);
        assertThat(m2.getTentativas()).isZero();
    }

    @Test
    @DisplayName("Mensagem AMQP carrega id, tipo, correlation-id e payload JSON")
    void conversaoAmqp() {
        OutboxMensagem pendente = novaPendente();
        Message msg = OutboxRelay.paraMensagemAmqp(pendente);
        assertThat(msg.getMessageProperties().getMessageId()).isEqualTo(pendente.getId().toString());
        assertThat(msg.getMessageProperties().getType()).isEqualTo("execution.iniciar-diagnostico");
        assertThat(msg.getMessageProperties().getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
        assertThat((String) msg.getMessageProperties().getHeader(TopologiaRabbit.HEADER_CORRELATION_ID)).isEqualTo("corr-1");
        assertThat(new String(msg.getBody(), StandardCharsets.UTF_8)).isEqualTo("{}");
    }

    @Test
    @DisplayName("Listener desserializa o envelope e delega ao orquestrador")
    void listener() {
        OrquestradorSagaOS orquestrador = mock(OrquestradorSagaOS.class);
        SagaEventosListener listener = new SagaEventosListener(orquestrador, jsonMapper);
        Mensagem evento = Mensagem.nova("execution.diagnostico-iniciado", UUID.randomUUID(), "corr-x", Map.of());
        Message amqp = new Message(jsonMapper.writeValueAsBytes(evento), new MessageProperties());

        listener.receber(amqp);

        ArgumentCaptor<Mensagem> captor = ArgumentCaptor.forClass(Mensagem.class);
        verify(orquestrador).processar(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(evento.id());
        assertThat(captor.getValue().tipo()).isEqualTo("execution.diagnostico-iniciado");
    }

    @Test
    @DisplayName("Scheduler de prazos aciona o orquestrador")
    void scheduler() {
        OrquestradorSagaOS orquestrador = mock(OrquestradorSagaOS.class);
        when(orquestrador.expirarSagasVencidas()).thenReturn(2, 0);
        SagaTimeoutScheduler scheduler = new SagaTimeoutScheduler(orquestrador);
        scheduler.verificarPrazos();
        scheduler.verificarPrazos();
        verify(orquestrador, times(2)).expirarSagasVencidas();
    }

    @Test
    @DisplayName("Inbox registra mensagem nova e rejeita repetida")
    void inbox() {
        MensagemProcessadaJpa jpa = mock(MensagemProcessadaJpa.class);
        UUID id = UUID.randomUUID();
        when(jpa.existsById(id)).thenReturn(false, true);
        MensagensProcessadasAdapter adapter = new MensagensProcessadasAdapter(jpa);

        assertThat(adapter.registrarSeNova(id, "x")).isTrue();
        assertThat(adapter.registrarSeNova(id, "x")).isFalse();
        verify(jpa, times(1)).save(any(MensagemProcessada.class));
    }

    private OutboxMensagem novaPendente() {
        return new OutboxMensagem(UUID.randomUUID(), TopologiaRabbit.EXCHANGE_COMANDOS,
                "execution.iniciar-diagnostico", "{}", "corr-1");
    }
}
