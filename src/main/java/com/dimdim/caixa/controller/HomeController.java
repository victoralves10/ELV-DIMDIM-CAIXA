package com.dimdim.caixa.controller;

import com.dimdim.caixa.service.ClienteService;
import com.dimdim.caixa.service.ContaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ClienteService clienteService;
    private final ContaService contaService;

    public HomeController(ClienteService clienteService, ContaService contaService) {
        this.clienteService = clienteService;
        this.contaService = contaService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("totalClientes", clienteService.listar().size());
        model.addAttribute("totalContas", contaService.listar().size());
        return "index";
    }
}
