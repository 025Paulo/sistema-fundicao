package com.fundicao.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class NotaFiscal {

    private Integer id;
    private String natureza;
    private String numero;
    private LocalDate data;
    private String ordemCompra;
    private Integer entidadeId;
    private String entidadeNome;
    private Double transporteRs;
    private Double descontoRs;
    private String transportadora;
    private Double pesoBruto;
    private Double pesoLiquido;
    private LocalDateTime criadoEm;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNatureza() { return natureza; }
    public void setNatureza(String natureza) { this.natureza = natureza; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public String getOrdemCompra() { return ordemCompra; }
    public void setOrdemCompra(String ordemCompra) { this.ordemCompra = ordemCompra; }

    public Integer getEntidadeId() { return entidadeId; }
    public void setEntidadeId(Integer entidadeId) { this.entidadeId = entidadeId; }

    public String getEntidadeNome() { return entidadeNome; }
    public void setEntidadeNome(String entidadeNome) { this.entidadeNome = entidadeNome; }

    public Double getTransporteRs() { return transporteRs; }
    public void setTransporteRs(Double transporteRs) { this.transporteRs = transporteRs; }

    public Double getDescontoRs() { return descontoRs; }
    public void setDescontoRs(Double descontoRs) { this.descontoRs = descontoRs; }

    public String getTransportadora() { return transportadora; }
    public void setTransportadora(String transportadora) { this.transportadora = transportadora; }

    public Double getPesoBruto() { return pesoBruto; }
    public void setPesoBruto(Double pesoBruto) { this.pesoBruto = pesoBruto; }

    public Double getPesoLiquido() { return pesoLiquido; }
    public void setPesoLiquido(Double pesoLiquido) { this.pesoLiquido = pesoLiquido; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}