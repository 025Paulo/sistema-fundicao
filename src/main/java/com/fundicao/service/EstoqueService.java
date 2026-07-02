package com.fundicao.service;

import com.fundicao.dao.EstoqueDAO;
import com.fundicao.model.Movimentacao;
import com.fundicao.model.SaldoEstoque;

import java.sql.SQLException;
import java.util.List;

public class EstoqueService {

    private final EstoqueDAO estoqueDAO = new EstoqueDAO();

    public int registrar(Movimentacao movimentacao) throws SQLException {
        validar(movimentacao, null);
        return estoqueDAO.registrar(movimentacao);
    }

    public void atualizar(Movimentacao movimentacao) throws SQLException {
        if (movimentacao.getId() <= 0)
            throw new IllegalArgumentException("ID inválido.");
        validar(movimentacao, movimentacao.getId());
        estoqueDAO.atualizar(movimentacao);
    }

    /** Cria ou atualiza dependendo se a movimentação já tem id. */
    public int salvar(Movimentacao movimentacao) throws SQLException {
        if (movimentacao.getId() > 0) {
            atualizar(movimentacao);
            return movimentacao.getId();
        }
        return registrar(movimentacao);
    }

    private void validar(Movimentacao movimentacao, Integer idParaExcluirDoSaldo) throws SQLException {
        if (movimentacao.getProdutoId() <= 0)
            throw new IllegalArgumentException("Produto inválido.");
        if (movimentacao.getQuantidade() <= 0)
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        if (movimentacao.getDataMovimentacao() == null)
            throw new IllegalArgumentException("Data da movimentação é obrigatória.");

        if ("Saida".equals(movimentacao.getTipo())) {
            // Ao editar, ignora a própria movimentação no cálculo do saldo
            // disponível — senão ela colidiria com a versão antiga dela mesma.
            double saldo = estoqueDAO.getSaldo(movimentacao.getProdutoId(), idParaExcluirDoSaldo);
            if (movimentacao.getQuantidade() > saldo)
                throw new IllegalArgumentException(
                        String.format("Saldo insuficiente! Disponível: %.2f kg", saldo));
        }
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
