package br.com.oficina.os.application.veiculo;

import br.com.oficina.os.api.veiculo.dto.VeiculoResponse;
import br.com.oficina.os.domain.cliente.Cpf;
import br.com.oficina.os.domain.veiculo.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListarVeiculosPorCpfProprietarioUseCase {
    private final VeiculoRepository repository;

    public List<VeiculoResponse> executar(String cpfProprietario) {
        return repository.listarPorCpfProprietario(new Cpf(cpfProprietario)).stream()
                .map(VeiculoMapper::toResponse)
                .toList();
    }
}
