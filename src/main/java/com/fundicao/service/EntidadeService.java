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
        entidadeDAO.inserir(entidade);
    }

    public void atualizar(Entidade entidade) throws SQLException {
        entidadeDAO.atualizar(entidade);
    }

    public void salvar(Entidade entidade) throws SQLException {
        if (entidade.getId() == 0) {
            entidadeDAO.inserir(entidade);
        } else {
            entidadeDAO.atualizar(entidade);
        }
    }

    public void excluir(int id) throws SQLException {
        entidadeDAO.excluir(id);
    }
}