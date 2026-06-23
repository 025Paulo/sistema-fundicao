package com.fundicao.model;

public class SaldoEstoque {

    private final int produtoId;
    private final String descricao;
    private final double saldo;
    private final String ultimaMovimentacao;
    private final String ultimoTipo;

    public SaldoEstoque(int produtoId, String descricao, double saldo,
                        String ultimaMovimentacao, String ultimoTipo) {
        this.produtoId = produtoId;
        this.descricao = descricao;
        this.saldo = saldo;
        this.ultimaMovimentacao = ultimaMovimentacao;
        this.ultimoTipo = ultimoTipo;
    }

    public int getProdutoId()            { return produtoId; }
    public String getDescricao()         { return descricao; }
    public double getSaldo()             { return saldo; }
    public String getUltimaMovimentacao(){ return ultimaMovimentacao; }
    public String getUltimoTipo()        { return ultimoTipo; }
}