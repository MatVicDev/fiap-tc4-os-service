package br.com.oficina.os.domain.veiculo;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@NoArgsConstructor
@Table(name = "veiculos", indexes = {
        @Index(name = "idx_veiculos_cpf_proprietario", columnList = "cpfProprietario")
})
public class Veiculo {

    @Id
    private UUID id;

    @Embedded
    private Placa placa;

    private String marca;
    private String modelo;
    private Integer ano;
    private String cor;

    @Column(nullable = false)
    private String cpfProprietario;

    public Veiculo(String placa, String marca, String modelo, Integer ano, String cor, String cpfProprietario) {
        validarAno(ano);
        this.id = UUID.randomUUID();
        this.placa = new Placa(placa);
        this.marca = marca;
        this.modelo = modelo;
        this.ano = ano;
        this.cor = cor;
        this.cpfProprietario = cpfProprietario;
    }

    private void validarAno(Integer ano) {
        int anoAtual = java.time.Year.now().getValue();
        if (ano < 1900 || ano > anoAtual + 1) {
            throw new IllegalArgumentException("Ano do veículo inválido.");
        }
    }

    public void atualizar(String novaMarca, String novaModelo, Integer novoAno, String novaCor) {
        if (novaMarca != null && !novaMarca.isBlank()) {
            this.marca = novaMarca;
        }

        if (novaModelo != null && !novaModelo.isBlank()) {
            this.modelo = novaModelo;
        }

        if (novoAno != null)  {
            this.ano = novoAno;
        }

        if (novaCor != null && !novaCor.isBlank()) {
            this.cor = novaCor;
        }
    }

    public UUID getId() {
        return id;
    }

    public Placa getPlaca() {
        return placa;
    }

    public String getMarca() {
        return marca;
    }

    public String getModelo() {
        return modelo;
    }

    public Integer getAno() {
        return ano;
    }

    public String getCor() {
        return cor;
    }

    public String getCpfProprietario() {
        return cpfProprietario;
    }
}