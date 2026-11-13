package br.com.oficina.os.application.saga;

import br.com.oficina.os.domain.exception.EntidadeNaoEncontradaException;
import br.com.oficina.os.domain.mensageria.Mensagem;
import br.com.oficina.os.domain.ordemServico.StatusOS;
import br.com.oficina.os.domain.saga.Compensacao;
import br.com.oficina.os.domain.saga.EtapaSaga;
import br.com.oficina.os.domain.saga.StatusSaga;
import br.com.oficina.os.support.CenarioSaga;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static br.com.oficina.os.domain.mensageria.TiposMensagem.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Testes do orquestrador da Saga da OS")
class OrquestradorSagaOSTest {

    private CenarioSaga c;

    @BeforeEach
    void setUp() {
        c = new CenarioSaga();
        c.abrirOs();
    }

    private void ateAguardarAprovacao() {
        c.receber(EVT_DIAGNOSTICO_INICIADO);
        c.receber(EVT_DIAGNOSTICO_CONCLUIDO, Map.of("itens", c.itensDiagnostico()));
        c.receber(EVT_ORCAMENTO_GERADO, Map.of("orcamentoId", c.orcamentoId.toString(), "valorTotal", "310.00"));
    }

    private void ateAguardarExecucao() {
        ateAguardarAprovacao();
        c.receber(EVT_ORCAMENTO_APROVADO, Map.of("orcamentoId", c.orcamentoId.toString(), "linkPagamento", "https://mp/pagar"));
        c.receber(EVT_PAGAMENTO_CONFIRMADO, Map.of("pagamentoId", c.pagamentoId.toString()));
    }

    @Nested
    @DisplayName("Fluxo feliz")
    class FluxoFeliz {

        @Test
        @DisplayName("Ao iniciar, cria a Saga na etapa de diagnóstico e envia o comando ao execution-service")
        void deveIniciarSaga() {
            assertThat(c.saga().getEtapa()).isEqualTo(EtapaSaga.DIAGNOSTICO);
            assertThat(c.saga().getStatus()).isEqualTo(StatusSaga.EM_ANDAMENTO);
            Mensagem cmd = c.publicador.ultimoComando(CMD_INICIAR_DIAGNOSTICO);
            assertThat(cmd.osId()).isEqualTo(c.os.getId());
            assertThat(cmd.texto("placa")).isEqualTo("ABC1D23");
            assertThat(cmd.texto("sintomaRelatado")).isEqualTo("Barulho no freio");
        }

        @Test
        @DisplayName("Diagnóstico concluído grava os itens e solicita o orçamento ao billing-service")
        void deveSolicitarOrcamento() {
            c.receber(EVT_DIAGNOSTICO_INICIADO);
            assertThat(c.os.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);

            c.receber(EVT_DIAGNOSTICO_CONCLUIDO, Map.of("itens", c.itensDiagnostico()));

            assertThat(c.os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
            assertThat(c.os.getValorTotal()).isEqualByComparingTo(new BigDecimal("310.00"));
            assertThat(c.saga().getEtapa()).isEqualTo(EtapaSaga.ORCAMENTO);
            Mensagem cmd = c.publicador.ultimoComando(CMD_GERAR_ORCAMENTO);
            assertThat(cmd.texto("valorTotal")).isEqualTo("310.00");
            assertThat(cmd.texto("emailCliente")).isEqualTo("maria@email.com");
            assertThat(cmd.lista("itens")).hasSize(2);
        }

        @Test
        @DisplayName("Orçamento gerado registra o id e abre prazo de aprovação")
        void deveRegistrarOrcamento() {
            ateAguardarAprovacao();
            assertThat(c.saga().getOrcamentoId()).isEqualTo(c.orcamentoId);
            assertThat(c.saga().getEtapa()).isEqualTo(EtapaSaga.APROVACAO);
            assertThat(c.saga().getExpiraEm()).isNotNull();
        }

        @Test
        @DisplayName("Orçamento aprovado coloca a OS aguardando pagamento e envia o link ao cliente")
        void deveAguardarPagamento() {
            ateAguardarAprovacao();
            c.receber(EVT_ORCAMENTO_APROVADO, Map.of("orcamentoId", c.orcamentoId.toString(), "linkPagamento", "https://mp/pagar"));

            assertThat(c.os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_PAGAMENTO);
            assertThat(c.saga().getEtapa()).isEqualTo(EtapaSaga.PAGAMENTO);
            assertThat(c.emails).anyMatch(e -> e.contains("https://mp/pagar"));
        }

        @Test
        @DisplayName("Pagamento confirmado envia a OS para execução com os itens do diagnóstico")
        void deveEnviarParaExecucao() {
            ateAguardarExecucao();

            assertThat(c.os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_EXECUCAO);
            assertThat(c.saga().getPagamentoId()).isEqualTo(c.pagamentoId);
            assertThat(c.saga().getEtapa()).isEqualTo(EtapaSaga.EXECUCAO);
            assertThat(c.publicador.ultimoComando(CMD_INICIAR_EXECUCAO).lista("itens")).hasSize(2);
        }

        @Test
        @DisplayName("Execução finalizada conclui a OS e a Saga")
        void deveConcluir() {
            ateAguardarExecucao();
            c.receber(EVT_EXECUCAO_INICIADA);
            assertThat(c.os.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);

            c.receber(EVT_EXECUCAO_FINALIZADA);

            assertThat(c.os.getStatus()).isEqualTo(StatusOS.FINALIZADA);
            assertThat(c.saga().getStatus()).isEqualTo(StatusSaga.CONCLUIDA);
            assertThat(c.saga().getEtapa()).isEqualTo(EtapaSaga.FINALIZADA);
            assertThat(c.publicador.eventos).extracting(Mensagem::tipo).contains(EVT_OS_STATUS_ALTERADO);
        }
    }

