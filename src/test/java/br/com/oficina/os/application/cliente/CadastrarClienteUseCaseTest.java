package br.com.oficina.os.application.cliente;

import br.com.oficina.os.api.cliente.dto.CadastrarClienteRequest;
import br.com.oficina.os.api.cliente.dto.ClienteResponse;
import br.com.oficina.os.domain.cliente.Cliente;
import br.com.oficina.os.domain.cliente.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do CadastrarClienteUseCase")
class CadastrarClienteUseCaseTest {

    @Mock
    private ClienteRepository repository;

    @InjectMocks
    private CadastrarClienteUseCase useCase;

    @Test
    @DisplayName("Deve cadastrar cliente com sucesso")
    void deveCadastrarClienteComSucesso() {
        CadastrarClienteRequest request = new CadastrarClienteRequest(
                "Matheus Victor",
                "12345678909",
                "41999999999",
                "matheus@email.com");

        when(repository.salvar(any(Cliente.class)))
                .thenAnswer(i -> i.getArgument(0));

        ClienteResponse response = useCase.executar(request);

        assertNotNull(response);
        assertEquals("Matheus Victor", response.nome());
        verify(repository, times(1)).salvar(any(Cliente.class));
    }

    @Test
    @DisplayName("Deve lançar exceção para CPF inválido")
    void deveLancarExcecaoParaCpfInvalido() {
        CadastrarClienteRequest request = new CadastrarClienteRequest(
                "Matheus Victor",
                "123",
                "41999999999",
                "joao@email.com");

        assertThrows(Exception.class, () -> useCase.executar(request));
        verify(repository, never()).salvar(any());
    }
}