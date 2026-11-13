package br.com.oficina.os.api;

import br.com.oficina.os.application.saga.OrquestradorSagaOS;
import br.com.oficina.os.domain.mensageria.Mensagem;
import br.com.oficina.os.infrastructure.persistence.mensageria.OutboxMensagem;
import br.com.oficina.os.infrastructure.persistence.mensageria.OutboxMensagemJpa;
import br.com.oficina.os.infrastructure.security.JWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static br.com.oficina.os.domain.mensageria.TiposMensagem.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração da API com contexto Spring completo (H2 no lugar do Postgres;
 * RabbitMQ e SMTP substituídos por mocks): cobre segurança JWT, controllers, JPA,
 * outbox e o orquestrador trabalhando juntos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Testes de integração da API do os-service")
class OsServiceApiIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JsonMapper json;
    @Autowired
    private JWTService jwtService;
    @Autowired
    private OrquestradorSagaOS orquestrador;
    @Autowired
    private OutboxMensagemJpa outbox;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;
    @MockitoBean
    private JavaMailSender mailSender;

    private String token;

    @BeforeEach
    void login() throws Exception {
        MvcResult res = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"admin\",\"senha\":\"senha-teste\"}"))
                .andExpect(status().isOk()).andReturn();
        token = "Bearer " + res.getResponse().getContentAsString();
    }

    @Test
    @DisplayName("Rejeita requisições sem token e login com senha errada")
    void seguranca() throws Exception {
        mvc.perform(get("/ordens-servico")).andExpect(status().isForbidden());
        mvc.perform(get("/ordens-servico").header("Authorization", "Bearer token-invalido")).andExpect(status().isForbidden());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"admin\",\"senha\":\"errada\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token de CLIENTE emitido pela Lambda de CPF é aceito")
    void tokenCliente() throws Exception {
        String tokenCliente = "Bearer " + jwtService.gerarToken("12345678909", "CLIENTE");
        mvc.perform(get("/ordens-servico").header("Authorization", tokenCliente)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Fluxo completo via API: cadastro, abertura da OS, eventos da Saga, consulta, histórico e entrega")
    void fluxoCompleto() throws Exception {
        String cpf = "52998224725";
        String placa = "BRA2E19";
        String clienteId = cadastrarCliente(cpf);
        cadastrarVeiculo(placa, cpf);

        MvcResult aberta = mvc.perform(post("/ordens-servico").header("Authorization", token)
                        .header("X-Correlation-Id", "corr-integracao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpfCliente\":\"" + cpf + "\",\"placaVeiculo\":\"" + placa + "\",\"sintomaRelatado\":\"Motor falhando\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEBIDA"))
                .andReturn();
        UUID osId = UUID.fromString(json.readTree(aberta.getResponse().getContentAsString()).get("id").asString());

        List<OutboxMensagem> pendentes = outbox.findAll().stream()
                .filter(m -> m.getPayload().contains(osId.toString())).toList();
        assertThat(pendentes).extracting(OutboxMensagem::getRoutingKey).contains(CMD_INICIAR_DIAGNOSTICO);
        assertThat(pendentes).allMatch(m -> m.getPublicadaEm() == null);

        UUID orcamentoId = UUID.randomUUID();
        UUID pagamentoId = UUID.randomUUID();
        processar(osId, EVT_DIAGNOSTICO_INICIADO, Map.of());
        processar(osId, EVT_DIAGNOSTICO_CONCLUIDO, Map.of("itens", List.of(Map.of(
                "tipo", "SERVICO", "referenciaId", UUID.randomUUID().toString(), "descricao", "Limpeza de bicos",
                "precoUnitario", "200.00", "quantidade", 1))));
        processar(osId, EVT_ORCAMENTO_GERADO, Map.of("orcamentoId", orcamentoId.toString()));
        processar(osId, EVT_ORCAMENTO_APROVADO, Map.of("orcamentoId", orcamentoId.toString(), "linkPagamento", "https://mp"));
        processar(osId, EVT_PAGAMENTO_CONFIRMADO, Map.of("pagamentoId", pagamentoId.toString()));
        processar(osId, EVT_EXECUCAO_INICIADA, Map.of());

        mvc.perform(get("/ordens-servico").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + osId + "')].status").value("EM_EXECUCAO"));

        processar(osId, EVT_EXECUCAO_FINALIZADA, Map.of());

        mvc.perform(get("/ordens-servico/" + osId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.valorTotal").value(200.0))
                .andExpect(jsonPath("$.itens[0].descricao").value("Limpeza de bicos"));

        mvc.perform(get("/ordens-servico/" + osId + "/historico").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7));

        mvc.perform(get("/ordens-servico/" + osId + "/saga").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.orcamentoId").value(orcamentoId.toString()))
                .andExpect(jsonPath("$.pagamentoId").value(pagamentoId.toString()));

        mvc.perform(get("/ordens-servico/cliente/" + clienteId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(osId.toString()));

        mvc.perform(patch("/ordens-servico/" + osId + "/entrega").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGUE"));
    }

    @Test
    @DisplayName("Cancelamento via API dispara compensação; segundo cancelamento retorna 422")
    void cancelamento() throws Exception {
        String cpf = "11144477735";
        String placa = "XYZ9A88";
        cadastrarCliente(cpf);
        cadastrarVeiculo(placa, cpf);
        MvcResult aberta = mvc.perform(post("/ordens-servico").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpfCliente\":\"" + cpf + "\",\"placaVeiculo\":\"" + placa + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        String osId = json.readTree(aberta.getResponse().getContentAsString()).get("id").asString();

        mvc.perform(post("/ordens-servico/" + osId + "/cancelar").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Desistência\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));

        mvc.perform(post("/ordens-servico/" + osId + "/cancelar").header("Authorization", token))
                .andExpect(status().isUnprocessableContent());

        mvc.perform(get("/ordens-servico/" + osId + "/saga").header("Authorization", token))
                .andExpect(jsonPath("$.status").value("COMPENSANDO"))
                .andExpect(jsonPath("$.compensacoesPendentes[0]").value("CANCELAMENTO_EXECUCAO"));
    }

    @Test
    @DisplayName("Erros: OS inexistente (404), CPF inválido (400), CPF duplicado (409), cliente sem veículo (404)")
    void erros() throws Exception {
        mvc.perform(get("/ordens-servico/" + UUID.randomUUID()).header("Authorization", token))
                .andExpect(status().isNotFound());
        mvc.perform(get("/ordens-servico/" + UUID.randomUUID() + "/saga").header("Authorization", token))
                .andExpect(status().isNotFound());
        mvc.perform(get("/clientes/cpf/123").header("Authorization", token))
                .andExpect(status().isBadRequest());

        cadastrarCliente("39053344705");
        mvc.perform(post("/clientes").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Outro Nome\",\"cpf\":\"39053344705\",\"telefone\":\"41988887777\"}"))
                .andExpect(status().is4xxClientError());

        mvc.perform(post("/ordens-servico").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpfCliente\":\"39053344705\",\"placaVeiculo\":\"AAA1B11\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CRUD de clientes e veículos")
    void crudClientesVeiculos() throws Exception {
        String clienteId = cadastrarCliente("71428793860");
        String veiculoId = cadastrarVeiculo("QWE4R56", "71428793860");

        mvc.perform(get("/clientes").header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(get("/clientes/" + clienteId).header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(get("/clientes/cpf/71428793860").header("Authorization", token))
                .andExpect(jsonPath("$.id").value(clienteId));
        mvc.perform(put("/clientes/" + clienteId).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"41911112222\",\"email\":\"novo@email.com\"}"))
                .andExpect(jsonPath("$.email").value("novo@email.com"));

        mvc.perform(get("/veiculos").header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(get("/veiculos/" + veiculoId).header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(get("/veiculos/placa/QWE4R56").header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(get("/veiculos/proprietario/71428793860").header("Authorization", token))
                .andExpect(jsonPath("$[0].id").value(veiculoId));
        mvc.perform(put("/veiculos/" + veiculoId).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cor\":\"Azul\"}"))
                .andExpect(jsonPath("$.cor").value("Azul"));

        mvc.perform(delete("/veiculos/" + veiculoId).header("Authorization", token)).andExpect(status().isNoContent());
        mvc.perform(delete("/clientes/" + clienteId).header("Authorization", token)).andExpect(status().isNoContent());
        mvc.perform(get("/clientes/" + clienteId).header("Authorization", token)).andExpect(status().isNotFound());
    }

    private void processar(UUID osId, String tipo, Map<String, Object> dados) {
        orquestrador.processar(Mensagem.nova(tipo, osId, "corr-integracao", dados));
    }

    private String cadastrarCliente(String cpf) throws Exception {
        MvcResult res = mvc.perform(post("/clientes").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Cliente Teste\",\"cpf\":\"" + cpf + "\",\"telefone\":\"41999998888\",\"email\":\"cliente@email.com\"}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(res.getResponse().getContentAsString());
        return body.get("id").asString();
    }

    private String cadastrarVeiculo(String placa, String cpf) throws Exception {
        MvcResult res = mvc.perform(post("/veiculos").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marca\":\"VW\",\"modelo\":\"Gol\",\"ano\":2020,\"cor\":\"Branco\",\"placa\":\"" + placa
                                + "\",\"cpfProprietario\":\"" + cpf + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("id").asString();
    }
}
