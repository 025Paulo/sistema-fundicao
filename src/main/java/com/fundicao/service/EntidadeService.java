package com.fundicao.service;

import com.fundicao.dao.EntidadeDAO;
import com.fundicao.model.Entidade;

import java.sql.SQLException;
import java.util.List;

public class EntidadeService {

    private final EntidadeDAO entidadeDAO = new EntidadeDAO();

    public List<Entidade> listarTodos() throws SQLException {
        return entidadeDAO.listarTodos();
    }

    public List<Entidade> buscar(String termo) throws SQLException {
        return entidadeDAO.buscar(termo);
    }

    public void inserir(Entidade entidade) throws SQLException {
        validar(entidade);
        entidadeDAO.inserir(entidade);
    }

    public void atualizar(Entidade entidade) throws SQLException {
        validar(entidade);
        entidadeDAO.atualizar(entidade);
    }

    public void salvar(Entidade entidade) throws SQLException {
        validar(entidade);
        if (entidade.getId() == 0) {
            entidadeDAO.inserir(entidade);
        } else {
            entidadeDAO.atualizar(entidade);
        }
    }

    public void excluir(int id) throws SQLException {
        try {
            entidadeDAO.excluir(id);
        } catch (SQLException e) {
            // erro de FK: entidade possui vínculos (NF, movimentações)
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("foreign key")) {
                throw new IllegalStateException(
                        "Não é possível excluir esta entidade pois ela possui notas fiscais ou movimentações vinculadas.");
            }
            throw e;
        }
    }

    private void validar(Entidade entidade) {
        if (entidade.getRazaoSocial() == null || entidade.getRazaoSocial().isBlank()) {
            throw new IllegalArgumentException("Razão social é obrigatória.");
        }
        if (entidade.getTipo() == null || entidade.getTipo().isBlank()) {
            throw new IllegalArgumentException("Tipo (Cliente/Fornecedor) é obrigatório.");
        }
    }
}