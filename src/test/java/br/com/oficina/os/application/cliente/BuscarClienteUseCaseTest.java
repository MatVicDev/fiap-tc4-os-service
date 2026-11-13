package br.com.oficina.os.application.cliente;

import br.com.oficina.os.api.cliente.dto.ClienteResponse;
import br.com.oficina.os.domain.cliente.Cliente;
import br.com.oficina.os.domain.cliente.ClienteRepository;
import br.com.oficina.os.domain.cliente.Cpf;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do BuscarClienteUseCase")
class BuscarClienteUseCaseTest {

    @Mock
    private ClienteRepository repository;

    @InjectMocks
    private BuscarClienteUseCase useCase;

    @Test
    @DisplayName("Deve buscar cliente por ID com sucesso")
    void deveBuscarClientePorIdComSucesso() {
        Cliente cliente = new Cliente(
                "Matheus Victor",
                "12345678909",
                "41999999999",
                "matheus@email.com");

        when(repository.buscarPorId(any())).thenReturn(Optional.of(cliente));

        ClienteResponse response = useCase.executar(UUID.randomUUID());

        assertNotNull(response);
        assertEquals("Matheus Victor", response.nome());
    }

    @Test
    @DisplayName("Deve buscar cliente por CPF com sucesso")
    void deveBuscarClientePorCpfComSucesso() {
        Cliente cliente = new Cliente(
                "Matheus Victor",
                "12345678909",
                "41999999999",
                "matheus@email.com");

        when(repository.buscarPorCpf(any())).thenReturn(Optional.of(cliente));

        ClienteResponse response = useCase.executar(new Cpf("12345678909"));

        assertNotNull(response);
        assertEquals("Matheus Victor", response.nome());
    }

    @Test
    @DisplayName("Deve lançar exceção quando cliente não for encontrado")
    void deveLancarExcecaoQuandoClienteNaoEncontrado() {
        when(repository.buscarPorId(any())).thenReturn(Optional.empty());

        assertThrows(EntidadeNaoEncontradaException.class,
                () -> useCase.executar(UUID.randomUUID()));
    }
}