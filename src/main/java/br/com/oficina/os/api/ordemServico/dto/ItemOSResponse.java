package br.com.oficina.os.api.ordemServico.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemOSResponse(
        String tipo,
        UUID referenciaId,
        String descricao,
        BigDecimal precoUnitario,
        Integer quantidade,
        BigDecimal precoTotal
) {
}
