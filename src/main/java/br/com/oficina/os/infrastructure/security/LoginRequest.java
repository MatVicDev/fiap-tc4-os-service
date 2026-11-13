package br.com.oficina.os.infrastructure.security;

public record LoginRequest(
        String usuario,
        String senha
) {}
