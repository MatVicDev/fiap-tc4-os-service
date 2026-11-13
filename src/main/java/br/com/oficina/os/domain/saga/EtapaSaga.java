package br.com.oficina.os.domain.saga;

/**
 * Etapas do fluxo transacional da OS, na ordem em que o orquestrador as percorre.
 */
public enum EtapaSaga {
    DIAGNOSTICO,
    ORCAMENTO,
    APROVACAO,
    PAGAMENTO,
    EXECUCAO,
    FINALIZADA
}
