package br.com.oficina.os.domain.cliente;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Testes do Value Object Cpf")
class CpfTest {

    @Test
    @DisplayName("Deve lançar exceção para CPF nulo")
    void deveLancarExcecaoParaCpfNulo() {
        assertThrows(IllegalArgumentException.class, () -> new Cpf(null));
    }

    @Test
    @DisplayName("Deve lançar exceção para CPF com menos de 11 dígitos")
    void deveLancarExcecaoParaCpfCurto() {
        assertThrows(IllegalArgumentException.class, () -> new Cpf("1234567890"));
    }

    @Test
    @DisplayName("Deve lançar exceção para CPF com mais de 11 dígitos")
    void deveLancarExcecaoParaCpfLongo() {
        assertThrows(IllegalArgumentException.class, () -> new Cpf("123456789012"));
    }

    @Test
    @DisplayName("Deve aceitar CNPJ válido")
    void deveAceitarCnpjValido() {
        Cpf cnpj = new Cpf("11222333000181");
        assertEquals("11222333000181", cnpj.getNumero());
    }

    @Test
    @DisplayName("Deve rejeitar CPF com todos os dígitos iguais")
    void deveRejeitarCpfComDigitosIguais() {
        assertThrows(IllegalArgumentException.class, () -> new Cpf("11111111111"));
    }

    @Test
    @DisplayName("Deve rejeitar CPF com dígitos verificadores inválidos")
    void deveRejeitarCpfComDigitosVerificadoresInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new Cpf("12345678900"));
    }
}