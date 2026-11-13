package br.com.oficina.os.infrastructure.notificacao;

import br.com.oficina.os.domain.notificacao.NotificacaoPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailNotificacaoAdapter implements NotificacaoPort {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificacaoAdapter.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@oficina.com}")
    private String remetente;

    @Override
    public void notificar(String destinatario, String assunto, String mensagem) {
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom(remetente);
            email.setTo(destinatario);
            email.setSubject(assunto);
            email.setText(mensagem);

            mailSender.send(email);
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail de notificação para {}: {}", destinatario, e.getMessage());
        }
    }
}
