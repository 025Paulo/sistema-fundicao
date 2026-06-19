package com.fundicao.dao;

import com.fundicao.model.Produto;
import com.fundicao.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDAO {

    public List<Produto> listarTodos() {
        List<Produto> lista = new ArrayList<>();
        String sql = "SELECT * FROM produtos ORDER BY descricao";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar produtos: " + e.getMessage(), e);
        }
        return lista;
    }

    public List<Produto> buscar(String termo) {
        List<Produto> lista = new ArrayList<>();
        String sql = "SELECT * FROM produtos WHERE descricao LIKE ? OR classificacao_fiscal LIKE ? ORDER BY descricao";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + termo + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar produtos: " + e.getMessage(), e);
        }
        return lista;
    }

    public int inserir(Produto p) {
        String sql = "INSERT INTO produtos (descricao, classificacao_fiscal) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getDescricao());
            ps.setString(2, p.getClassificacaoFiscal());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir produto: " + e.getMessage(), e);
        }
        return -1;
    }

    public void atualizar(Produto p) {
        String sql = "UPDATE produtos SET descricao=?, classificacao_fiscal=? WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getDescricao());
            ps.setString(2, p.getClassificacaoFiscal());
            ps.setInt(3, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar produto: " + e.getMessage(), e);
        }
    }

    public void excluir(int id) {
        String sql = "DELETE FROM produtos WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir produto: " + e.getMessage(), e);
        }
    }

    private Produto mapear(ResultSet rs) throws SQLException {
        Produto p = new Produto();
        p.setId(rs.getInt("id"));
        p.setDescricao(rs.getString("descricao"));
        p.setClassificacaoFiscal(rs.getString("classificacao_fiscal"));
        p.setCriadoEm(rs.getString("criado_em"));
        return p;
    }
}