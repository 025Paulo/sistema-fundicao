package com.fundicao.service;

import com.fundicao.dao.EstoqueDAO;
import com.fundicao.model.Movimentacao;
import com.fundicao.model.SaldoEstoque;

import java.sql.SQLException;
import java.util.List;

public class EstoqueService {

    private final EstoqueDAO estoqueDAO = new EstoqueDAO();

    public int registrar(Movimentacao movimentacao) throws SQLException {
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
        estoqueDAO.excluir(id);
    }
}