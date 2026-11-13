package br.com.oficina.os.api.ordemServico.dto;

import br.com.oficina.os.domain.ordemServico.StatusOS;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrdemServicoResponse(
        UUID id,
        UUID clienteId,
        UUID veiculoId,
        StatusOS status,
        String sintomaRelatado,
        List<ItemOSResponse> itens,
        BigDecimal valorTotal,
        String motivoCancelamento,
        LocalDateTime dataAbertura,
        LocalDateTime dataFinalizacao,
        LocalDateTime dataEntrega
) {
}
