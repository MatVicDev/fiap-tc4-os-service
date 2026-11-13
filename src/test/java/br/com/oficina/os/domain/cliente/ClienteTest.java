package br.com.oficina.os.domain.cliente;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes da entidade Cliente")
class ClienteTest {

    @Test
    @DisplayName("Deve criar cliente com sucesso")
    void deveCriarClienteComSucesso() {
        Cliente cliente = new Cliente(
                "Matheus Victor",
                "12345678909",
                "41999999999",
                "matheus@email.com");

        assertNotNull(cliente.getId());
        assertEquals("Matheus Victor", cliente.getNome());
        assertEquals("12345678909", cliente.getCpfNumero());
        assertNotNull(cliente.getDataCadastro());
        assertEquals(StatusCliente.ATIVO, cliente.getStatus());
        assertTrue(cliente.isAtivo());
    }

    @Test
    @DisplayName("Deve inativar e reativar cliente")
    void deveInativarEReativarCliente() {
        Cliente cliente = new Cliente(
                "Matheus Victor",
                "12345678909",
                "41999999999",
                "matheus@email.com");

        cliente.inativar();
        assertEquals(StatusCliente.INATIVO, cliente.getStatus());
        assertFalse(cliente.isAtivo());

        cliente.ativar();
        assertEquals(StatusCliente.ATIVO, cliente.getStatus());
        assertTrue(cliente.isAtivo());
    }

    @Test
    @DisplayName("Deve lançar exceção para nome muito curto")
    void deveLancarExcecaoParaNomeMuitoCurto() {
        assertThrows(IllegalArgumentException.class, () ->
                new Cliente("AB",
                        "12345678901",
                        "41999999999",
                        "matheus@email.com"));
    }

    @Test
    @DisplayName("Deve lançar exceção para nome nulo")
    void deveLancarExcecaoParaNomeNulo() {
        assertThrows(IllegalArgumentException.class, () ->
                new Cliente(
                        null,
                        "12345678901",
                        "41999999999",
                        "matheus@email.com"));
    }

    @Test
    @DisplayName("Deve atualizar contatos com sucesso")
    void deveAtualizarContatosComSucesso() {
        Cliente cliente = new Cliente(
                "Matheus Victor",
                "12345678909",
                "41999999999",
                "matheus@email.com");

        cliente.atualizarContatos("41988888888", "novo@email.com");

        assertEquals("41988888888", cliente.getTelefone());
        assertEquals("novo@email.com", cliente.getEmail());
    }

    @Test
    @DisplayName("Não deve atualizar contatos com valores nulos")
    void naoDeveAtualizarContatosComValoresNulos() {
        Cliente cliente = new Cliente(
                "Matheus Victor",
                "12345678909",
                "41999999999",
                "matheus@email.com");

        cliente.atualizarContatos(null, null);

        assertEquals("41999999999", cliente.getTelefone());
        assertEquals("matheus@email.com", cliente.getEmail());
    }
}