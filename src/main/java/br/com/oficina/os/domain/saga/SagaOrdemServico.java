package br.com.oficina.os.domain.saga;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Estado persistido da Saga orquestrada de uma OS. Guardar o estado em banco (e não
 * só em memória) é o que permite ao orquestrador retomar o fluxo após um restart do
 * pod e decidir quais compensações disparar conforme a etapa em que a falha ocorreu.
 */
@Entity
@NoArgsConstructor
@Table(name = "sagas_ordem_servico", indexes = {
        @Index(name = "idx_saga_os_id", columnList = "osId", unique = true),
        @Index(name = "idx_saga_status_expira", columnList = "status,expiraEm")
})
public class SagaOrdemServico {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID osId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EtapaSaga etapa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSaga status;

    private UUID orcamentoId;
    private UUID pagamentoId;

    @Column(length = 500)
    private String motivoFalha;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "saga_compensacoes_pendentes", joinColumns = @JoinColumn(name = "saga_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "compensacao", length = 30)
    private Set<Compensacao> compensacoesPendentes = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "saga_eventos", joinColumns = @JoinColumn(name = "saga_id"))
    @OrderColumn(name = "ordem")
    private List<EventoSaga> eventos = new ArrayList<>();

    private LocalDateTime iniciadaEm;
    private LocalDateTime atualizadaEm;

    /** Prazo da etapa atual (aprovação/pagamento); expirado, o orquestrador compensa. */
    private LocalDateTime expiraEm;

    @Version
    private Long versao;

    public SagaOrdemServico(UUID osId) {
        this.id = UUID.randomUUID();
        this.osId = osId;
        this.etapa = EtapaSaga.DIAGNOSTICO;
        this.status = StatusSaga.EM_ANDAMENTO;
        this.iniciadaEm = LocalDateTime.now();
        registrarEvento("SAGA_INICIADA", "Saga iniciada para a OS " + osId);
    }

    public void avancar(EtapaSaga novaEtapa) {
        exigirEmAndamento();
        this.etapa = novaEtapa;
        this.expiraEm = null;
        tocar();
    }

    public void definirPrazo(LocalDateTime prazo) {
        this.expiraEm = prazo;
    }

    public void registrarOrcamento(UUID orcamentoId) {
        this.orcamentoId = orcamentoId;
        tocar();
    }

    public void registrarPagamento(UUID pagamentoId) {
        this.pagamentoId = pagamentoId;
        tocar();
    }

    public void concluir() {
        exigirEmAndamento();
        this.etapa = EtapaSaga.FINALIZADA;
        this.status = StatusSaga.CONCLUIDA;
        this.expiraEm = null;
        registrarEvento("SAGA_CONCLUIDA", "Fluxo da OS concluído com sucesso");
    }

    public void iniciarCompensacao(String motivo, Set<Compensacao> pendentes) {
        exigirEmAndamento();
        this.status = StatusSaga.COMPENSANDO;
        this.motivoFalha = motivo;
        this.expiraEm = null;
        this.compensacoesPendentes.clear();
        this.compensacoesPendentes.addAll(pendentes);
        registrarEvento("COMPENSACAO_INICIADA", motivo + " — pendentes: " + pendentes);
        verificarFimCompensacao();
    }

    /**
     * Uma compensação extra pode ser exigida depois que a Saga já foi compensada —
     * p.ex. um pagamento aprovado no Mercado Pago após o cliente ter cancelado a OS.
     */
    public void adicionarCompensacao(Compensacao compensacao) {
        if (status == StatusSaga.EM_ANDAMENTO || status == StatusSaga.CONCLUIDA) {
            throw new IllegalStateException("Compensação extra só é permitida em Saga já em compensação.");
        }
        this.status = StatusSaga.COMPENSANDO;
        this.compensacoesPendentes.add(compensacao);
        registrarEvento("COMPENSACAO_ADICIONAL", compensacao.name());
    }

    public void confirmarCompensacao(Compensacao compensacao) {
        if (compensacoesPendentes.remove(compensacao)) {
            registrarEvento("COMPENSACAO_CONFIRMADA", compensacao.name());
            verificarFimCompensacao();
        }
    }

    private void verificarFimCompensacao() {
        if (status == StatusSaga.COMPENSANDO && compensacoesPendentes.isEmpty()) {
            this.status = StatusSaga.COMPENSADA;
            registrarEvento("SAGA_COMPENSADA", "Todas as compensações foram confirmadas");
        }
    }

    public void registrarEvento(String tipo, String descricao) {
        this.eventos.add(new EventoSaga(tipo, descricao));
        tocar();
    }

    public boolean isEmAndamento() {
        return status == StatusSaga.EM_ANDAMENTO;
    }

    public boolean isExpirada(LocalDateTime agora) {
        return isEmAndamento() && expiraEm != null && expiraEm.isBefore(agora);
    }

    private void exigirEmAndamento() {
        if (!isEmAndamento()) {
            throw new IllegalStateException("A Saga da OS " + osId + " não está em andamento (status " + status + ").");
        }
    }

    private void tocar() {
        this.atualizadaEm = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOsId() {
        return osId;
    }

    public EtapaSaga getEtapa() {
        return etapa;
    }

    public StatusSaga getStatus() {
        return status;
    }

    public UUID getOrcamentoId() {
        return orcamentoId;
    }

    public UUID getPagamentoId() {
        return pagamentoId;
    }

    public String getMotivoFalha() {
        return motivoFalha;
    }

    public Set<Compensacao> getCompensacoesPendentes() {
        return Collections.unmodifiableSet(compensacoesPendentes);
    }

    public List<EventoSaga> getEventos() {
        return Collections.unmodifiableList(eventos);
    }

    public LocalDateTime getIniciadaEm() {
        return iniciadaEm;
    }

    public LocalDateTime getAtualizadaEm() {
        return atualizadaEm;
    }

    public LocalDateTime getExpiraEm() {
        return expiraEm;
    }
}
