package br.com.oficina.os.api.cliente;

import br.com.oficina.os.api.cliente.dto.AtualizarClienteRequest;
import br.com.oficina.os.api.cliente.dto.CadastrarClienteRequest;
import br.com.oficina.os.api.cliente.dto.ClienteResponse;
import br.com.oficina.os.application.cliente.*;
import br.com.oficina.os.domain.cliente.Cpf;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {
    private final CadastrarClienteUseCase cadastrarClienteUseCase;
    private final ListarClientesUseCase listarClientesUseCase;
    private final BuscarClienteUseCase buscarClienteUseCase;
    private final AtualizarClienteUseCase atualizarClienteUseCase;
    private final ExcluirClienteUseCase excluirClienteUseCase;

    @PostMapping
    public ResponseEntity<ClienteResponse> cadastrar(@Valid @RequestBody CadastrarClienteRequest request) {
        return ResponseEntity.ok(cadastrarClienteUseCase.executar(request));
    }

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listar() {
        return ResponseEntity.ok(listarClientesUseCase.executar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(buscarClienteUseCase.executar(id));
    }

    @GetMapping("/cpf/{cpf}")
    public ResponseEntity<ClienteResponse> buscarPorCpf(@PathVariable String cpf) {
        return ResponseEntity.ok(buscarClienteUseCase.executar(new Cpf(cpf)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponse> atualizar(@PathVariable UUID id, @RequestBody AtualizarClienteRequest request) {
        return ResponseEntity.ok(atualizarClienteUseCase.executar(id,request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@Valid @PathVariable UUID id) {
        excluirClienteUseCase.executar(id);
        return ResponseEntity.noContent().build();
    }
}
