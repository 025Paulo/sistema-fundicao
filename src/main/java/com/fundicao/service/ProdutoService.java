package com.fundicao.service;

import com.fundicao.dao.ProdutoDAO;
import com.fundicao.dao.ProdutoFornecedorDAO;
import com.fundicao.model.Produto;
import com.fundicao.model.ProdutoFornecedor;

import java.sql.SQLException;
import java.util.List;

public class ProdutoService {

    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final ProdutoFornecedorDAO produtoFornecedorDAO = new ProdutoFornecedorDAO();

    public List<Produto> listarTodos() throws SQLException {
        return produtoDAO.listarTodos();
    }

    public List<Produto> buscar(String termo) throws SQLException {
        return produtoDAO.buscar(termo);
    }

    public Produto buscarPorId(int id) throws SQLException {
        return produtoDAO.buscarPorId(id);
    }

    public List<ProdutoFornecedor> listarFornecedoresPorProduto(int produtoId) throws SQLException {
        return produtoFornecedorDAO.listarPorProduto(produtoId);
    }

    public void excluirProduto(int produtoId) throws SQLException {
        produtoDAO.excluir(produtoId);
    }

    public void excluirFornecedorDoProduto(int produtoFornecedorId) throws SQLException {
        produtoFornecedorDAO.excluir(produtoFornecedorId);
    }

    public int salvarProduto(Produto produto, List<ProdutoFornecedor> fornecedores) throws SQLException {
        if (produto.getDescricao() == null || produto.getDescricao().isBlank()) {
            throw new IllegalArgumentException("Descrição do produto é obrigatória.");
        }

        int id;

        if (produto.getId() == 0) {
            id = produtoDAO.inserir(produto);
        } else {
            produtoDAO.atualizar(produto);
            id = produto.getId();
        }

        for (ProdutoFornecedor pf : fornecedores) {
            pf.setProdutoId(id);
            produtoFornecedorDAO.salvar(pf);
        }

        return id;
    }
}