    @Nested
    @DisplayName("Compensações")
    class Compensacoes {

        @Test
        @DisplayName("Orçamento rejeitado cancela a OS, o orçamento e a execução")
        void orcamentoRejeitado() {
            ateAguardarAprovacao();
            c.receber(EVT_ORCAMENTO_REJEITADO, Map.of("orcamentoId", c.orcamentoId.toString()));

            assertThat(c.os.getStatus()).isEqualTo(StatusOS.CANCELADA);
            assertThat(c.saga().getStatus()).isEqualTo(StatusSaga.COMPENSANDO);
            assertThat(c.saga().getCompensacoesPendentes())
                    .containsExactlyInAnyOrder(Compensacao.CANCELAMENTO_ORCAMENTO, Compensacao.CANCELAMENTO_EXECUCAO);
            assertThat(c.publicador.tiposComandos()).contains(CMD_CANCELAR_ORCAMENTO, CMD_CANCELAR_EXECUCAO)
                    .doesNotContain(CMD_ESTORNAR_PAGAMENTO);

            c.receber(EVT_ORCAMENTO_CANCELADO);
            c.receber(EVT_EXECUCAO_CANCELADA);
            assertThat(c.saga().getStatus()).isEqualTo(StatusSaga.COMPENSADA);
        }

        @Test
        @DisplayName("Pagamento recusado compensa sem estorno")
        void pagamentoRecusado() {
            ateAguardarAprovacao();
            c.receber(EVT_ORCAMENTO_APROVADO, Map.of("orcamentoId", c.orcamentoId.toString()));
            c.receber(EVT_PAGAMENTO_RECUSADO, Map.of("motivo", "cc_rejected_insufficient_amount"));

            assertThat(c.os.getStatus()).isEqualTo(StatusOS.CANCELADA);
            assertThat(c.os.getMotivoCancelamento()).contains("cc_rejected_insufficient_amount");
            assertThat(c.publicador.tiposComandos()).doesNotContain(CMD_ESTORNAR_PAGAMENTO);
        }

        @Test
        @DisplayName("Falha na execução (estoque insuficiente) estorna o pagamento no Mercado Pago")
        void falhaNaExecucaoEstornaPagamento() {
            ateAguardarExecucao();
            c.receber(EVT_EXECUCAO_FALHOU, Map.of("motivo", "Estoque insuficiente"));

            assertThat(c.os.getStatus()).isEqualTo(StatusOS.CANCELADA);
            assertThat(c.saga().getCompensacoesPendentes())
                    .containsExactlyInAnyOrder(Compensacao.ESTORNO_PAGAMENTO, Compensacao.CANCELAMENTO_EXECUCAO);
            Mensagem estorno = c.publicador.ultimoComando(CMD_ESTORNAR_PAGAMENTO);
            assertThat(estorno.uuid("pagamentoId")).isEqualTo(c.pagamentoId);
            assertThat(estorno.uuid("orcamentoId")).isEqualTo(c.orcamentoId);

            c.receber(EVT_PAGAMENTO_ESTORNADO);
            assertThat(c.saga().getStatus()).isEqualTo(StatusSaga.COMPENSANDO);
            c.receber(EVT_EXECUCAO_CANCELADA);
            assertThat(c.saga().getStatus()).isEqualTo(StatusSaga.COMPENSADA);
        }

        @Test
        @DisplayName("Cancelamento solicitado durante o diagnóstico só cancela a execução")
        void cancelamentoNoDiagnostico() {
            c.orquestrador.cancelarPorSolicitacao(c.os.getId(), "Cliente desistiu");

            assertThat(c.os.getStatus()).isEqualTo(StatusOS.CANCELADA);
            assertThat(c.saga().getCompensacoesPendentes()).containsExactly(Compensacao.CANCELAMENTO_EXECUCAO);
            assertThat(c.os.getMotivoCancelamento()).isEqualTo("Cancelamento solicitado: Cliente desistiu");
        }

