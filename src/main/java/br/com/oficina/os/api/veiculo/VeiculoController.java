package br.com.oficina.os.api.veiculo;

import br.com.oficina.os.api.veiculo.dto.AtualizarVeiculoRequest;
import br.com.oficina.os.api.veiculo.dto.CadastrarVeiculoRequest;
import br.com.oficina.os.api.veiculo.dto.VeiculoResponse;
import br.com.oficina.os.application.veiculo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/veiculos")
@RequiredArgsConstructor
public class VeiculoController {
    private final CadastrarVeiculoUseCase cadastrarVeiculoUseCase;
    private final ListarVeiculosUseCase listarVeiculosUseCase;
    private final ListarVeiculosPorCpfProprietarioUseCase listarVeiculosPorCpfProprietarioUseCase;
    private final BuscarVeiculoUseCase buscarVeiculoUseCase;
    private final AtualizarVeiculoUseCase atualizarVeiculoUseCase;
    private final ExcluirVeiculoUseCase excluirVeiculoUseCase;

    @PostMapping
    public ResponseEntity<VeiculoResponse> cadastrar(@Valid @RequestBody CadastrarVeiculoRequest request) {
        return ResponseEntity.ok(cadastrarVeiculoUseCase.executar(request));
    }

    @GetMapping
    public ResponseEntity<List<VeiculoResponse>> listar() {
        return ResponseEntity.ok(listarVeiculosUseCase.executar());
    }

    @GetMapping("/proprietario/{cpf}")
    public ResponseEntity<List<VeiculoResponse>> listarPorProprietario(@PathVariable String cpf) {
        return ResponseEntity.ok(listarVeiculosPorCpfProprietarioUseCase.executar(cpf));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VeiculoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(buscarVeiculoUseCase.executar(id));
    }

    @GetMapping("/placa/{placa}")
    public ResponseEntity<VeiculoResponse> buscarPorPlaca(@PathVariable String placa) {
        return ResponseEntity.ok(buscarVeiculoUseCase.executar(placa));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VeiculoResponse> atualizar(@PathVariable UUID id, @RequestBody AtualizarVeiculoRequest request) {
        return ResponseEntity.ok(atualizarVeiculoUseCase.executar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        excluirVeiculoUseCase.executar(id);
        return ResponseEntity.noContent().build();
    }
}
