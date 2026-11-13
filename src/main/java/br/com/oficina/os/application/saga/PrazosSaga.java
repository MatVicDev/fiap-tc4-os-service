package br.com.oficina.os.application.saga;

import java.time.Duration;

/**
 * Prazos máximos das etapas que dependem de ação humana (cliente aprovar o orçamento
 * e efetuar o pagamento). Esgotado o prazo, a Saga é compensada automaticamente.
 */
public record PrazosSaga(Duration aprovacao, Duration pagamento) {
}
