package br.com.oficina.os.domain.mensageria;

/**
 * Porta de saída da mensageria. A implementação grava num outbox transacional, de
 * modo que o comando só é publicado se a transação que mudou o estado da OS/Saga
 * for confirmada (sem "dual write" entre banco e broker).
 */
public interface PublicadorMensagens {

    void enviarComando(Mensagem comando);

    void publicarEvento(Mensagem evento);
}
