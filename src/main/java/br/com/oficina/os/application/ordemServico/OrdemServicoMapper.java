package br.com.oficina.os.application.ordemServico;

import br.com.oficina.os.api.ordemServico.dto.ItemOSResponse;
import br.com.oficina.os.api.ordemServico.dto.OrdemServicoResponse;
import br.com.oficina.os.api.ordemServico.dto.SagaResponse;
import br.com.oficina.os.domain.ordemServico.OrdemServico;
import br.com.oficina.os.domain.saga.SagaOrdemServico;

public final class OrdemServicoMapper {

    private OrdemServicoMapper() {
    }

    public static OrdemServicoResponse toResponse(OrdemServico os) {
        return new OrdemServicoResponse(
                os.getId(),
                os.getClienteId(),
                os.getVeiculoId(),
                os.getStatus(),
                os.getSintomaRelatado(),
                os.getItens().stream()
                        .map(i -> new ItemOSResponse(i.getTipo().name(), i.getReferenciaId(), i.getDescricao(),
                                i.getPrecoUnitario(), i.getQuantidade(), i.getPrecoTotal()))
                        .toList(),
                os.getValorTotal(),
                os.getMotivoCancelamento(),
                os.getDataAbertura(),
                os.getDataFinalizacao(),
                os.getDataEntrega());
    }

    public static SagaResponse toSagaResponse(SagaOrdemServico saga) {
        return new SagaResponse(
                saga.getId(),
                saga.getOsId(),
                saga.getEtapa(),
                saga.getStatus(),
                saga.getOrcamentoId(),
                saga.getPagamentoId(),
                saga.getMotivoFalha(),
                saga.getCompensacoesPendentes(),
                saga.getExpiraEm(),
                saga.getEventos().stream()
                        .map(e -> new SagaResponse.Evento(e.getTipo(), e.getDescricao(), e.getDataHora()))
                        .toList());
    }
}
