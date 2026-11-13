package br.com.oficina.os.api.ordemServico;

import br.com.oficina.os.api.ordemServico.dto.*;
import br.com.oficina.os.application.ordemServico.AbrirOrdemServicoUseCase;
import br.com.oficina.os.application.ordemServico.CancelarOrdemServicoUseCase;
import br.com.oficina.os.application.ordemServico.ConsultarOrdemServicoUseCase;
import br.com.oficina.os.application.ordemServico.RegistrarEntregaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Ordens de Serviço", description = "Abertura, acompanhamento, histórico e Saga da OS")
@RestController
@RequestMapping("/ordens-servico")
@RequiredArgsConstructor
public class OrdemServicoController {
    private final AbrirOrdemServicoUseCase abrirOrdemServicoUseCase;
    private final ConsultarOrdemServicoUseCase consultarOrdemServicoUseCase;
    private final CancelarOrdemServicoUseCase cancelarOrdemServicoUseCase;
    private final RegistrarEntregaUseCase registrarEntregaUseCase;

    @Operation(summary = "Abre uma OS e inicia a Saga (envia o veículo para a fila de diagnóstico)")
    @PostMapping
    public ResponseEntity<OrdemServicoResponse> abrir(@Valid @RequestBody AbrirOrdemServicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(abrirOrdemServicoUseCase.executar(request));
    }

    @Operation(summary = "Lista as OS em andamento, ordenadas por proximidade da conclusão")
    @GetMapping
    public ResponseEntity<List<OrdemServicoResponse>> listar() {
        return ResponseEntity.ok(consultarOrdemServicoUseCase.listarEmAndamento());
    }

    @Operation(summary = "Lista todas as OS de um cliente (mais recentes primeiro)")
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<OrdemServicoResponse>> listarPorCliente(@PathVariable UUID clienteId) {
        return ResponseEntity.ok(consultarOrdemServicoUseCase.listarPorCliente(clienteId));
    }

    @Operation(summary = "Consulta o status atual da OS")
    @GetMapping("/{id}")
    public ResponseEntity<OrdemServicoResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(consultarOrdemServicoUseCase.buscar(id));
    }

    @Operation(summary = "Histórico de mudanças de status da OS")
    @GetMapping("/{id}/historico")
    public ResponseEntity<List<HistoricoResponse>> historico(@PathVariable UUID id) {
        return ResponseEntity.ok(consultarOrdemServicoUseCase.historico(id));
    }

    @Operation(summary = "Estado e log de eventos da Saga da OS (rastreamento do fluxo distribuído)")
    @GetMapping("/{id}/saga")
    public ResponseEntity<SagaResponse> saga(@PathVariable UUID id) {
        return ResponseEntity.ok(consultarOrdemServicoUseCase.saga(id));
    }

    @Operation(summary = "Cancela a OS e dispara as compensações da Saga")
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<OrdemServicoResponse> cancelar(@PathVariable UUID id,
                                                         @Valid @RequestBody(required = false) CancelarOrdemServicoRequest request) {
        return ResponseEntity.ok(cancelarOrdemServicoUseCase.executar(id, request == null ? null : request.motivo()));
    }

    @Operation(summary = "Registra a entrega do veículo ao cliente")
    @PatchMapping("/{id}/entrega")
    public ResponseEntity<OrdemServicoResponse> registrarEntrega(@PathVariable UUID id) {
        return ResponseEntity.ok(registrarEntregaUseCase.executar(id));
    }
}
