package br.com.oficina.os.application.veiculo;

import br.com.oficina.os.api.veiculo.dto.VeiculoResponse;
import br.com.oficina.os.domain.exception.EntidadeNaoEncontradaException;
import br.com.oficina.os.domain.veiculo.Veiculo;
import br.com.oficina.os.domain.veiculo.VeiculoRepository;
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
@DisplayName("Testes para buscar os veículos")
class BuscarVeiculoUseCaseTest {

    @Mock
    private VeiculoRepository repository;

    @InjectMocks
    private BuscarVeiculoUseCase useCase;

    @Test
    @DisplayName("Deve buscar veículo por ID com sucesso")
    void deveBuscarVeiculoPorIdComSucesso() {
        Veiculo veiculo = new Veiculo(
                "ABC1D23",
                "Toyota",
                "Corolla",
                2020,
                "Prata",
                "12345678901");

        when(repository.buscarPorId(any())).thenReturn(Optional.of(veiculo));

        VeiculoResponse response = useCase.executar(UUID.randomUUID());

        assertNotNull(response);
        assertEquals("Toyota", response.marca());
        assertEquals("ABC1D23", response.placa());
    }

    @Test
    @DisplayName("Deve buscar veículo por placa com sucesso")
    void deveBuscarVeiculoPorPlacaComSucesso() {
        Veiculo veiculo = new Veiculo(
                "ABC1D23",
                "Toyota",
                "Corolla",
                2020,
                "Prata",
                "12345678901");

        when(repository.buscarPorPlaca(any())).thenReturn(Optional.of(veiculo));

        VeiculoResponse response = useCase.executar("ABC1D23");

        assertNotNull(response);
        assertEquals("Toyota", response.marca());
    }

    @Test
    @DisplayName("Deve lançar exceção quando veículo não for encontrado")
    void deveLancarExcecaoQuandoVeiculoNaoEncontrado() {
        when(repository.buscarPorId(any())).thenReturn(Optional.empty());

        assertThrows(EntidadeNaoEncontradaException.class, () -> useCase.executar(UUID.randomUUID()));
    }
}