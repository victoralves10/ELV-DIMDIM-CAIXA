package com.dimdim.caixa.service;

/**
 * Erro de regra de negócio, exibido na tela para o usuário.
 */
public class NegocioException extends RuntimeException {
    public NegocioException(String message) {
        super(message);
    }
}
