package br.com.oficina.os.application.saga;

import br.com.oficina.os.domain.ordemServico.ItemOS;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Conversão entre {@link ItemOS} e a representação de item usada nos contratos de
 * mensagem ({tipo, referenciaId, descricao, precoUnitario, quantidade}).
 */
final class ItemMensagem {

    private ItemMensagem() {
    }

    static ItemOS paraItemOS(Map<String, Object> item) {
        return new ItemOS(
                ItemOS.TipoItem.valueOf(String.valueOf(item.get("tipo"))),
                UUID.fromString(String.valueOf(item.get("referenciaId"))),
                String.valueOf(item.get("descricao")),
                new BigDecimal(String.valueOf(item.get("precoUnitario"))),
                Integer.valueOf(String.valueOf(item.get("quantidade"))));
    }

    static Map<String, Object> deItemOS(ItemOS item) {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("tipo", item.getTipo().name());
        mapa.put("referenciaId", item.getReferenciaId().toString());
        mapa.put("descricao", item.getDescricao());
        mapa.put("precoUnitario", item.getPrecoUnitario().toPlainString());
        mapa.put("quantidade", item.getQuantidade());
        return mapa;
    }
}
