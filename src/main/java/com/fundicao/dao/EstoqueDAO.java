package com.fundicao.dao;

import com.fundicao.model.Movimentacao;
import com.fundicao.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EstoqueDAO {

    private Connection getConnection() throws SQLException {
        return DatabaseManager.getInstance().getConnection();
    }

    public int registrar(Movimentacao m) throws SQLException {
        String sql = """
            INSERT INTO estoque_movimentacoes
                (produto_id, tipo, quantidade, data_movimentacao,
                 nota_id, valor_unitario, entidade_id,
                 transportadora, ordem_compra, observacoes)
            VALUES (?,?,?,?,?,?,?,?,?,?)
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, m.getProdutoId());
            ps.setString(2, m.getTipo());
            ps.setDouble(3, m.getQuantidade());
            ps.setString(4, m.getDataMovimentacao().toString());
            if (m.getNotaId() != null) ps.setInt(5, m.getNotaId()); else ps.setNull(5, Types.INTEGER);
            if (m.getValorUnitario() != null) ps.setDouble(6, m.getValorUnitario()); else ps.setNull(6, Types.REAL);
            if (m.getEntidadeId() != null) ps.setInt(7, m.getEntidadeId()); else ps.setNull(7, Types.INTEGER);
            ps.setString(8, m.getTransportadora());
            ps.setString(9, m.getOrdemCompra());
            ps.setString(10, m.getObservacoes());

            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    public double getSaldo(int produtoId) throws SQLException {
        String sql = """
            SELECT COALESCE(
                SUM(CASE WHEN tipo='Entrada' THEN quantidade ELSE -quantidade END), 0
            ) FROM estoque_movimentacoes WHERE produto_id = ?
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, produtoId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    public List<Object[]> getSaldoTodos() throws SQLException {
        String sql = """
            SELECT p.id, p.descricao,
                   SUM(CASE WHEN tipo='Entrada' THEN quantidade ELSE -quantidade END) AS saldo,
                   MAX(em.data_movimentacao) AS ultima_mov,
                   (SELECT tipo FROM estoque_movimentacoes
                    WHERE produto_id = p.id
                    ORDER BY data_movimentacao DESC LIMIT 1) AS ultimo_tipo
            FROM produtos p
            INNER JOIN estoque_movimentacoes em ON em.produto_id = p.id
            GROUP BY p.id, p.descricao
            ORDER BY p.descricao
        """;

        List<Object[]> dados = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                dados.add(new Object[]{
                        rs.getInt("id"),
                        rs.getString("descricao"),
                        rs.getDouble("saldo"),
                        rs.getString("ultima_mov"),
                        rs.getString("ultimo_tipo")
                });
            }
        }

        return dados;
    }

    public List<Movimentacao> getHistorico(int produtoId) throws SQLException {
        String sql = """
            SELECT em.*, e.razao_social AS entidade_nome
            FROM estoque_movimentacoes em
            LEFT JOIN entidades e ON e.id = em.entidade_id
            WHERE em.produto_id = ?
            ORDER BY em.data_movimentacao DESC, em.id DESC
        """;
        List<Movimentacao> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, produtoId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public void excluir(int id) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM estoque_movimentacoes WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Movimentacao mapear(ResultSet rs) throws SQLException {
        Movimentacao m = new Movimentacao();
        m.setId(rs.getInt("id"));
        m.setProdutoId(rs.getInt("produto_id"));
        m.setTipo(rs.getString("tipo"));
        m.setQuantidade(rs.getDouble("quantidade"));

        String data = rs.getString("data_movimentacao");
        if (data != null) m.setDataMovimentacao(LocalDate.parse(data));

        int notaId = rs.getInt("nota_id");
        if (!rs.wasNull()) m.setNotaId(notaId);

        double vrUnit = rs.getDouble("valor_unitario");
        if (!rs.wasNull()) m.setValorUnitario(vrUnit);

        int entId = rs.getInt("entidade_id");
        if (!rs.wasNull()) m.setEntidadeId(entId);

        m.setEntidadeNome(rs.getString("entidade_nome"));
        m.setTransportadora(rs.getString("transportadora"));
        m.setOrdemCompra(rs.getString("ordem_compra"));
        m.setObservacoes(rs.getString("observacoes"));

        String criadoEm = rs.getString("criado_em");
        if (criadoEm != null) m.setCriadoEm(LocalDateTime.parse(criadoEm.replace(" ", "T")));

        return m;
    }
}