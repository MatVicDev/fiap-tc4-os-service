package br.com.oficina.os.infrastructure.persistence.mensageria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "mensagens_processadas")
public class MensagemProcessada {

    @Id
    private UUID id;

    @Column(nullable = false, length = 80)
    private String tipo;

    @Column(nullable = false)
    private LocalDateTime processadaEm;

    public MensagemProcessada(UUID id, String tipo) {
        this.id = id;
        this.tipo = tipo;
        this.processadaEm = LocalDateTime.now();
    }
}
