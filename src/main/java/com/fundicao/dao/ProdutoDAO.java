package com.fundicao.dao;

import com.fundicao.model.Produto;
import com.fundicao.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDAO {

    private Connection getConnection() throws SQLException {
        return DatabaseManager.getInstance().getConnection();
    }

    public List<Produto> listarTodos() throws SQLException {
        List<Produto> lista = new ArrayList<>();
        String sql = "SELECT * FROM produtos ORDER BY descricao";
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public List<Produto> buscar(String termo) throws SQLException {
        List<Produto> lista = new ArrayList<>();
        String sql = "SELECT * FROM produtos WHERE descricao LIKE ? OR classificacao_fiscal LIKE ? ORDER BY descricao";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            String like = "%" + termo + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Produto buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM produtos WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapear(rs);
        }
        return null;
    }

    public int inserir(Produto p) throws SQLException {
        String sql = "INSERT INTO produtos (descricao, classificacao_fiscal) VALUES (?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getDescricao());
            ps.setString(2, p.getClassificacaoFiscal());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        }
        return -1;
    }

    public void atualizar(Produto p) throws SQLException {
        String sql = "UPDATE produtos SET descricao=?, classificacao_fiscal=? WHERE id=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, p.getDescricao());
            ps.setString(2, p.getClassificacaoFiscal());
            ps.setInt(3, p.getId());
            ps.executeUpdate();
        }
    }

    public void excluir(int id) throws SQLException {
        String sql = "DELETE FROM produtos WHERE id=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
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
