package br.com.oficina.os.application.veiculo;

import br.com.oficina.os.api.veiculo.dto.CadastrarVeiculoRequest;
import br.com.oficina.os.api.veiculo.dto.VeiculoResponse;
import br.com.oficina.os.domain.veiculo.Veiculo;
import br.com.oficina.os.domain.veiculo.VeiculoRepository;
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
@DisplayName("Testes do CadastrarVeiculoUseCase")
class CadastrarVeiculoUseCaseTest {

    @Mock
    private VeiculoRepository repository;

    @InjectMocks
    private CadastrarVeiculoUseCase useCase;

    @Test
    @DisplayName("Deve cadastrar veículo com sucesso")
    void deveCadastrarVeiculoComSucesso() {
        CadastrarVeiculoRequest request = new CadastrarVeiculoRequest(
                "Toyota",
                "Corolla",
                2020,
                "Prata",
                "ABC1D23",
                "12345678901");

        when(repository.salvar(any(Veiculo.class)))
                .thenAnswer(i -> i.getArgument(0));

        VeiculoResponse reponse = useCase.executar(request);

        assertNotNull(reponse);
        assertEquals("Toyota", reponse.marca());
        assertEquals("ABC1D23", reponse.placa());
        verify(repository, times(1)).salvar(any(Veiculo.class));
    }

    @Test
    @DisplayName("Deve lançar exceção para placa inválida")
    void deveLancarExcecaoParaPlacaInvalida() {
        CadastrarVeiculoRequest request = new CadastrarVeiculoRequest(
                "Toyota", "Corolla", 2020, "Prata", "INVALIDA", "12345678901");

        assertThrows(Exception.class, () -> useCase.executar(request));
        verify(repository, never()).salvar(any());
    }
}