package br.com.oficina.os.support;

import br.com.oficina.os.domain.mensageria.MensagensProcessadas;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FakeMensagensProcessadas implements MensagensProcessadas {
    private final Set<UUID> ids = new HashSet<>();

    @Override
    public boolean registrarSeNova(UUID mensagemId, String tipo) {
        return ids.add(mensagemId);
    }
}
