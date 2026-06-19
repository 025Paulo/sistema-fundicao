package com.fundicao.model;

public class Produto {

    private int id;
    private String descricao;
    private String classificacaoFiscal;
    private String criadoEm;

    public Produto() {}

    public Produto(int id, String descricao, String classificacaoFiscal) {
        this.id = id;
        this.descricao = descricao;
        this.classificacaoFiscal = classificacaoFiscal;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getClassificacaoFiscal() { return classificacaoFiscal; }
    public void setClassificacaoFiscal(String classificacaoFiscal) { this.classificacaoFiscal = classificacaoFiscal; }

    public String getCriadoEm() { return criadoEm; }
    public void setCriadoEm(String criadoEm) { this.criadoEm = criadoEm; }

    @Override
    public String toString() { return descricao; }
}