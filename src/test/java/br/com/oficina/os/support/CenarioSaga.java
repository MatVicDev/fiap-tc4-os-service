package br.com.oficina.os.support;

import br.com.oficina.os.application.ordemServico.NotificarStatusOrdemServicoService;
import br.com.oficina.os.application.saga.OrquestradorSagaOS;
import br.com.oficina.os.application.saga.PrazosSaga;
import br.com.oficina.os.domain.cliente.Cliente;
import br.com.oficina.os.domain.cliente.ClienteRepository;
import br.com.oficina.os.domain.mensageria.Mensagem;
import br.com.oficina.os.domain.notificacao.NotificacaoPort;
import br.com.oficina.os.domain.ordemServico.OrdemServico;
import br.com.oficina.os.domain.saga.SagaOrdemServico;
import br.com.oficina.os.domain.veiculo.Veiculo;

import java.time.*;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Monta o orquestrador com fakes em memória, um relógio controlável e helpers para
 * simular os eventos dos participantes. Usado nos testes unitários e nos steps BDD.
 */
public class CenarioSaga {
    public final FakeOrdemServicoRepository osRepository = new FakeOrdemServicoRepository();
    public final FakeSagaRepository sagaRepository = new FakeSagaRepository();
    public final FakePublicador publicador = new FakePublicador();
    public final FakeMensagensProcessadas processadas = new FakeMensagensProcessadas();
    public final List<String> emails = new ArrayList<>();
    public final MutableClock clock = new MutableClock(Instant.parse("2026-11-11T10:00:00Z"));
    public final Cliente cliente = new Cliente("Maria Souza", "12345678909", "41999999999", "maria@email.com");
    public final Veiculo veiculo = new Veiculo("ABC1D23", "Fiat", "Argo", 2022, "Prata", "12345678909");
    public final OrquestradorSagaOS orquestrador;

    public final UUID orcamentoId = UUID.randomUUID();
    public final UUID pagamentoId = UUID.randomUUID();
    public final UUID insumoId = UUID.randomUUID();
    public final UUID servicoId = UUID.randomUUID();
    public OrdemServico os;

    public CenarioSaga() {
        ClienteRepository clienteRepository = mock(ClienteRepository.class);
        when(clienteRepository.buscarPorId(any())).thenReturn(Optional.of(cliente));
        NotificacaoPort notificacao = (dest, assunto, msg) -> emails.add(msg);
        orquestrador = new OrquestradorSagaOS(osRepository, sagaRepository, publicador, processadas,
                new NotificarStatusOrdemServicoService(clienteRepository, notificacao),
                new PrazosSaga(Duration.ofHours(48), Duration.ofHours(24)), clock);
    }

    public OrdemServico abrirOs() {
        os = osRepository.salvar(new OrdemServico(cliente.getId(), veiculo.getId(), "Barulho no freio"));
        orquestrador.iniciar(os, cliente, veiculo);
        return os;
    }

    public SagaOrdemServico saga() {
        return sagaRepository.buscarPorOsId(os.getId()).orElseThrow();
    }

    public Mensagem evento(String tipo, Map<String, Object> dados) {
        return Mensagem.nova(tipo, os.getId(), "corr-teste", dados);
    }

    public void receber(String tipo) {
        receber(tipo, Map.of());
    }

    public void receber(String tipo, Map<String, Object> dados) {
        orquestrador.processar(evento(tipo, dados));
    }

    public List<Map<String, Object>> itensDiagnostico() {
        return List.of(
                Map.of("tipo", "SERVICO", "referenciaId", servicoId.toString(), "descricao", "Troca de pastilhas",
                        "precoUnitario", "150.00", "quantidade", 1),
                Map.of("tipo", "INSUMO", "referenciaId", insumoId.toString(), "descricao", "Pastilha de freio",
                        "precoUnitario", "80.00", "quantidade", 2));
    }

    public static class MutableClock extends Clock {
        private Instant agora;

        public MutableClock(Instant inicio) {
            this.agora = inicio;
        }

        public void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
