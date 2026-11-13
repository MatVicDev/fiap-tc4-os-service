package br.com.oficina.os.application.veiculo;

import br.com.oficina.os.api.veiculo.dto.VeiculoResponse;
import br.com.oficina.os.domain.veiculo.Veiculo;
import br.com.oficina.os.domain.veiculo.VeiculoRepository;
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
@DisplayName("Testes de listagem de veículos")
class ListarVeiculosUseCaseTest {

    @Mock
    private VeiculoRepository repository;

    @InjectMocks
    private ListarVeiculosUseCase useCase;

    @Test
    @DisplayName("Deve listar todos os veículos")
    void deveListarTodosOsVeiculos() {
        List<Veiculo> veiculos = List.of(
                new Veiculo("ABC1D23", "Toyota", "Corolla", 2020, "Prata", "12345678901"),
                new Veiculo("XYZ9K87", "Honda", "Civic", 2021, "Preto", "98765432100"));

        when(repository.listarTodos()).thenReturn(veiculos);

        List<VeiculoResponse> response = useCase.executar();

        assertEquals(2, response.size());
        verify(repository, times(1)).listarTodos();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há veículos")
    void deveRetornarListaVaziaQuandoNaoHaVeiculos() {
        when(repository.listarTodos()).thenReturn(List.of());

        List<VeiculoResponse> response = useCase.executar();

        assertTrue(response.isEmpty());
    }
}