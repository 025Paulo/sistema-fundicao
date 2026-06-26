package com.fundicao.service;

import com.fundicao.dao.NotaFiscalDAO;
import com.fundicao.model.NotaFiscal;

import java.sql.SQLException;
import java.util.List;

public class NotaFiscalService {

    private final NotaFiscalDAO notaFiscalDAO = new NotaFiscalDAO();

    public List<NotaFiscal> listarTodas() throws SQLException {
        return notaFiscalDAO.listarTodas();
    }

    public int inserir(NotaFiscal notaFiscal) throws SQLException {
        return notaFiscalDAO.inserir(notaFiscal);
    }

    public void atualizar(NotaFiscal notaFiscal) throws SQLException {
        notaFiscalDAO.atualizar(notaFiscal);
    }

    public int salvar(NotaFiscal notaFiscal) throws SQLException {
        if (notaFiscal.getId() == 0) {
            return notaFiscalDAO.inserir(notaFiscal);
        } else {
            notaFiscalDAO.atualizar(notaFiscal);
            return notaFiscal.getId();
        }
    }

    public void excluir(int id) throws SQLException {
        notaFiscalDAO.excluir(id);
    }
}