package com.dimdim.caixa.controller;

import com.dimdim.caixa.model.Cliente;
import com.dimdim.caixa.service.ClienteService;
import com.dimdim.caixa.service.ContaService;
import com.dimdim.caixa.service.NegocioException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final ContaService contaService;

    public ClienteController(ClienteService clienteService, ContaService contaService) {
        this.clienteService = clienteService;
        this.contaService = contaService;
    }

    // READ - lista
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("clientes", clienteService.listar());
        return "clientes/list";
    }

    // READ - detalhes com as contas do cliente (relacionamento 1:N)
    @GetMapping("/{id}")
    public String detalhes(@PathVariable Long id, Model model) {
        model.addAttribute("cliente", clienteService.buscar(id));
        model.addAttribute("contas", contaService.listarPorCliente(id));
        return "clientes/details";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "clientes/form";
    }

    // CREATE
    @PostMapping
    public String criar(@Valid @ModelAttribute("cliente") Cliente cliente, BindingResult result,
                        Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "clientes/form";
        }
        try {
            clienteService.criar(cliente);
            ra.addFlashAttribute("mensagem", "Cliente " + cliente.getNome() + " cadastrado com sucesso.");
            return "redirect:/clientes";
        } catch (NegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return "clientes/form";
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("cliente", clienteService.buscar(id));
        return "clientes/form";
    }

    // UPDATE
    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("cliente") Cliente cliente,
                            BindingResult result, Model model, RedirectAttributes ra) {
        cliente.setId(id);
        if (result.hasErrors()) {
            return "clientes/form";
        }
        try {
            clienteService.atualizar(id, cliente);
            ra.addFlashAttribute("mensagem", "Cliente atualizado com sucesso.");
            return "redirect:/clientes/" + id;
        } catch (NegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return "clientes/form";
        }
    }

    // DELETE
    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes ra) {
        try {
            clienteService.excluir(id);
            ra.addFlashAttribute("mensagem", "Cliente e suas contas foram excluídos.");
        } catch (NegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/clientes";
    }
}
