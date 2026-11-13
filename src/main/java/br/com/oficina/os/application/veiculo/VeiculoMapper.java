package br.com.oficina.os.application.veiculo;

import br.com.oficina.os.api.veiculo.dto.VeiculoResponse;
import br.com.oficina.os.domain.veiculo.Veiculo;

public class VeiculoMapper {

    public static VeiculoResponse toResponse(Veiculo veiculo) {
        return new  VeiculoResponse(
                veiculo.getId(),
                veiculo.getMarca(),
                veiculo.getModelo(),
                veiculo.getAno(),
                veiculo.getCor(),
                veiculo.getPlaca().getNumero(),
                veiculo.getCpfProprietario());
    }
}
