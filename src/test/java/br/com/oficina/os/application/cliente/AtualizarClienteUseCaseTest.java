package br.com.oficina.os.application.cliente;

import br.com.oficina.os.api.cliente.dto.AtualizarClienteRequest;
import br.com.oficina.os.api.cliente.dto.ClienteResponse;
import br.com.oficina.os.domain.cliente.Cliente;
import br.com.oficina.os.domain.cliente.ClienteRepository;
import br.com.oficina.os.domain.exception.EntidadeNaoEncontradaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes para atualizar clientes")
class AtualizarClienteUseCaseTest {

    @Mock
    private ClienteRepository repository;

    @InjectMocks
    private AtualizarClienteUseCase useCase;

    @Test
    @DisplayName("Deve atualizar contatos do cliente com sucesso")
    void deveAtualizarContatosDoClienteComSucesso() {
        Cliente cliente = new Cliente(
                "Matheus Victor",
                "12345678909",
                "41999999999",
                "matheus@email.com");

        AtualizarClienteRequest request = new AtualizarClienteRequest(
                "41988888888",
                "matheus.novo@email.com");

        when(repository.buscarPorId(any())).thenReturn(Optional.of(cliente));
        when(repository.salvar(any())).thenAnswer(i -> i.getArgument(0));

        ClienteResponse response = useCase.executar(UUID.randomUUID(), request);

        assertNotNull(response);
        assertEquals("41988888888", response.telefone());
        verify(repository, times(1)).salvar(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando cliente não for encontrado")
    void deveLancarExcecaoQuandoClienteNaoEncontrado() {
        when(repository.buscarPorId(any())).thenReturn(Optional.empty());

        assertThrows(EntidadeNaoEncontradaException.class,
                () -> useCase.executar(UUID.randomUUID(),
                        new AtualizarClienteRequest("41988888888", "email@email.com")));

        verify(repository, never()).salvar(any());
    }
}