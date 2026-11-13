package br.com.oficina.os.domain.mensageria;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Envelope único de todas as mensagens (comandos e eventos) trocadas entre os
 * microsserviços. O {@code tipo} é também a routing key no RabbitMQ; os dados vão
 * num mapa genérico para que cada serviço evolua seus contratos sem compartilhar
 * bibliotecas/classes — cada repositório é independente.
 */
public record Mensagem(
        UUID id,
        String tipo,
        UUID osId,
        Instant ocorridoEm,
        String correlationId,
        Map<String, Object> dados
) {

    public static Mensagem nova(String tipo, UUID osId, String correlationId, Map<String, Object> dados) {
        return new Mensagem(UUID.randomUUID(), tipo, osId, Instant.now(), correlationId, dados == null ? Map.of() : dados);
    }

    public String texto(String chave) {
        Object valor = dados == null ? null : dados.get(chave);
        return valor == null ? null : valor.toString();
    }

    public UUID uuid(String chave) {
        String valor = texto(chave);
        return valor == null || valor.isBlank() ? null : UUID.fromString(valor);
    }

    public BigDecimal decimal(String chave) {
        String valor = texto(chave);
        return valor == null ? null : new BigDecimal(valor);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> lista(String chave) {
        Object valor = dados == null ? null : dados.get(chave);
        return valor instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
    }
}
