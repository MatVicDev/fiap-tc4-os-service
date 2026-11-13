package br.com.oficina.os.infrastructure.persistence.mensageria;

import br.com.oficina.os.domain.mensageria.MensagensProcessadas;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Registro roda na mesma transação do processamento do evento: se o processamento
 * falhar, o registro também é desfeito e a reentrega será processada normalmente.
 */
@Component
@RequiredArgsConstructor
public class MensagensProcessadasAdapter implements MensagensProcessadas {
    private final MensagemProcessadaJpa jpa;

    @Override
    public boolean registrarSeNova(UUID mensagemId, String tipo) {
        if (jpa.existsById(mensagemId)) {
            return false;
        }
        jpa.save(new MensagemProcessada(mensagemId, tipo));
        return true;
    }
}
