package br.com.oficina.os.application.ordemServico;

import br.com.oficina.os.api.ordemServico.dto.OrdemServicoResponse;
import br.com.oficina.os.application.saga.OrquestradorSagaOS;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CancelarOrdemServicoUseCase {
    private final OrquestradorSagaOS orquestrador;

    public OrdemServicoResponse executar(UUID id, String motivo) {
        return OrdemServicoMapper.toResponse(orquestrador.cancelarPorSolicitacao(id, motivo));
    }
}
