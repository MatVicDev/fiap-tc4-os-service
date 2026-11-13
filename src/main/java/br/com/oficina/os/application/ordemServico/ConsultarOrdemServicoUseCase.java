package br.com.oficina.os.application.ordemServico;

import br.com.oficina.os.api.ordemServico.dto.HistoricoResponse;
import br.com.oficina.os.api.ordemServico.dto.OrdemServicoResponse;
import br.com.oficina.os.api.ordemServico.dto.SagaResponse;
import br.com.oficina.os.domain.exception.EntidadeNaoEncontradaException;
import br.com.oficina.os.domain.ordemServico.OrdemServico;
import br.com.oficina.os.domain.ordemServico.OrdemServicoRepository;
import br.com.oficina.os.domain.ordemServico.StatusOS;
import br.com.oficina.os.domain.saga.SagaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultarOrdemServicoUseCase {

    /** Ordem de exibição da fila de acompanhamento: o que está mais perto do fim primeiro. */
    private static final List<StatusOS> ORDEM_EXIBICAO = List.of(
            StatusOS.EM_EXECUCAO, StatusOS.AGUARDANDO_EXECUCAO, StatusOS.AGUARDANDO_PAGAMENTO,
            StatusOS.AGUARDANDO_APROVACAO, StatusOS.EM_DIAGNOSTICO, StatusOS.RECEBIDA);

    private final OrdemServicoRepository osRepository;
    private final SagaRepository sagaRepository;

    public OrdemServicoResponse buscar(UUID id) {
        return OrdemServicoMapper.toResponse(buscarOs(id));
    }

    public List<OrdemServicoResponse> listarEmAndamento() {
        return osRepository.listarTodos().stream()
                .filter(os -> ORDEM_EXIBICAO.contains(os.getStatus()))
                .sorted(Comparator.comparing((OrdemServico os) -> ORDEM_EXIBICAO.indexOf(os.getStatus()))
                        .thenComparing(OrdemServico::getDataAbertura))
                .map(OrdemServicoMapper::toResponse)
                .toList();
    }

    public List<OrdemServicoResponse> listarPorCliente(UUID clienteId) {
        return osRepository.listarPorCliente(clienteId).stream()
                .sorted(Comparator.comparing(OrdemServico::getDataAbertura).reversed())
                .map(OrdemServicoMapper::toResponse)
                .toList();
    }

    public List<HistoricoResponse> historico(UUID id) {
        return buscarOs(id).getHistorico().stream()
                .map(h -> new HistoricoResponse(h.getStatus(), h.getDataHora(), h.getObservacao()))
                .toList();
    }

    public SagaResponse saga(UUID osId) {
        return sagaRepository.buscarPorOsId(osId)
                .map(OrdemServicoMapper::toSagaResponse)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Saga da OS não encontrada"));
    }

    private OrdemServico buscarOs(UUID id) {
        return osRepository.buscarPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Ordem de serviço não encontrada"));
    }
}
