package br.com.oficina.os.infrastructure.persistence.mensageria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MensagemProcessadaJpa extends JpaRepository<MensagemProcessada, UUID> {
}
