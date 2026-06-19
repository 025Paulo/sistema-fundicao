package com.fundicao.model;

public class ProdutoFornecedor {

    private int id;
    private int produtoId;
    private int fornecedorId;
    private String fornecedorNome;
    private Double pesoKg;
    private Double vrKg;
    private Double vrPeca;
    private Double vrTotal;

    public ProdutoFornecedor() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProdutoId() { return produtoId; }
    public void setProdutoId(int produtoId) { this.produtoId = produtoId; }

    public int getFornecedorId() { return fornecedorId; }
    public void setFornecedorId(int fornecedorId) { this.fornecedorId = fornecedorId; }

    public String getFornecedorNome() { return fornecedorNome; }
    public void setFornecedorNome(String fornecedorNome) { this.fornecedorNome = fornecedorNome; }

    public Double getPesoKg() { return pesoKg; }
    public void setPesoKg(Double pesoKg) { this.pesoKg = pesoKg; }

    public Double getVrKg() { return vrKg; }
    public void setVrKg(Double vrKg) { this.vrKg = vrKg; }

    public Double getVrPeca() { return vrPeca; }
    public void setVrPeca(Double vrPeca) { this.vrPeca = vrPeca; }

    public Double getVrTotal() { return vrTotal; }
    public void setVrTotal(Double vrTotal) { this.vrTotal = vrTotal; }
}