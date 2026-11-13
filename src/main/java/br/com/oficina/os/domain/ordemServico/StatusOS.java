package br.com.oficina.os.domain.ordemServico;

public enum StatusOS {
    RECEBIDA,
    EM_DIAGNOSTICO,
    AGUARDANDO_APROVACAO,
    AGUARDANDO_PAGAMENTO,
    AGUARDANDO_EXECUCAO,
    EM_EXECUCAO,
    FINALIZADA,
    ENTREGUE,
    CANCELADA;

    public boolean isTerminal() {
        return this == ENTREGUE || this == CANCELADA || this == FINALIZADA;
    }
}
