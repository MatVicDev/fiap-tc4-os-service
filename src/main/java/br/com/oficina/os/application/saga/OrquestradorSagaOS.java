package br.com.oficina.os.application.saga;

import br.com.oficina.os.application.ordemServico.NotificarStatusOrdemServicoService;
import br.com.oficina.os.domain.cliente.Cliente;
import br.com.oficina.os.domain.exception.EntidadeNaoEncontradaException;
import br.com.oficina.os.domain.mensageria.Mensagem;
import br.com.oficina.os.domain.mensageria.MensagensProcessadas;
import br.com.oficina.os.domain.mensageria.PublicadorMensagens;
import br.com.oficina.os.domain.ordemServico.ItemOS;
import br.com.oficina.os.domain.ordemServico.OrdemServico;
import br.com.oficina.os.domain.ordemServico.OrdemServicoRepository;
import br.com.oficina.os.domain.saga.Compensacao;
import br.com.oficina.os.domain.saga.EtapaSaga;
import br.com.oficina.os.domain.saga.SagaOrdemServico;
import br.com.oficina.os.domain.saga.SagaRepository;
import br.com.oficina.os.domain.veiculo.Veiculo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

import static br.com.oficina.os.domain.mensageria.TiposMensagem.*;

/**
 * Orquestrador central da Saga da Ordem de Serviço.
 * <p>
 * Fluxo feliz: abrir OS → diagnóstico (execution) → orçamento (billing) → aprovação do
 * cliente → pagamento Mercado Pago (billing) → reserva de estoque e execução (execution)
 * → finalização. Em qualquer falha (orçamento rejeitado, pagamento recusado, estoque
 * insuficiente, prazo expirado ou cancelamento solicitado) o orquestrador dispara as
 * compensações necessárias conforme o ponto em que o fluxo parou.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrquestradorSagaOS {

    private final OrdemServicoRepository osRepository;
    private final SagaRepository sagaRepository;
    private final PublicadorMensagens publicador;
    private final MensagensProcessadas mensagensProcessadas;
    private final NotificarStatusOrdemServicoService notificador;
    private final PrazosSaga prazos;
    private final Clock clock;

    /** Chamado na mesma transação da abertura da OS. */
    @Transactional
    public SagaOrdemServico iniciar(OrdemServico os, Cliente cliente, Veiculo veiculo) {
        SagaOrdemServico saga = new SagaOrdemServico(os.getId());

        Map<String, Object> dados = new HashMap<>();
        dados.put("clienteId", cliente.getId().toString());
        dados.put("veiculoId", veiculo.getId().toString());
        dados.put("placa", veiculo.getPlaca().getNumero());
        dados.put("marca", veiculo.getMarca());
        dados.put("modelo", veiculo.getModelo());
        dados.put("sintomaRelatado", os.getSintomaRelatado());
        enviar(saga, CMD_INICIAR_DIAGNOSTICO, os.getId(), dados);

        return sagaRepository.salvar(saga);
    }

    @Transactional
    public void processar(Mensagem evento) {
        if (!mensagensProcessadas.registrarSeNova(evento.id(), evento.tipo())) {
            log.info("Evento {} ({}) já processado — ignorando entrega duplicada", evento.id(), evento.tipo());
            return;
        }

        Optional<SagaOrdemServico> sagaOpt = sagaRepository.buscarPorOsId(evento.osId());
        if (sagaOpt.isEmpty()) {
            log.warn("Evento {} recebido para OS {} sem Saga — descartado", evento.tipo(), evento.osId());
            return;
        }
        SagaOrdemServico saga = sagaOpt.get();
        OrdemServico os = buscarOs(evento.osId());

        saga.registrarEvento("RECEBIDO " + evento.tipo(), resumo(evento));
        log.info("Saga da OS {} (etapa {}, status {}) recebeu {}", os.getId(), saga.getEtapa(), saga.getStatus(), evento.tipo());

        try {
            if (saga.isEmAndamento()) {
                tratarEventoNoFluxo(saga, os, evento);
            } else {
                tratarEventoForaDoFluxo(saga, evento);
            }
        } catch (IllegalStateException e) {
            // Evento fora de ordem/inesperado para o estado atual: registra e segue, sem
            // reenfileirar — repetir a mensagem não mudaria o resultado.
            log.warn("Evento {} ignorado para a OS {}: {}", evento.tipo(), os.getId(), e.getMessage());
            saga.registrarEvento("IGNORADO " + evento.tipo(), e.getMessage());
        }

        osRepository.salvar(os);
        sagaRepository.salvar(saga);
    }

    private void tratarEventoNoFluxo(SagaOrdemServico saga, OrdemServico os, Mensagem evento) {
        switch (evento.tipo()) {
            case EVT_DIAGNOSTICO_INICIADO -> {
                os.iniciarDiagnostico();
                notificar(os, null);
            }
            case EVT_DIAGNOSTICO_CONCLUIDO -> aoConcluirDiagnostico(saga, os, evento);
            case EVT_ORCAMENTO_GERADO -> {
                saga.registrarOrcamento(evento.uuid("orcamentoId"));
                saga.avancar(EtapaSaga.APROVACAO);
                saga.definirPrazo(LocalDateTime.now(clock).plus(prazos.aprovacao()));
            }
            case EVT_ORCAMENTO_APROVADO -> {
                os.aguardarPagamento();
                saga.avancar(EtapaSaga.PAGAMENTO);
                saga.definirPrazo(LocalDateTime.now(clock).plus(prazos.pagamento()));
                String link = evento.texto("linkPagamento");
                notificar(os, link == null ? null : "Efetue o pagamento em: " + link);
            }
            case EVT_ORCAMENTO_REJEITADO -> compensar(saga, os, "Orçamento rejeitado pelo cliente");
            case EVT_PAGAMENTO_CONFIRMADO -> aoConfirmarPagamento(saga, os, evento);
            case EVT_PAGAMENTO_RECUSADO -> compensar(saga, os,
                    "Pagamento recusado" + sufixo(evento.texto("motivo")));
            case EVT_EXECUCAO_INICIADA -> {
                os.iniciarExecucao();
                notificar(os, null);
            }
            case EVT_EXECUCAO_FALHOU -> compensar(saga, os,
                    "Falha na execução" + sufixo(evento.texto("motivo")));
            case EVT_EXECUCAO_FINALIZADA -> {
                os.finalizar();
                saga.concluir();
                notificar(os, null);
            }
            default -> throw new IllegalStateException("Evento " + evento.tipo() + " não esperado na etapa " + saga.getEtapa());
        }
    }

    private void aoConcluirDiagnostico(SagaOrdemServico saga, OrdemServico os, Mensagem evento) {
        List<ItemOS> itens = evento.lista("itens").stream().map(ItemMensagem::paraItemOS).toList();
        os.concluirDiagnostico(itens);
        saga.avancar(EtapaSaga.ORCAMENTO);

        Map<String, Object> dados = new HashMap<>();
        dados.put("clienteId", os.getClienteId().toString());
        dados.put("valorTotal", os.getValorTotal().toPlainString());
        dados.put("itens", os.getItens().stream().map(ItemMensagem::deItemOS).toList());
        notificador.dadosContato(os.getClienteId()).ifPresent(c -> {
            dados.put("nomeCliente", c.nome());
            dados.put("emailCliente", c.email());
            dados.put("cpfCliente", c.cpf());
        });
        enviar(saga, CMD_GERAR_ORCAMENTO, os.getId(), dados);
        notificar(os, null);
    }

    private void aoConfirmarPagamento(SagaOrdemServico saga, OrdemServico os, Mensagem evento) {
        saga.registrarPagamento(evento.uuid("pagamentoId"));
        os.confirmarPagamento();
        saga.avancar(EtapaSaga.EXECUCAO);

        Map<String, Object> dados = new HashMap<>();
        dados.put("itens", os.getItens().stream().map(ItemMensagem::deItemOS).toList());
        enviar(saga, CMD_INICIAR_EXECUCAO, os.getId(), dados);
        notificar(os, null);
    }

    /**
     * Eventos que chegam com a Saga já compensando/compensada/concluída: confirmações de
     * compensação ou efeitos tardios que exigem uma compensação adicional.
     */
    private void tratarEventoForaDoFluxo(SagaOrdemServico saga, Mensagem evento) {
        switch (evento.tipo()) {
            case EVT_PAGAMENTO_ESTORNADO -> saga.confirmarCompensacao(Compensacao.ESTORNO_PAGAMENTO);
            case EVT_ORCAMENTO_CANCELADO, EVT_ORCAMENTO_REJEITADO ->
                    saga.confirmarCompensacao(Compensacao.CANCELAMENTO_ORCAMENTO);
            case EVT_EXECUCAO_CANCELADA -> saga.confirmarCompensacao(Compensacao.CANCELAMENTO_EXECUCAO);
            case EVT_PAGAMENTO_CONFIRMADO -> {
                // Cliente pagou depois que a OS já havia sido cancelada: estorna.
                saga.registrarPagamento(evento.uuid("pagamentoId"));
                saga.confirmarCompensacao(Compensacao.CANCELAMENTO_ORCAMENTO);
                saga.adicionarCompensacao(Compensacao.ESTORNO_PAGAMENTO);
                enviar(saga, CMD_ESTORNAR_PAGAMENTO, saga.getOsId(), dadosEstorno(saga, "Pagamento recebido após cancelamento da OS"));
            }
            case EVT_ORCAMENTO_GERADO -> {
                // Orçamento criado depois do cancelamento (comando já estava em trânsito).
                saga.registrarOrcamento(evento.uuid("orcamentoId"));
                saga.adicionarCompensacao(Compensacao.CANCELAMENTO_ORCAMENTO);
                enviar(saga, CMD_CANCELAR_ORCAMENTO, saga.getOsId(), dadosCancelamentoOrcamento(saga, "OS cancelada"));
            }
            default -> throw new IllegalStateException("Saga com status " + saga.getStatus());
        }
    }

    /** Cancelamento solicitado pelo cliente/atendente via API. */
    @Transactional
    public OrdemServico cancelarPorSolicitacao(UUID osId, String motivo) {
        OrdemServico os = buscarOs(osId);
        SagaOrdemServico saga = sagaRepository.buscarPorOsId(osId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Saga da OS não encontrada"));
        if (!saga.isEmAndamento() || !os.podeSerCancelada()) {
            throw new IllegalStateException("A OS em estado " + os.getStatus() + " não pode mais ser cancelada.");
        }
        compensar(saga, os, "Cancelamento solicitado" + sufixo(motivo));
        osRepository.salvar(os);
        sagaRepository.salvar(saga);
        return os;
    }

    /** Executado periodicamente: compensa Sagas cuja etapa humana passou do prazo. */
    @Transactional
    public int expirarSagasVencidas() {
        LocalDateTime agora = LocalDateTime.now(clock);
        int expiradas = 0;
        for (SagaOrdemServico saga : sagaRepository.listarExpiradas(agora)) {
            if (!saga.isExpirada(agora)) {
                continue;
            }
            OrdemServico os = buscarOs(saga.getOsId());
            compensar(saga, os, "Prazo da etapa " + saga.getEtapa() + " expirado");
            osRepository.salvar(os);
            sagaRepository.salvar(saga);
            expiradas++;
        }
        return expiradas;
    }

    /**
     * Decide e dispara as compensações conforme o que já foi efetivado nos participantes:
     * pagamento confirmado → estorno; orçamento gerado sem pagamento → cancelamento do
     * orçamento; e sempre o cancelamento da execução (libera estoque reservado e remove
     * a OS da fila — idempotente no execution-service).
     */
    private void compensar(SagaOrdemServico saga, OrdemServico os, String motivo) {
        log.warn("Iniciando compensação da Saga da OS {} na etapa {}: {}", os.getId(), saga.getEtapa(), motivo);
        Set<Compensacao> pendentes = EnumSet.of(Compensacao.CANCELAMENTO_EXECUCAO);

        if (saga.getPagamentoId() != null) {
            pendentes.add(Compensacao.ESTORNO_PAGAMENTO);
        } else if (saga.getOrcamentoId() != null) {
            pendentes.add(Compensacao.CANCELAMENTO_ORCAMENTO);
        }

        saga.iniciarCompensacao(motivo, pendentes);
        os.cancelar(motivo);

        if (pendentes.contains(Compensacao.ESTORNO_PAGAMENTO)) {
            enviar(saga, CMD_ESTORNAR_PAGAMENTO, os.getId(), dadosEstorno(saga, motivo));
        }
        if (pendentes.contains(Compensacao.CANCELAMENTO_ORCAMENTO)) {
            enviar(saga, CMD_CANCELAR_ORCAMENTO, os.getId(), dadosCancelamentoOrcamento(saga, motivo));
        }
        enviar(saga, CMD_CANCELAR_EXECUCAO, os.getId(), Map.of("motivo", motivo));
        notificar(os, motivo);
    }

    private Map<String, Object> dadosEstorno(SagaOrdemServico saga, String motivo) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("orcamentoId", saga.getOrcamentoId() == null ? null : saga.getOrcamentoId().toString());
        dados.put("pagamentoId", saga.getPagamentoId().toString());
        dados.put("motivo", motivo);
        return dados;
    }

    private Map<String, Object> dadosCancelamentoOrcamento(SagaOrdemServico saga, String motivo) {
        return Map.of("orcamentoId", saga.getOrcamentoId().toString(), "motivo", motivo);
    }

    private void enviar(SagaOrdemServico saga, String tipo, UUID osId, Map<String, Object> dados) {
        publicador.enviarComando(Mensagem.nova(tipo, osId, MDC.get("correlationId"), dados));
        saga.registrarEvento("ENVIADO " + tipo, null);
    }

    private void notificar(OrdemServico os, String complemento) {
        notificador.notificarMudancaStatus(os, complemento);
        publicador.publicarEvento(Mensagem.nova(EVT_OS_STATUS_ALTERADO, os.getId(), MDC.get("correlationId"),
                Map.of("status", os.getStatus().name())));
    }

    private OrdemServico buscarOs(UUID osId) {
        return osRepository.buscarPorId(osId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("OS não encontrada"));
    }

    private static String sufixo(String detalhe) {
        return detalhe == null || detalhe.isBlank() ? "" : ": " + detalhe;
    }

    private static String resumo(Mensagem evento) {
        String motivo = evento.texto("motivo");
        return motivo != null ? "motivo=" + motivo : "mensagem " + evento.id();
    }
}
