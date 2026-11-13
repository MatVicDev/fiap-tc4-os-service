package br.com.oficina.os.domain.cliente;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Table(name = "clientes")
public class Cliente {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Embedded
    private Cpf cpf;

    @Column(length = 11)
    private String telefone;

    @Column(length = 50)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusCliente status;

    private LocalDateTime dataCadastro;

    public Cliente(String nome, String cpfRaw, String telefone, String email) {
        validarNome(nome);
        this.id = UUID.randomUUID();
        this.nome = nome;
        this.cpf = new Cpf(cpfRaw);
        this.telefone = telefone;
        this.email = email;
        this.status = StatusCliente.ATIVO;
        this.dataCadastro = LocalDateTime.now();
    }

    public void ativar() {
        this.status = StatusCliente.ATIVO;
    }

    public void inativar() {
        this.status = StatusCliente.INATIVO;
    }

    public boolean isAtivo() {
        return this.status == StatusCliente.ATIVO;
    }

    private void validarNome(String nome) {
        if (nome == null || nome.trim().length() < 3) {
            throw new IllegalArgumentException("O nome do cliente deve ter pelo menos 3 caracteres.");
        }
    }

    public void atualizarContatos(String novoTelefone, String novoEmail) {
        if (novoTelefone != null && !novoTelefone.isBlank()) {
            this.telefone = novoTelefone;
        }

        if (novoEmail != null && !novoEmail.isBlank()) {
            this.email = novoEmail;
        }
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Cpf getCpf() {
        return cpf;
    }

    public String getCpfNumero() {
        return cpf.getNumero();
    }

    public String getTelefone() {
        return telefone;
    }

    public String getEmail() {
        return email;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public StatusCliente getStatus() {
        return status;
    }
}