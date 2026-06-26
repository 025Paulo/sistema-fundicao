package com.fundicao.service;

import com.fundicao.dao.EstoqueDAO;
import com.fundicao.model.Movimentacao;
import com.fundicao.model.SaldoEstoque;

import java.sql.SQLException;
import java.util.List;

public class EstoqueService {

    private final EstoqueDAO estoqueDAO = new EstoqueDAO();

    public int registrar(Movimentacao movimentacao) throws SQLException {
        if (movimentacao.getProdutoId() <= 0)
            throw new IllegalArgumentException("Produto inválido.");
        if (movimentacao.getQuantidade() <= 0)
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        if (movimentacao.getDataMovimentacao() == null)
            throw new IllegalArgumentException("Data da movimentação é obrigatória.");

        if ("Saida".equals(movimentacao.getTipo())) {
            double saldo = estoqueDAO.getSaldo(movimentacao.getProdutoId());
            if (movimentacao.getQuantidade() > saldo)
                throw new IllegalArgumentException(
                        String.format("Saldo insuficiente! Disponível: %.2f kg", saldo));
        }

        return estoqueDAO.registrar(movimentacao);
    }

    public double getSaldo(int produtoId) throws SQLException {
        return estoqueDAO.getSaldo(produtoId);
    }

    public List<SaldoEstoque> getSaldoTodos() throws SQLException {
        return estoqueDAO.getSaldoTodos();
    }

    public List<Movimentacao> listarMovimentacoes(int produtoId) throws SQLException {
        return estoqueDAO.getHistorico(produtoId);
    }

    public void excluir(int id) throws SQLException {
        if (id <= 0) throw new IllegalArgumentException("ID inválido.");
        estoqueDAO.excluir(id);
    }
}