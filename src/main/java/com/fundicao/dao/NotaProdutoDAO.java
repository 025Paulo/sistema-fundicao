package com.fundicao.dao;

import com.fundicao.model.NotaProduto;
import com.fundicao.util.DatabaseManager;
import java.sql.*;
import java.util.*;

public class NotaProdutoDAO {

    private Connection getConnection() throws SQLException {
        return DatabaseManager.getInstance().getConnection();
    }

    public List<NotaProduto> listarPorNota(int notaId) throws SQLException {
        List<NotaProduto> lista = new ArrayList<>();
        String sql = """
            SELECT np.*, p.descricao as produto_descricao
            FROM nota_produtos np
            JOIN produtos p ON p.id = np.produto_id
            WHERE np.nota_id = ?
            ORDER BY p.descricao
        """;
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, notaId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public void salvar(NotaProduto np) throws SQLException {
        if (np.getId() > 0) {
            atualizar(np);
        } else {
            inserir(np);
        }
    }

    private void inserir(NotaProduto np) throws SQLException {
        String sql = """
            INSERT INTO nota_produtos (nota_id, produto_id, quantidade, vr_unitario, vr_total)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, np.getNotaId());
            ps.setInt(2, np.getProdutoId());
            setDoubleOrNull(ps, 3, np.getQuantidade());
            setDoubleOrNull(ps, 4, np.getVrUnitario());
            setDoubleOrNull(ps, 5, np.getVrTotal());
            ps.executeUpdate();
        }
    }

    private void atualizar(NotaProduto np) throws SQLException {
        String sql = """
            UPDATE nota_produtos
            SET quantidade  = ?,
                vr_unitario = ?,
                vr_total    = ?
            WHERE id = ?
        """;
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            setDoubleOrNull(ps, 1, np.getQuantidade());
            setDoubleOrNull(ps, 2, np.getVrUnitario());
            setDoubleOrNull(ps, 3, np.getVrTotal());
            ps.setInt(4, np.getId());
            ps.executeUpdate();
        }
    }

    public void excluirPorNota(int notaId) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "DELETE FROM nota_produtos WHERE nota_id = ?")) {
            ps.setInt(1, notaId);
            ps.executeUpdate();
        }
    }

    public void excluir(int id) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "DELETE FROM nota_produtos WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void setDoubleOrNull(PreparedStatement ps, int idx, Double val) throws SQLException {
        if (val != null) ps.setDouble(idx, val);
        else ps.setNull(idx, Types.REAL);
    }

    private NotaProduto mapear(ResultSet rs) throws SQLException {
        NotaProduto np = new NotaProduto();
        np.setId(rs.getInt("id"));
        np.setNotaId(rs.getInt("nota_id"));
        np.setProdutoId(rs.getInt("produto_id"));
        np.setProdutoDescricao(rs.getString("produto_descricao"));
        np.setQuantidade(rs.getObject("quantidade")  != null ? rs.getDouble("quantidade")  : null);
        np.setVrUnitario(rs.getObject("vr_unitario") != null ? rs.getDouble("vr_unitario") : null);
        np.setVrTotal(rs.getObject("vr_total")       != null ? rs.getDouble("vr_total")    : null);
        return np;
    }
}
