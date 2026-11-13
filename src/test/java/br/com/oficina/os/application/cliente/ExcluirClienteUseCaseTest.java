package br.com.oficina.os.application.cliente;

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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do ExcluirClienteUseCase")
class ExcluirClienteUseCaseTest {

    @Mock
    private ClienteRepository repository;

    @InjectMocks
    private ExcluirClienteUseCase useCase;

    @Test
    @DisplayName("Deve excluir cliente com sucesso")
    void deveExcluirClienteComSucesso() {
        Cliente cliente = new Cliente(
                "Matheus Victor",
                "12345678909",
                "41999999999",
                "matheus@email.com");

        when(repository.buscarPorId(any())).thenReturn(Optional.of(cliente));

        useCase.executar(UUID.randomUUID());

        verify(repository, times(1)).excluir(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando cliente não for encontrado")
    void deveLancarExcecaoQuandoClienteNaoEncontrado() {
        when(repository.buscarPorId(any())).thenReturn(Optional.empty());

        assertThrows(EntidadeNaoEncontradaException.class, () -> useCase.executar(UUID.randomUUID()));

        verify(repository, never()).excluir(any());
    }
}