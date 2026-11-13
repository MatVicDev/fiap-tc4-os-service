package br.com.oficina.os.infrastructure.config;

import br.com.oficina.os.application.saga.PrazosSaga;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableScheduling
public class AppConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    PrazosSaga prazosSaga(@Value("${saga.prazo.aprovacao:P2D}") Duration aprovacao,
                          @Value("${saga.prazo.pagamento:P1D}") Duration pagamento) {
        return new PrazosSaga(aprovacao, pagamento);
    }
}
