package com.dimdim.caixa.service;

import com.dimdim.caixa.model.Cliente;
import com.dimdim.caixa.model.Conta;
import com.dimdim.caixa.repository.ContaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ContaService {

    private final ContaRepository contaRepository;
    private final ClienteService clienteService;

    public ContaService(ContaRepository contaRepository, ClienteService clienteService) {
        this.contaRepository = contaRepository;
        this.clienteService = clienteService;
    }

    @Transactional(readOnly = true)
    public List<Conta> listar() {
        return contaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Conta> listarPorCliente(Long clienteId) {
        return contaRepository.findByClienteIdOrderByNumero(clienteId);
    }

    @Transactional(readOnly = true)
    public Conta buscar(Long id) {
        return contaRepository.findById(id)
                .orElseThrow(() -> new NegocioException("Conta não encontrada."));
    }

    // CREATE
    @Transactional
    public Conta criar(Conta conta) {
        if (contaRepository.existsByNumero(conta.getNumero())) {
            throw new NegocioException("Já existe uma conta com esse número.");
        }
        Cliente titular = clienteService.buscar(conta.getCliente().getId());
        conta.setId(null);
        conta.setCliente(titular);
        return contaRepository.save(conta);
    }

    // UPDATE (o saldo só muda por saque/depósito no caixa)
    @Transactional
    public Conta atualizar(Long id, Conta dados) {
        Conta conta = buscar(id);
        if (contaRepository.existsByNumeroAndIdNot(dados.getNumero(), id)) {
            throw new NegocioException("Já existe outra conta com esse número.");
        }
        conta.setNumero(dados.getNumero());
        conta.setAgencia(dados.getAgencia());
        conta.setTipo(dados.getTipo());
        conta.setPin(dados.getPin());
        conta.setCliente(clienteService.buscar(dados.getCliente().getId()));
        return contaRepository.save(conta);
    }

    // DELETE
    @Transactional
    public void excluir(Long id) {
        contaRepository.delete(buscar(id));
    }

    // ---------- Operações do Caixa Eletrônico ----------

    @Transactional(readOnly = true)
    public Conta autenticar(String numero, String pin) {
        return contaRepository.findByNumero(numero)
                .filter(c -> c.getPin().equals(pin))
                .orElseThrow(() -> new NegocioException("Conta ou PIN inválidos."));
    }

    @Transactional
    public Conta depositar(Long contaId, BigDecimal valor) {
        validarValor(valor);
        Conta conta = buscar(contaId);
        conta.setSaldo(conta.getSaldo().add(valor));
        return contaRepository.save(conta);
    }

    @Transactional
    public Conta sacar(Long contaId, BigDecimal valor) {
        validarValor(valor);
        Conta conta = buscar(contaId);
        if (conta.getSaldo().compareTo(valor) < 0) {
            throw new NegocioException("Saldo insuficiente para este saque.");
        }
        conta.setSaldo(conta.getSaldo().subtract(valor));
        return contaRepository.save(conta);
    }

    private void validarValor(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("Informe um valor maior que zero.");
        }
        if (valor.scale() > 2) {
            throw new NegocioException("Use no máximo 2 casas decimais.");
        }
    }
}
