package br.com.oficina.os.application.ordemServico;

import br.com.oficina.os.api.ordemServico.dto.OrdemServicoResponse;
import br.com.oficina.os.domain.exception.EntidadeNaoEncontradaException;
import br.com.oficina.os.domain.ordemServico.OrdemServico;
import br.com.oficina.os.domain.ordemServico.OrdemServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrarEntregaUseCase {
    private final OrdemServicoRepository repository;
    private final NotificarStatusOrdemServicoService notificador;

    @Transactional
    public OrdemServicoResponse executar(UUID id) {
        OrdemServico os = repository.buscarPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("OS não encontrada"));

        os.registrarEntrega();
        repository.salvar(os);
        notificador.notificarMudancaStatus(os, null);

        return OrdemServicoMapper.toResponse(os);
    }
}
