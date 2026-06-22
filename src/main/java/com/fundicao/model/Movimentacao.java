package com.fundicao.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Movimentacao {

    private int id;
    private int produtoId;
    private String produtoNome;
    private String tipo; // "Entrada" ou "Saida"
    private double quantidade;
    private LocalDate dataMovimentacao;
    private Integer notaId;
    private Double valorUnitario;
    private Integer entidadeId;
    private String entidadeNome;
    private String transportadora;
    private String ordemCompra;
    private String observacoes;
    private LocalDateTime criadoEm;

    public Movimentacao() {}

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProdutoId() { return produtoId; }
    public void setProdutoId(int produtoId) { this.produtoId = produtoId; }

    public String getProdutoNome() { return produtoNome; }
    public void setProdutoNome(String produtoNome) { this.produtoNome = produtoNome; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public double getQuantidade() { return quantidade; }
    public void setQuantidade(double quantidade) { this.quantidade = quantidade; }

    public LocalDate getDataMovimentacao() { return dataMovimentacao; }
    public void setDataMovimentacao(LocalDate dataMovimentacao) { this.dataMovimentacao = dataMovimentacao; }

    public Integer getNotaId() { return notaId; }
    public void setNotaId(Integer notaId) { this.notaId = notaId; }

    public Double getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(Double valorUnitario) { this.valorUnitario = valorUnitario; }

    public Integer getEntidadeId() { return entidadeId; }
    public void setEntidadeId(Integer entidadeId) { this.entidadeId = entidadeId; }

    public String getEntidadeNome() { return entidadeNome; }
    public void setEntidadeNome(String entidadeNome) { this.entidadeNome = entidadeNome; }

    public String getTransportadora() { return transportadora; }
    public void setTransportadora(String transportadora) { this.transportadora = transportadora; }

    public String getOrdemCompra() { return ordemCompra; }
    public void setOrdemCompra(String ordemCompra) { this.ordemCompra = ordemCompra; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}