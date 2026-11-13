package br.com.oficina.os.application.ordemServico;

import br.com.oficina.os.api.ordemServico.dto.AbrirOrdemServicoRequest;
import br.com.oficina.os.api.ordemServico.dto.OrdemServicoResponse;
import br.com.oficina.os.application.saga.OrquestradorSagaOS;
import br.com.oficina.os.domain.cliente.Cliente;
import br.com.oficina.os.domain.cliente.ClienteRepository;
import br.com.oficina.os.domain.cliente.Cpf;
import br.com.oficina.os.domain.exception.DomainException;
import br.com.oficina.os.domain.exception.EntidadeNaoEncontradaException;
import br.com.oficina.os.domain.ordemServico.OrdemServico;
import br.com.oficina.os.domain.ordemServico.OrdemServicoRepository;
import br.com.oficina.os.domain.veiculo.Placa;
import br.com.oficina.os.domain.veiculo.Veiculo;
import br.com.oficina.os.domain.veiculo.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abre a OS e inicia a Saga na mesma transação local: OS, estado da Saga e o primeiro
 * comando (no outbox) são gravados juntos — ou nada é gravado.
 */
@Service
@RequiredArgsConstructor
public class AbrirOrdemServicoUseCase {
    private final OrdemServicoRepository osRepository;
    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final OrquestradorSagaOS orquestrador;
    private final NotificarStatusOrdemServicoService notificador;

    @Transactional
    public OrdemServicoResponse executar(AbrirOrdemServicoRequest request) {
        Cliente cliente = clienteRepository.buscarPorCpf(new Cpf(request.cpfCliente()))
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Cliente não encontrado"));
        if (!cliente.isAtivo()) {
            throw new DomainException("Cliente inativo não pode abrir OS.");
        }

        Veiculo veiculo = veiculoRepository.buscarPorPlaca(new Placa(request.placaVeiculo()))
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Veículo não encontrado"));

        OrdemServico os = osRepository.salvar(new OrdemServico(cliente.getId(), veiculo.getId(), request.sintomaRelatado()));
        orquestrador.iniciar(os, cliente, veiculo);
        notificador.notificarMudancaStatus(os, null);

        return OrdemServicoMapper.toResponse(os);
    }
}
