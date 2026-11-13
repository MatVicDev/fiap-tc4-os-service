package br.com.oficina.os.domain.mensageria;

/**
 * Catálogo de comandos e eventos da Saga. O prefixo indica o serviço dono: comandos
 * são roteados para o serviço do prefixo; eventos são publicados pelo serviço do prefixo.
 */
public final class TiposMensagem {

    private TiposMensagem() {
    }

    // Comandos enviados pelo orquestrador -> execution-service
    public static final String CMD_INICIAR_DIAGNOSTICO = "execution.iniciar-diagnostico";
    public static final String CMD_INICIAR_EXECUCAO = "execution.iniciar-execucao";
    public static final String CMD_CANCELAR_EXECUCAO = "execution.cancelar-execucao";

    // Comandos enviados pelo orquestrador -> billing-service
    public static final String CMD_GERAR_ORCAMENTO = "billing.gerar-orcamento";
    public static final String CMD_CANCELAR_ORCAMENTO = "billing.cancelar-orcamento";
    public static final String CMD_ESTORNAR_PAGAMENTO = "billing.estornar-pagamento";

    // Eventos publicados pelo execution-service
    public static final String EVT_DIAGNOSTICO_INICIADO = "execution.diagnostico-iniciado";
    public static final String EVT_DIAGNOSTICO_CONCLUIDO = "execution.diagnostico-concluido";
    public static final String EVT_EXECUCAO_INICIADA = "execution.execucao-iniciada";
    public static final String EVT_EXECUCAO_FALHOU = "execution.execucao-falhou";
    public static final String EVT_EXECUCAO_FINALIZADA = "execution.execucao-finalizada";
    public static final String EVT_EXECUCAO_CANCELADA = "execution.execucao-cancelada";

    // Eventos publicados pelo billing-service
    public static final String EVT_ORCAMENTO_GERADO = "billing.orcamento-gerado";
    public static final String EVT_ORCAMENTO_APROVADO = "billing.orcamento-aprovado";
    public static final String EVT_ORCAMENTO_REJEITADO = "billing.orcamento-rejeitado";
    public static final String EVT_ORCAMENTO_CANCELADO = "billing.orcamento-cancelado";
    public static final String EVT_PAGAMENTO_CONFIRMADO = "billing.pagamento-confirmado";
    public static final String EVT_PAGAMENTO_RECUSADO = "billing.pagamento-recusado";
    public static final String EVT_PAGAMENTO_ESTORNADO = "billing.pagamento-estornado";

    // Eventos publicados pelo os-service (consumo opcional por outros serviços/observabilidade)
    public static final String EVT_OS_STATUS_ALTERADO = "os.status-alterado";
}
