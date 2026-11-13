package br.com.oficina.os.domain.ordemServico;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Snapshot de um item (serviço ou insumo) definido no diagnóstico. O catálogo e o
 * estoque pertencem ao execution-service — aqui guardamos apenas a cópia dos dados
 * (descrição e preço na data do diagnóstico), sem FK para o banco de outro serviço.
 */
@Embeddable
@NoArgsConstructor
public class ItemOS {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoItem tipo;

    @Column(nullable = false)
    private UUID referenciaId;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private Integer quantidade;

    public ItemOS(TipoItem tipo, UUID referenciaId, String descricao, BigDecimal precoUnitario, Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }
        if (precoUnitario == null || precoUnitario.signum() < 0) {
            throw new IllegalArgumentException("Preço unitário inválido.");
        }
        this.tipo = tipo;
        this.referenciaId = referenciaId;
        this.descricao = descricao;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoTotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public TipoItem getTipo() {
        return tipo;
    }

    public UUID getReferenciaId() {
        return referenciaId;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public enum TipoItem {
        SERVICO,
        INSUMO
    }
}
