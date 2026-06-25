package com.fundicao.dao;

import com.fundicao.model.ProdutoFornecedor;
import com.fundicao.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProdutoFornecedorDAO {

    private Connection getConnection() throws SQLException {
        return DatabaseManager.getInstance().getConnection();
    }

    public List<ProdutoFornecedor> listarPorProduto(int produtoId) {
        List<ProdutoFornecedor> lista = new ArrayList<>();
        String sql = """
            SELECT pf.*, e.razao_social as fornecedor_nome
            FROM produto_fornecedor pf
            JOIN entidades e ON e.id = pf.fornecedor_id
            WHERE pf.produto_id = ?
            ORDER BY e.razao_social
        """;
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, produtoId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar fornecedores do produto: " + e.getMessage(), e);
        }
        return lista;
    }

    public void salvar(ProdutoFornecedor pf) {
        String sql = """
            INSERT INTO produto_fornecedor (produto_id, fornecedor_id, peso_kg, vr_kg, vr_peca, vr_total)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(produto_id, fornecedor_id) DO UPDATE SET
                peso_kg  = excluded.peso_kg,
                vr_kg    = excluded.vr_kg,
                vr_peca  = excluded.vr_peca,
                vr_total = excluded.vr_total
        """;
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, pf.getProdutoId());
            ps.setInt(2, pf.getFornecedorId());
            setDoubleOrNull(ps, 3, pf.getPesoKg());
            setDoubleOrNull(ps, 4, pf.getVrKg());
            setDoubleOrNull(ps, 5, pf.getVrPeca());
            setDoubleOrNull(ps, 6, pf.getVrTotal());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar vínculo produto-fornecedor: " + e.getMessage(), e);
        }
    }

    public void excluir(int id) {
        String sql = "DELETE FROM produto_fornecedor WHERE id=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir vínculo: " + e.getMessage(), e);
        }
    }

    private void setDoubleOrNull(PreparedStatement ps, int idx, Double val) throws SQLException {
        if (val != null) ps.setDouble(idx, val);
        else ps.setNull(idx, Types.REAL);
    }

    private ProdutoFornecedor mapear(ResultSet rs) throws SQLException {
        ProdutoFornecedor pf = new ProdutoFornecedor();
        pf.setId(rs.getInt("id"));
        pf.setProdutoId(rs.getInt("produto_id"));
        pf.setFornecedorId(rs.getInt("fornecedor_id"));
        pf.setFornecedorNome(rs.getString("fornecedor_nome"));
        pf.setPesoKg(rs.getObject("peso_kg") != null ? rs.getDouble("peso_kg") : null);
        pf.setVrKg(rs.getObject("vr_kg") != null ? rs.getDouble("vr_kg") : null);
        pf.setVrPeca(rs.getObject("vr_peca") != null ? rs.getDouble("vr_peca") : null);
        pf.setVrTotal(rs.getObject("vr_total") != null ? rs.getDouble("vr_total") : null);
        return pf;
    }
}