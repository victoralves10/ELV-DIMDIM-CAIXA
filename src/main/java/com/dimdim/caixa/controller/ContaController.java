package com.dimdim.caixa.controller;

import com.dimdim.caixa.model.Cliente;
import com.dimdim.caixa.model.Conta;
import com.dimdim.caixa.model.TipoConta;
import com.dimdim.caixa.service.ClienteService;
import com.dimdim.caixa.service.ContaService;
import com.dimdim.caixa.service.NegocioException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/contas")
public class ContaController {

    private final ContaService contaService;
    private final ClienteService clienteService;

    public ContaController(ContaService contaService, ClienteService clienteService) {
        this.contaService = contaService;
        this.clienteService = clienteService;
    }

    @ModelAttribute("tipos")
    public TipoConta[] tipos() {
        return TipoConta.values();
    }

    @ModelAttribute("clientes")
    public List<Cliente> clientes() {
        return clienteService.listar();
    }

    // READ - lista
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("contas", contaService.listar());
        return "contas/list";
    }

    // READ - detalhes
    @GetMapping("/{id}")
    public String detalhes(@PathVariable Long id, Model model) {
        model.addAttribute("conta", contaService.buscar(id));
        return "contas/details";
    }

    // Formulário de abertura (pode vir com o cliente já selecionado)
    @GetMapping("/nova")
    public String nova(@RequestParam(required = false) Long clienteId, Model model) {
        Conta conta = new Conta();
        conta.setCliente(new Cliente());
        conta.getCliente().setId(clienteId);
        model.addAttribute("conta", conta);
        return "contas/form";
    }

    // CREATE
    @PostMapping
    public String criar(@Valid @ModelAttribute("conta") Conta conta, BindingResult result,
                        Model model, RedirectAttributes ra) {
        validarTitular(conta, result);
        if (result.hasErrors()) {
            return "contas/form";
        }
        try {
            Conta salva = contaService.criar(conta);
            ra.addFlashAttribute("mensagem", "Conta " + salva.getNumero() + " aberta com sucesso.");
            return "redirect:/contas";
        } catch (NegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return "contas/form";
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("conta", contaService.buscar(id));
        return "contas/form";
    }

    // UPDATE
    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("conta") Conta conta,
                            BindingResult result, Model model, RedirectAttributes ra) {
        conta.setId(id);
        validarTitular(conta, result);
        if (result.hasErrors()) {
            return "contas/form";
        }
        try {
            contaService.atualizar(id, conta);
            ra.addFlashAttribute("mensagem", "Conta atualizada com sucesso.");
            return "redirect:/contas/" + id;
        } catch (NegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return "contas/form";
        }
    }

    // DELETE
    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes ra) {
        try {
            contaService.excluir(id);
            ra.addFlashAttribute("mensagem", "Conta encerrada com sucesso.");
        } catch (NegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/contas";
    }

    private void validarTitular(Conta conta, BindingResult result) {
        if (conta.getCliente() == null || conta.getCliente().getId() == null) {
            result.rejectValue("cliente", "titular", "Selecione o cliente titular.");
        }
    }
}
