package br.com.oficina.os.domain.veiculo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes da entidade Veiculo")
class VeiculoTest {

    @Test
    @DisplayName("Deve criar veículo com sucesso")
    void deveCriarVeiculoComSucesso() {
        Veiculo veiculo = new Veiculo(
                "ABC1D23",
                "Toyota",
                "Corolla",
                2020,
                "Prata",
                "12345678901");

        assertNotNull(veiculo.getId());
        assertEquals("ABC1D23", veiculo.getPlaca().getNumero());
        assertEquals("Toyota", veiculo.getMarca());
        assertEquals(2020, veiculo.getAno());
    }

    @Test
    @DisplayName("Deve lançar exceção para ano inválido")
    void deveLancarExcecaoParaAnoInvalido() {
        assertThrows(IllegalArgumentException.class, () ->
                new Veiculo(
                        "ABC1D23",
                        "Toyota",
                        "Corolla",
                        1800,
                        "Prata",
                        "12345678901"));
    }

    @Test
    @DisplayName("Deve lançar exceção para placa inválida")
    void deveLancarExcecaoParaPlacaInvalida() {
        assertThrows(IllegalArgumentException.class, () ->
                new Veiculo(
                        "INVALIDA",
                        "Toyota",
                        "Corolla",
                        2020,
                        "Prata",
                        "12345678901"));
    }

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

        veiculo.atualizar("Honda", "Civic", 2022, "Preto");

        assertEquals("Honda", veiculo.getMarca());
        assertEquals("Civic", veiculo.getModelo());
        assertEquals(2022, veiculo.getAno());
        assertEquals("Preto", veiculo.getCor());
    }
}