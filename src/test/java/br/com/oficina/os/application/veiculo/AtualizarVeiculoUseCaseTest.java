package br.com.oficina.os.application.veiculo;

import br.com.oficina.os.api.veiculo.dto.AtualizarVeiculoRequest;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do atualização dos veículos")
class AtualizarVeiculoUseCaseTest {

    @Mock
    private VeiculoRepository repository;

    @InjectMocks
    private AtualizarVeiculoUseCase useCase;

    @Test
    @DisplayName("Deve atualizar veículo com sucesso")
    void deveAtualizarVeiculoComSucesso() {
        Veiculo veiculo = new Veiculo(
                "ABC1D23",
                "Toyota",
                "Corolla",
                2020,
                "Prata",
                "12345678901");

        AtualizarVeiculoRequest request = new AtualizarVeiculoRequest(
                "Toyota",
                "Corolla",
                2021,
                "Branco");

        when(repository.buscarPorId(any())).thenReturn(Optional.of(veiculo));
        when(repository.salvar(any())).thenAnswer(i -> i.getArgument(0));

        VeiculoResponse response = useCase.executar(UUID.randomUUID(), request);

        assertNotNull(response);
        verify(repository, times(1)).salvar(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando veículo não encontrado")
    void deveLancarExcecaoQuandoVeiculoNaoEncontrado() {
        when(repository.buscarPorId(any())).thenReturn(Optional.empty());

        assertThrows(EntidadeNaoEncontradaException.class,
                () -> useCase.executar(UUID.randomUUID(),
                        new AtualizarVeiculoRequest(
                                "Toyota",
                                "Corolla",
                                2020,
                                "Prata")));

        verify(repository, never()).salvar(any());
    }
}