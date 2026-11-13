package br.com.oficina.os.support;

import br.com.oficina.os.domain.mensageria.Mensagem;
import br.com.oficina.os.domain.mensageria.PublicadorMensagens;

import java.util.ArrayList;
import java.util.List;

public class FakePublicador implements PublicadorMensagens {
    public final List<Mensagem> comandos = new ArrayList<>();
    public final List<Mensagem> eventos = new ArrayList<>();

    @Override
    public void enviarComando(Mensagem comando) {
        comandos.add(comando);
    }

    @Override
    public void publicarEvento(Mensagem evento) {
        eventos.add(evento);
    }

    public List<String> tiposComandos() {
        return comandos.stream().map(Mensagem::tipo).toList();
    }

    public Mensagem ultimoComando(String tipo) {
        return comandos.stream().filter(c -> c.tipo().equals(tipo)).reduce((a, b) -> b)
                .orElseThrow(() -> new AssertionError("Comando " + tipo + " não enviado. Enviados: " + tiposComandos()));
    }
}
