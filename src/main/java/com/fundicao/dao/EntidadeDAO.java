package com.fundicao.dao;

import com.fundicao.model.Entidade;
import com.fundicao.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EntidadeDAO {

    public List<Entidade> listarTodos() {
        List<Entidade> lista = new ArrayList<>();
        String sql = "SELECT * FROM entidades ORDER BY razao_social";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar entidades: " + e.getMessage(), e);
        }
        return lista;
    }

    public List<Entidade> buscar(String termo) {
        List<Entidade> lista = new ArrayList<>();
        String sql = """
            SELECT * FROM entidades
            WHERE razao_social LIKE ? OR cnpj_cpf LIKE ? OR email LIKE ?
            ORDER BY razao_social
        """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + termo + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar entidades: " + e.getMessage(), e);
        }
        return lista;
    }

    public void inserir(Entidade e) {
        String sql = """
            INSERT INTO entidades (razao_social, tipo, tipo_pessoa, cnpj_cpf,
                inscricao_estadual, site, telefone, fax, email,
                rua, numero, complemento, bairro, cidade, uf, cep, situacao)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            preencherStatement(ps, e);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Erro ao inserir entidade: " + ex.getMessage(), ex);
        }
    }

    public void atualizar(Entidade e) {
        String sql = """
            UPDATE entidades SET razao_social=?, tipo=?, tipo_pessoa=?, cnpj_cpf=?,
                inscricao_estadual=?, site=?, telefone=?, fax=?, email=?,
                rua=?, numero=?, complemento=?, bairro=?, cidade=?, uf=?, cep=?, situacao=?
            WHERE id=?
        """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            preencherStatement(ps, e);
            ps.setInt(18, e.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Erro ao atualizar entidade: " + ex.getMessage(), ex);
        }
    }

    public void excluir(int id) {
        String sql = "DELETE FROM entidades WHERE id=?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir entidade: " + e.getMessage(), e);
        }
    }

    private void preencherStatement(PreparedStatement ps, Entidade e) throws SQLException {
        ps.setString(1,  e.getRazaoSocial());
        ps.setString(2,  e.getTipo());
        ps.setString(3,  e.getTipoPessoa());
        ps.setString(4,  e.getCnpjCpf());
        ps.setString(5,  e.getInscricaoEstadual());
        ps.setString(6,  e.getSite());
        ps.setString(7,  e.getTelefone());
        ps.setString(8,  e.getFax());
        ps.setString(9,  e.getEmail());
        ps.setString(10, e.getRua());
        ps.setString(11, e.getNumero());
        ps.setString(12, e.getComplemento());
        ps.setString(13, e.getBairro());
        ps.setString(14, e.getCidade());
        ps.setString(15, e.getUf());
        ps.setString(16, e.getCep());
        ps.setString(17, e.getSituacao() != null ? e.getSituacao() : "Ativo");
    }

    private Entidade mapear(ResultSet rs) throws SQLException {
        Entidade e = new Entidade();
        e.setId(rs.getInt("id"));
        e.setRazaoSocial(rs.getString("razao_social"));
        e.setTipo(rs.getString("tipo"));
        e.setTipoPessoa(rs.getString("tipo_pessoa"));
        e.setCnpjCpf(rs.getString("cnpj_cpf"));
        e.setInscricaoEstadual(rs.getString("inscricao_estadual"));
        e.setSite(rs.getString("site"));
        e.setTelefone(rs.getString("telefone"));
        e.setFax(rs.getString("fax"));
        e.setEmail(rs.getString("email"));
        e.setRua(rs.getString("rua"));
        e.setNumero(rs.getString("numero"));
        e.setComplemento(rs.getString("complemento"));
        e.setBairro(rs.getString("bairro"));
        e.setCidade(rs.getString("cidade"));
        e.setUf(rs.getString("uf"));
        e.setCep(rs.getString("cep"));
        e.setSituacao(rs.getString("situacao"));
        e.setCriadoEm(rs.getString("criado_em"));
        return e;
    }
}