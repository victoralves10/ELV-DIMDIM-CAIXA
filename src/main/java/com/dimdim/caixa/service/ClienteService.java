package com.dimdim.caixa.service;

import com.dimdim.caixa.model.Cliente;
import com.dimdim.caixa.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new NegocioException("Cliente não encontrado."));
    }

    // CREATE
    @Transactional
    public Cliente criar(Cliente cliente) {
        if (clienteRepository.existsByCpf(cliente.getCpf())) {
            throw new NegocioException("Já existe um cliente com esse CPF.");
        }
        cliente.setId(null);
        return clienteRepository.save(cliente);
    }

    // UPDATE
    @Transactional
    public Cliente atualizar(Long id, Cliente dados) {
        Cliente cliente = buscar(id);
        if (clienteRepository.existsByCpfAndIdNot(dados.getCpf(), id)) {
            throw new NegocioException("Já existe outro cliente com esse CPF.");
        }
        cliente.setNome(dados.getNome());
        cliente.setCpf(dados.getCpf());
        cliente.setEmail(dados.getEmail());
        return clienteRepository.save(cliente);
    }

    // DELETE (as contas do cliente são removidas em cascata)
    @Transactional
    public void excluir(Long id) {
        Cliente cliente = buscar(id);
        clienteRepository.delete(cliente);
    }
}
