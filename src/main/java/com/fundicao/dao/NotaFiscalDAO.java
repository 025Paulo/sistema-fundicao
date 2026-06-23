package com.fundicao.dao;

import com.fundicao.model.NotaFiscal;
import com.fundicao.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotaFiscalDAO {

    private Connection getConnection() throws SQLException {
        return DatabaseManager.getInstance().getConnection();
    }

    public List<NotaFiscal> listarTodas() throws SQLException {
        String sql = """
            SELECT nf.*, e.razao_social AS entidade_nome
            FROM notas_fiscais nf
            LEFT JOIN entidades e ON e.id = nf.entidade_id
            ORDER BY nf.data DESC, nf.id DESC
        """;

        List<NotaFiscal> lista = new ArrayList<>();

        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }

        return lista;
    }

    public int inserir(NotaFiscal nf) throws SQLException {
        String sql = """
            INSERT INTO notas_fiscais
            (natureza, numero, data, ordem_compra, entidade_id,
             transporte_rs, desconto_rs, transportadora, peso_bruto, peso_liquido)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencherStatement(ps, nf);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public void atualizar(NotaFiscal nf) throws SQLException {
        String sql = """
            UPDATE notas_fiscais SET
                natureza = ?,
                numero = ?,
                data = ?,
                ordem_compra = ?,
                entidade_id = ?,
                transporte_rs = ?,
                desconto_rs = ?,
                transportadora = ?,
                peso_bruto = ?,
                peso_liquido = ?
            WHERE id = ?
        """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            preencherStatement(ps, nf);
            ps.setInt(11, nf.getId());
            ps.executeUpdate();
        }
    }

    public void excluir(int id) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "DELETE FROM notas_fiscais WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void preencherStatement(PreparedStatement ps, NotaFiscal nf) throws SQLException {
        ps.setString(1, nf.getNatureza());
        ps.setString(2, nf.getNumero());
        ps.setString(3, nf.getData().toString());
        ps.setString(4, valorOuNull(nf.getOrdemCompra()));

        if (nf.getEntidadeId() != null) ps.setInt(5, nf.getEntidadeId());
        else ps.setNull(5, Types.INTEGER);

        if (nf.getTransporteRs() != null) ps.setDouble(6, nf.getTransporteRs());
        else ps.setNull(6, Types.REAL);

        if (nf.getDescontoRs() != null) ps.setDouble(7, nf.getDescontoRs());
        else ps.setNull(7, Types.REAL);

        ps.setString(8, valorOuNull(nf.getTransportadora()));

        if (nf.getPesoBruto() != null) ps.setDouble(9, nf.getPesoBruto());
        else ps.setNull(9, Types.REAL);

        if (nf.getPesoLiquido() != null) ps.setDouble(10, nf.getPesoLiquido());
        else ps.setNull(10, Types.REAL);
    }

    private NotaFiscal mapear(ResultSet rs) throws SQLException {
        NotaFiscal nf = new NotaFiscal();
        nf.setId(rs.getInt("id"));
        nf.setNatureza(rs.getString("natureza"));
        nf.setNumero(rs.getString("numero"));

        String data = rs.getString("data");
        if (data != null) nf.setData(LocalDate.parse(data));

        nf.setOrdemCompra(rs.getString("ordem_compra"));

        int entidadeId = rs.getInt("entidade_id");
        if (!rs.wasNull()) nf.setEntidadeId(entidadeId);

        nf.setEntidadeNome(rs.getString("entidade_nome"));

        double transporte = rs.getDouble("transporte_rs");
        if (!rs.wasNull()) nf.setTransporteRs(transporte);

        double desconto = rs.getDouble("desconto_rs");
        if (!rs.wasNull()) nf.setDescontoRs(desconto);

        nf.setTransportadora(rs.getString("transportadora"));

        double pesoBruto = rs.getDouble("peso_bruto");
        if (!rs.wasNull()) nf.setPesoBruto(pesoBruto);

        double pesoLiquido = rs.getDouble("peso_liquido");
        if (!rs.wasNull()) nf.setPesoLiquido(pesoLiquido);

        String criadoEm = rs.getString("criado_em");
        if (criadoEm != null) {
            nf.setCriadoEm(LocalDateTime.parse(criadoEm.replace(" ", "T")));
        }

        return nf;
    }

    private String valorOuNull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}