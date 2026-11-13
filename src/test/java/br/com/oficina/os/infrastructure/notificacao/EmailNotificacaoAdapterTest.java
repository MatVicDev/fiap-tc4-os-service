package br.com.oficina.os.infrastructure.notificacao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do EmailNotificacaoAdapter")
class EmailNotificacaoAdapterTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailNotificacaoAdapter adapter;

    @Test
    @DisplayName("Deve enviar e-mail com sucesso")
    void deveEnviarEmailComSucesso() {
        ReflectionTestUtils.setField(adapter, "remetente", "no-reply@oficina.com");

        adapter.notificar("cliente@email.com", "Assunto", "Mensagem");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Não deve propagar exceção quando o envio falhar")
    void naoDevePropagarExcecaoQuandoEnvioFalhar() {
        ReflectionTestUtils.setField(adapter, "remetente", "no-reply@oficina.com");
        doThrow(new RuntimeException("SMTP indisponível")).when(mailSender).send(any(SimpleMailMessage.class));

        adapter.notificar("cliente@email.com", "Assunto", "Mensagem");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
