package br.com.oficina.os.application.cliente;

import br.com.oficina.os.api.cliente.dto.ClienteResponse;
import br.com.oficina.os.domain.cliente.Cliente;
import br.com.oficina.os.domain.cliente.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes para listar clientes")
class ListarClientesUseCaseTest {

    @Mock
    private ClienteRepository repository;

    @InjectMocks
    private ListarClientesUseCase useCase;

    @Test
    @DisplayName("Deve listar todos os clientes")
    void deveListarTodosOsClientes() {
        List<Cliente> clientes = List.of(
                new Cliente("Matheus Victor", "12345678909", "41999999999", "matheus@email.com"),
                new Cliente("João Silva", "11144477735", "41988888888", "joao@email.com"));

        when(repository.listarTodos()).thenReturn(clientes);

        List<ClienteResponse> response = useCase.executar();

        assertEquals(2, response.size());
        verify(repository, times(1)).listarTodos();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há clientes")
    void deveRetornarListaVaziaQuandoNaoHaClientes() {
        when(repository.listarTodos()).thenReturn(List.of());

        List<ClienteResponse> response = useCase.executar();

        assertTrue(response.isEmpty());
    }
}