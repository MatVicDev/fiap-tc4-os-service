package br.com.oficina.os.domain.veiculo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Testes do Value Object Placa")
class PlacaTest {

    @Test
    @DisplayName("Deve criar placa padrão Mercosul válida")
    void deveCriarPlacaMercosulValida() {
        Placa placa = new Placa("ABC1D23");
        assertEquals("ABC1D23", placa.getNumero());
    }

    @Test
    @DisplayName("Deve criar placa padrão antigo válida")
    void deveCriarPlacaAntigoValida() {
        Placa placa = new Placa("ABC1234");
        assertEquals("ABC1234", placa.getNumero());
    }

    @Test
    @DisplayName("Deve converter placa para maiúsculo")
    void deveConverterPlacaParaMaiusculo() {
        Placa placa = new Placa("abc1d23");
        assertEquals("ABC1D23", placa.getNumero());
    }

    @Test
    @DisplayName("Deve lançar exceção para placa nula")
    void deveLancarExcecaoParaPlacaNula() {
        assertThrows(IllegalArgumentException.class, () -> new Placa(null));
    }

    @Test
    @DisplayName("Deve lançar exceção para placa inválida")
    void deveLancarExcecaoParaPlacaInvalida() {
        assertThrows(IllegalArgumentException.class, () -> new Placa("INVALIDA"));
    }
}