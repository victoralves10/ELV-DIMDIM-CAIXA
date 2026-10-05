package com.dimdim.caixa.model;

public enum TipoConta {
    CORRENTE("Conta Corrente"),
    POUPANCA("Poupança");

    private final String descricao;

    TipoConta(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
