package br.com.oficina.os.domain.notificacao;

public interface NotificacaoPort {
    void notificar(String destinatario, String assunto, String mensagem);
}
