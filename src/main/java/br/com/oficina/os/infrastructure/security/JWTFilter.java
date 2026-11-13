package br.com.oficina.os.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {
    private final JWTService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String cabecalhoAutorizacao = request.getHeader("Authorization");

        if (cabecalhoAutorizacao == null || !cabecalhoAutorizacao.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = cabecalhoAutorizacao.substring(7);

        if (jwtService.isTokenValido(token)) {
            String subject = jwtService.extrairUsername(token);
            String role = jwtService.extrairRole(token);

            UsernamePasswordAuthenticationToken autenticacao = "CLIENTE".equals(role)
                    ? autenticacaoDoCliente(subject)
                    : autenticacaoDoAdmin(subject);

            SecurityContextHolder.getContext().setAuthentication(autenticacao);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Token emitido pela Function Serverless de autenticação por CPF: não existe um
     * UserDetails cadastrado para o cliente, então a autenticação é montada a partir
     * das próprias claims do token, já validado.
     */
    private UsernamePasswordAuthenticationToken autenticacaoDoCliente(String cpf) {
        return new UsernamePasswordAuthenticationToken(
                cpf, null, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));
    }

    private UsernamePasswordAuthenticationToken autenticacaoDoAdmin(String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
    }
}
