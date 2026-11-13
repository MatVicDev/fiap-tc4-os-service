package br.com.oficina.os.domain.ordemServico;

import br.com.oficina.os.domain.exception.DomainException;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Agregado raiz da Ordem de Serviço. As transições de estado são disparadas pelo
 * orquestrador da Saga a partir dos eventos dos outros microsserviços — por isso
 * cada método valida o estado de origem e registra a mudança no histórico.
 */
@Entity
@NoArgsConstructor
@Table(name = "ordens_servico", indexes = {
        @Index(name = "idx_os_cliente_id", columnList = "clienteId"),
        @Index(name = "idx_os_status", columnList = "status")
})
public class OrdemServico {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID clienteId;

    @Column(nullable = false)
    private UUID veiculoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOS status;

    @Column(length = 1000)
    private String sintomaRelatado;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "itens_ordem_servico", joinColumns = @JoinColumn(name = "ordem_servico_id"))
    private List<ItemOS> itens = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "historico_status_os", joinColumns = @JoinColumn(name = "ordem_servico_id"))
    @OrderColumn(name = "ordem")
    private List<HistoricoStatusOS> historico = new ArrayList<>();

    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(length = 500)
    private String motivoCancelamento;

    private LocalDateTime dataAbertura;
    private LocalDateTime dataFinalizacao;
    private LocalDateTime dataEntrega;

    @Version
    private Long versao;

    public OrdemServico(UUID clienteId, UUID veiculoId, String sintomaRelatado) {
        if (clienteId == null || veiculoId == null) {
            throw new DomainException("Cliente e veículo são obrigatórios para abrir uma OS.");
        }
        this.id = UUID.randomUUID();
        this.clienteId = clienteId;
        this.veiculoId = veiculoId;
        this.sintomaRelatado = sintomaRelatado;
        this.dataAbertura = LocalDateTime.now();
        mudarStatus(StatusOS.RECEBIDA, "OS aberta");
    }

    public void iniciarDiagnostico() {
        validarTransicao(StatusOS.RECEBIDA);
        mudarStatus(StatusOS.EM_DIAGNOSTICO, "Diagnóstico iniciado pela equipe técnica");
    }

    public void concluirDiagnostico(List<ItemOS> itensDiagnostico) {
        validarTransicao(StatusOS.EM_DIAGNOSTICO);
        if (itensDiagnostico == null || itensDiagnostico.isEmpty()) {
            throw new DomainException("O diagnóstico deve conter ao menos um item.");
        }
        this.itens.clear();
        this.itens.addAll(itensDiagnostico);
        this.valorTotal = itens.stream().map(ItemOS::getPrecoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        mudarStatus(StatusOS.AGUARDANDO_APROVACAO, "Diagnóstico concluído — orçamento enviado ao cliente");
    }

    public void aguardarPagamento() {
        validarTransicao(StatusOS.AGUARDANDO_APROVACAO);
        mudarStatus(StatusOS.AGUARDANDO_PAGAMENTO, "Orçamento aprovado pelo cliente");
    }

    public void confirmarPagamento() {
        validarTransicao(StatusOS.AGUARDANDO_PAGAMENTO);
        mudarStatus(StatusOS.AGUARDANDO_EXECUCAO, "Pagamento confirmado — OS enviada para a fila de execução");
    }

    public void iniciarExecucao() {
        validarTransicao(StatusOS.AGUARDANDO_EXECUCAO);
        mudarStatus(StatusOS.EM_EXECUCAO, "Execução dos serviços iniciada");
    }

    public void finalizar() {
        validarTransicao(StatusOS.EM_EXECUCAO);
        this.dataFinalizacao = LocalDateTime.now();
        mudarStatus(StatusOS.FINALIZADA, "Serviços finalizados — veículo pronto para retirada");
    }

    public void registrarEntrega() {
        if (status != StatusOS.FINALIZADA && status != StatusOS.CANCELADA) {
            throw new IllegalStateException(String.format(
                    "Transição inválida: Não é possível registrar a entrega pois a OS está em estado %s.", status));
        }
        this.dataEntrega = LocalDateTime.now();
        mudarStatus(StatusOS.ENTREGUE, "Veículo entregue ao cliente");
    }

    /**
     * Cancelamento é a transição de compensação da Saga — permitida em qualquer estado
     * anterior à finalização dos serviços.
     */
    public void cancelar(String motivo) {
        if (!podeSerCancelada()) {
            throw new IllegalStateException(String.format(
                    "Transição inválida: Não é possível cancelar a OS em estado %s.", status));
        }
        this.motivoCancelamento = motivo;
        mudarStatus(StatusOS.CANCELADA, motivo);
    }

    public boolean podeSerCancelada() {
        return status != StatusOS.EM_EXECUCAO && !status.isTerminal();
    }

    private void validarTransicao(StatusOS esperado) {
        if (this.status != esperado) {
            throw new IllegalStateException(String.format(
                    "Transição inválida: Não é possível realizar esta ação pois a OS está em estado %s.", this.status));
        }
    }

    private void mudarStatus(StatusOS novoStatus, String observacao) {
        this.status = novoStatus;
        this.historico.add(new HistoricoStatusOS(novoStatus, observacao));
    }

    public UUID getId() {
        return id;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public UUID getVeiculoId() {
        return veiculoId;
    }

    public StatusOS getStatus() {
        return status;
    }

    public String getSintomaRelatado() {
        return sintomaRelatado;
    }

    public List<ItemOS> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public List<HistoricoStatusOS> getHistorico() {
        return Collections.unmodifiableList(historico);
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public LocalDateTime getDataFinalizacao() {
        return dataFinalizacao;
    }

    public LocalDateTime getDataEntrega() {
        return dataEntrega;
    }
}
