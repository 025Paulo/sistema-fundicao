package com.fundicao.model;

public class NotaProduto {
    private int id;
    private int notaId;
    private int produtoId;
    private String produtoDescricao;
    private Double quantidade;
    private Double vrUnitario;
    private Double vrTotal;

    // getters e setters...
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getNotaId() { return notaId; }
    public void setNotaId(int notaId) { this.notaId = notaId; }
    public int getProdutoId() { return produtoId; }
    public void setProdutoId(int produtoId) { this.produtoId = produtoId; }
    public String getProdutoDescricao() { return produtoDescricao; }
    public void setProdutoDescricao(String produtoDescricao) { this.produtoDescricao = produtoDescricao; }
    public Double getQuantidade() { return quantidade; }
    public void setQuantidade(Double quantidade) { this.quantidade = quantidade; }
    public Double getVrUnitario() { return vrUnitario; }
    public void setVrUnitario(Double vrUnitario) { this.vrUnitario = vrUnitario; }
    public Double getVrTotal() { return vrTotal; }
    public void setVrTotal(Double vrTotal) { this.vrTotal = vrTotal; }
}