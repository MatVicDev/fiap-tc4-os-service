package br.com.oficina.os.domain.saga;

/**
 * Ações de compensação que o orquestrador pode disparar nos participantes. Cada uma
 * fica pendente na Saga até o participante confirmar com o evento correspondente.
 */
public enum Compensacao {
    ESTORNO_PAGAMENTO,
    CANCELAMENTO_ORCAMENTO,
    CANCELAMENTO_EXECUCAO
}
