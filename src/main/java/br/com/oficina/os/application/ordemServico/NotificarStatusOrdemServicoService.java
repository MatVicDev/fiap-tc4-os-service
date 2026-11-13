package br.com.oficina.os.application.ordemServico;

import br.com.oficina.os.domain.cliente.ClienteRepository;
import br.com.oficina.os.domain.notificacao.NotificacaoPort;
import br.com.oficina.os.domain.ordemServico.OrdemServico;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificarStatusOrdemServicoService {
    private final ClienteRepository clienteRepository;
    private final NotificacaoPort notificacaoPort;

    public void notificarMudancaStatus(OrdemServico os, String complemento) {
        dadosContato(os.getClienteId()).filter(ContatoCliente::temEmail).ifPresent(contato -> {
            String mensagem = "Olá, " + contato.nome() + "! Sua OS " + os.getId() + " agora está: " + os.getStatus();
            if (complemento != null && !complemento.isBlank()) {
                mensagem += "\n" + complemento;
            }
            notificacaoPort.notificar(contato.email(), "Atualização da sua Ordem de Serviço", mensagem);
        });
    }

    public Optional<ContatoCliente> dadosContato(UUID clienteId) {
        return clienteRepository.buscarPorId(clienteId)
                .map(cliente -> new ContatoCliente(cliente.getNome(), cliente.getEmail(), cliente.getCpfNumero()));
    }

    public record ContatoCliente(String nome, String email, String cpf) {
        public boolean temEmail() {
            return email != null && !email.isBlank();
        }
    }
}
