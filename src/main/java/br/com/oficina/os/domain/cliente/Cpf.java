package br.com.oficina.os.domain.cliente;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Cpf {
    @Column(name = "cpf", length = 14, nullable = false, unique = true)
    private String numero;

    protected Cpf() {}

    public Cpf(String numero) {
        if (!validar(numero)) {
            throw new IllegalArgumentException("CPF/CNPJ inválido: " + numero);
        }

        this.numero = sanear(numero);
    }

    private String sanear(String valor) {
        return valor.replaceAll("\\D", "");
    }

    private boolean validar(String valor) {
        if (valor == null) return false;
        String numeroSaneado = sanear(valor);
        if (numeroSaneado.length() == 11) return validoComoCpf(numeroSaneado);
        if (numeroSaneado.length() == 14) return validoComoCnpj(numeroSaneado);
        return false;
    }

    private boolean validoComoCpf(String cpf) {
        if (cpf.chars().distinct().count() == 1) return false;

        int soma = 0;

        for (int i = 0; i < 9; i++) {
            soma += (cpf.charAt(i) - '0') * (10 - i);
        }

        int primeiroDigitoVerificador = 11 - (soma % 11);
        if (primeiroDigitoVerificador >= 10) primeiroDigitoVerificador = 0;
        if (primeiroDigitoVerificador != (cpf.charAt(9) - '0')) return false;

        soma = 0;
        for (int i = 0; i < 10; i++)
            soma += (cpf.charAt(i) - '0') * (11 - i);
        int segundoDigitoVerificador = 11 - (soma % 11);
        if (segundoDigitoVerificador >= 10) segundoDigitoVerificador = 0;
        return segundoDigitoVerificador == (cpf.charAt(10) - '0');
    }

    private boolean validoComoCnpj(String cnpj) {
        if (cnpj.chars().distinct().count() == 1) {
            return false;
        }

        int[] pesosPrimeiroDigito = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesosSegundoDigito = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        int soma = 0;
        for (int i = 0; i < 12; i++) {
            soma += (cnpj.charAt(i) - '0') * pesosPrimeiroDigito[i];
        }

        int primeiroDigitoVerificador = soma % 11 < 2 ? 0 : 11 - (soma % 11);
        if (primeiroDigitoVerificador != (cnpj.charAt(12) - '0')) return false;

        soma = 0;
        for (int i = 0; i < 13; i++)
            soma += (cnpj.charAt(i) - '0') * pesosSegundoDigito[i];
        int segundoDigitoVerificador = soma % 11 < 2 ? 0 : 11 - (soma % 11);

        return segundoDigitoVerificador == (cnpj.charAt(13) - '0');
    }

    public String getNumero() {
        return numero;
    }
}
