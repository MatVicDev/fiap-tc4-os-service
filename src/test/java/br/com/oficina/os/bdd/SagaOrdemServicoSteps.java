package br.com.oficina.os.bdd;

import br.com.oficina.os.domain.ordemServico.HistoricoStatusOS;
import br.com.oficina.os.support.CenarioSaga;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static br.com.oficina.os.domain.mensageria.TiposMensagem.*;
import static org.assertj.core.api.Assertions.assertThat;

public class SagaOrdemServicoSteps {

    private CenarioSaga c;

    @Dado("um cliente cadastrado com um veículo")
    public void umClienteCadastradoComUmVeiculo() {
        c = new CenarioSaga();
    }

    @E("que o atendente abriu uma ordem de serviço")
    public void queOAtendenteAbriuUmaOrdemDeServico() {
        c.abrirOs();
    }

    @Dado("que a OS está aguardando aprovação do orçamento")
    public void queAOsEstaAguardandoAprovacao() {
        c.receber(EVT_DIAGNOSTICO_INICIADO);
        c.receber(EVT_DIAGNOSTICO_CONCLUIDO, Map.of("itens", c.itensDiagnostico()));
        publicar(EVT_ORCAMENTO_GERADO);
    }

    @Dado("que a OS está com o pagamento confirmado")
    public void queAOsEstaComOPagamentoConfirmado() {
        queAOsEstaAguardandoAprovacao();
        publicar(EVT_ORCAMENTO_APROVADO);
        publicar(EVT_PAGAMENTO_CONFIRMADO);
    }

    @Quando("o execution-service publicar {string}")
    public void oExecutionServicePublicar(String tipo) {
        publicar(tipo);
    }

    @Quando("o billing-service publicar {string}")
    public void oBillingServicePublicar(String tipo) {
        publicar(tipo);
    }

    @Quando("o execution-service publicar {string} com motivo {string}")
    public void oExecutionServicePublicarComMotivo(String tipo, String motivo) {
        c.receber(tipo, Map.of("motivo", motivo));
    }

    @Quando("o billing-service publicar {string} com motivo {string}")
    public void oBillingServicePublicarComMotivo(String tipo, String motivo) {
        c.receber(tipo, Map.of("motivo", motivo));
    }

    @Quando("o execution-service concluir o diagnóstico com itens no valor de {string}")
    public void concluirDiagnostico(String valor) {
        c.receber(EVT_DIAGNOSTICO_CONCLUIDO, Map.of("itens", c.itensDiagnostico()));
        assertThat(c.os.getValorTotal()).isEqualByComparingTo(new BigDecimal(valor));
    }

    @Quando("se passarem {int} horas sem pagamento")
    public void sePassaremHoras(int horas) {
        c.clock.avancar(Duration.ofHours(horas));
        c.orquestrador.expirarSagasVencidas();
    }

    @Então("a OS deve estar com status {string}")
    public void aOsDeveEstarComStatus(String status) {
        assertThat(c.os.getStatus().name()).isEqualTo(status);
    }

    @Então("a Saga deve estar com status {string}")
    public void aSagaDeveEstarComStatus(String status) {
        assertThat(c.saga().getStatus().name()).isEqualTo(status);
    }

    @Então("o comando {string} deve ser enviado")
    public void oComandoDeveSerEnviado(String tipo) {
        assertThat(c.publicador.tiposComandos()).contains(tipo);
    }

    @Então("o comando {string} não deve ser enviado")
    public void oComandoNaoDeveSerEnviado(String tipo) {
        assertThat(c.publicador.tiposComandos()).doesNotContain(tipo);
    }

    @Então("o motivo do cancelamento deve conter {string}")
    public void oMotivoDoCancelamentoDeveConter(String trecho) {
        assertThat(c.os.getMotivoCancelamento()).contains(trecho);
    }

    @Então("o histórico da OS deve conter os status:")
    public void oHistoricoDaOsDeveConterOsStatus(List<String> status) {
        assertThat(c.os.getHistorico()).extracting(HistoricoStatusOS::getStatus)
                .extracting(Enum::name)
                .containsExactlyElementsOf(status);
    }

    private void publicar(String tipo) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("orcamentoId", c.orcamentoId.toString());
        dados.put("pagamentoId", c.pagamentoId.toString());
        dados.put("linkPagamento", "https://www.mercadopago.com.br/checkout/teste");
        c.receber(tipo, dados);
    }
}