        @Test
        @DisplayName("Não permite cancelar OS em execução")
        void naoCancelaEmExecucao() {
            ateAguardarExecucao();
            c.receber(EVT_EXECUCAO_INICIADA);
            UUID osId = c.os.getId();

            assertThatThrownBy(() -> c.orquestrador.cancelarPorSolicitacao(osId, null))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Cancelar OS inexistente lança exceção de entidade não encontrada")
        void cancelarInexistente() {
            UUID id = UUID.randomUUID();
            assertThatThrownBy(() -> c.orquestrador.cancelarPorSolicitacao(id, null))
                    .isInstanceOf(EntidadeNaoEncontradaException.class);
        }

        @Test
        @DisplayName("Prazo de pagamento expirado compensa a Saga automaticamente")
        void prazoExpirado() {
            ateAguardarAprovacao();
            c.receber(EVT_ORCAMENTO_APROVADO, Map.of("orcamentoId", c.orcamentoId.toString()));

            assertThat(c.orquestrador.expirarSagasVencidas()).isZero();
            c.clock.avancar(Duration.ofHours(25));
            assertThat(c.orquestrador.expirarSagasVencidas()).isEqualTo(1);

            assertThat(c.os.getStatus()).isEqualTo(StatusOS.CANCELADA);
            assertThat(c.saga().getMotivoFalha()).contains("PAGAMENTO");
            assertThat(c.publicador.tiposComandos()).contains(CMD_CANCELAR_ORCAMENTO);
        }

        @Test
        @DisplayName("Pagamento confirmado após o cancelamento dispara estorno adicional")
        void pagamentoTardioEstornado() {
            ateAguardarAprovacao();
            c.receber(EVT_ORCAMENTO_APROVADO, Map.of("orcamentoId", c.orcamentoId.toString()));
            c.orquestrador.cancelarPorSolicitacao(c.os.getId(), null);

            c.receber(EVT_PAGAMENTO_CONFIRMADO, Map.of("pagamentoId", c.pagamentoId.toString()));

            assertThat(c.saga().getCompensacoesPendentes()).contains(Compensacao.ESTORNO_PAGAMENTO)
                    .doesNotContain(Compensacao.CANCELAMENTO_ORCAMENTO);
            assertThat(c.publicador.ultimoComando(CMD_ESTORNAR_PAGAMENTO).uuid("pagamentoId")).isEqualTo(c.pagamentoId);
        }

        @Test
        @DisplayName("Orçamento gerado após o cancelamento é cancelado em seguida")
        void orcamentoTardioCancelado() {
            c.receber(EVT_DIAGNOSTICO_INICIADO);
            c.receber(EVT_DIAGNOSTICO_CONCLUIDO, Map.of("itens", c.itensDiagnostico()));
            c.orquestrador.cancelarPorSolicitacao(c.os.getId(), null);

            c.receber(EVT_ORCAMENTO_GERADO, Map.of("orcamentoId", c.orcamentoId.toString()));

            assertThat(c.saga().getCompensacoesPendentes()).contains(Compensacao.CANCELAMENTO_ORCAMENTO);
            assertThat(c.publicador.ultimoComando(CMD_CANCELAR_ORCAMENTO).uuid("orcamentoId")).isEqualTo(c.orcamentoId);
        }
    }

    @Nested
    @DisplayName("Robustez")
    class Robustez {

        @Test
        @DisplayName("Evento duplicado (mesmo id) é processado uma única vez")
        void eventoDuplicado() {
            Mensagem evento = c.evento(EVT_DIAGNOSTICO_INICIADO, Map.of());
            c.orquestrador.processar(evento);
            int eventosNaSaga = c.saga().getEventos().size();

            c.orquestrador.processar(evento);

            assertThat(c.saga().getEventos()).hasSize(eventosNaSaga);
            assertThat(c.os.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
        }

        @Test
        @DisplayName("Evento fora de ordem é ignorado e registrado no log da Saga")
        void eventoForaDeOrdem() {
            c.receber(EVT_EXECUCAO_FINALIZADA);

            assertThat(c.os.getStatus()).isEqualTo(StatusOS.RECEBIDA);
            assertThat(c.saga().getEventos()).anyMatch(e -> e.getTipo().startsWith("IGNORADO"));
        }

        @Test
        @DisplayName("Evento desconhecido é ignorado")
        void eventoDesconhecido() {
            c.receber("execution.evento-inexistente");
            assertThat(c.saga().getEventos()).anyMatch(e -> e.getTipo().equals("IGNORADO execution.evento-inexistente"));
        }

        @Test
        @DisplayName("Evento de OS sem Saga é descartado sem erro")
        void eventoSemSaga() {
            Mensagem evento = Mensagem.nova(EVT_DIAGNOSTICO_INICIADO, UUID.randomUUID(), null, Map.of());
            c.orquestrador.processar(evento);
            assertThat(c.os.getStatus()).isEqualTo(StatusOS.RECEBIDA);
        }

        @Test
        @DisplayName("Evento não relacionado a compensação com Saga compensada é ignorado")
        void eventoIrrelevanteAposCompensacao() {
            c.orquestrador.cancelarPorSolicitacao(c.os.getId(), null);
            c.receber(EVT_DIAGNOSTICO_INICIADO);
            assertThat(c.os.getStatus()).isEqualTo(StatusOS.CANCELADA);
            assertThat(c.saga().getEventos()).anyMatch(e -> e.getTipo().startsWith("IGNORADO"));
        }
    }
}
