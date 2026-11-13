package br.com.oficina.os.infrastructure.messaging;

import br.com.oficina.os.application.saga.OrquestradorSagaOS;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaTimeoutScheduler {
    private final OrquestradorSagaOS orquestrador;

    @Scheduled(fixedDelayString = "${saga.verificacao-prazos-ms:60000}", initialDelayString = "${saga.verificacao-prazos-ms:60000}")
    public void verificarPrazos() {
        int expiradas = orquestrador.expirarSagasVencidas();
        if (expiradas > 0) {
            log.warn("{} Saga(s) compensada(s) por prazo expirado", expiradas);
        }
    }
}
