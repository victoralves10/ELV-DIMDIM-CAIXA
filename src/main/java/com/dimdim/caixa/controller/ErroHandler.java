package com.dimdim.caixa.controller;

import com.dimdim.caixa.service.NegocioException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Mostra uma página amigável quando um registro não é encontrado
 * (ex.: abrir /clientes/999).
 */
@ControllerAdvice
public class ErroHandler {

    @ExceptionHandler(NegocioException.class)
    public String negocio(NegocioException e, Model model) {
        model.addAttribute("erro", e.getMessage());
        return "erro";
    }
}
