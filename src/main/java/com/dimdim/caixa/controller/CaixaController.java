package com.dimdim.caixa.controller;

import com.dimdim.caixa.model.Conta;
import com.dimdim.caixa.service.ContaService;
import com.dimdim.caixa.service.NegocioException;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/**
 * Caixa Eletrônico DimDim: o cliente entra com número da conta + PIN
 * e realiza saque, depósito e consulta de saldo.
 */
@Controller
@RequestMapping("/caixa")
public class CaixaController {

    private static final String SESSAO_CONTA = "contaCaixaId";

    private final ContaService contaService;

    public CaixaController(ContaService contaService) {
        this.contaService = contaService;
    }

    @GetMapping
    public String inicio(HttpSession session) {
        if (session.getAttribute(SESSAO_CONTA) != null) {
            return "redirect:/caixa/menu";
        }
        return "caixa/login";
    }

    @PostMapping("/entrar")
    public String entrar(@RequestParam String numero, @RequestParam String pin,
                         HttpSession session, RedirectAttributes ra) {
        try {
            Conta conta = contaService.autenticar(numero.trim(), pin.trim());
            session.setAttribute(SESSAO_CONTA, conta.getId());
            return "redirect:/caixa/menu";
        } catch (NegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/caixa";
        }
    }

    @GetMapping("/menu")
    public String menu(HttpSession session, Model model) {
        Long contaId = (Long) session.getAttribute(SESSAO_CONTA);
        if (contaId == null) {
            return "redirect:/caixa";
        }
        try {
            model.addAttribute("conta", contaService.buscar(contaId));
        } catch (NegocioException e) {
            // conta foi encerrada na gerência enquanto a sessão estava aberta
            session.removeAttribute(SESSAO_CONTA);
            return "redirect:/caixa";
        }
        return "caixa/menu";
    }

    // Tela única do caixa: o cliente escolhe SAQUE ou DEPOSITO e digita o valor
    @PostMapping("/operacao")
    public String operacao(@RequestParam String tipo, @RequestParam(required = false) BigDecimal valor,
                           HttpSession session, RedirectAttributes ra) {
        return "DEPOSITO".equals(tipo) ? depositar(valor, session, ra) : sacar(valor, session, ra);
    }

    @PostMapping("/deposito")
    public String depositar(@RequestParam(required = false) BigDecimal valor,
                            HttpSession session, RedirectAttributes ra) {
        return operar(session, ra, () -> {
            Conta c = contaService.depositar(contaAtual(session), valor);
            return "Depósito de R$ " + formatar(valor) + " realizado. Novo saldo: R$ " + formatar(c.getSaldo());
        });
    }

    @PostMapping("/saque")
    public String sacar(@RequestParam(required = false) BigDecimal valor,
                        HttpSession session, RedirectAttributes ra) {
        return operar(session, ra, () -> {
            Conta c = contaService.sacar(contaAtual(session), valor);
            return "Saque de R$ " + formatar(valor) + " realizado. Retire as notas. Novo saldo: R$ " + formatar(c.getSaldo());
        });
    }

    @GetMapping("/sair")
    public String sair(HttpSession session, RedirectAttributes ra) {
        session.removeAttribute(SESSAO_CONTA);
        ra.addFlashAttribute("mensagem", "Obrigado por usar o Caixa DimDim. Até logo!");
        return "redirect:/caixa";
    }

    // ---------- auxiliares ----------

    private interface Operacao {
        String executar();
    }

    private String operar(HttpSession session, RedirectAttributes ra, Operacao op) {
        if (session.getAttribute(SESSAO_CONTA) == null) {
            return "redirect:/caixa";
        }
        try {
            ra.addFlashAttribute("mensagem", op.executar());
        } catch (NegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/caixa/menu";
    }

    private Long contaAtual(HttpSession session) {
        return (Long) session.getAttribute(SESSAO_CONTA);
    }

    private String formatar(BigDecimal valor) {
        return String.format(new java.util.Locale("pt", "BR"), "%,.2f", valor);
    }
}
