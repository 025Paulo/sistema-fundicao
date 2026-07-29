package com.fundicao.service;

import com.fundicao.dao.NotaFiscalDAO;
import com.fundicao.dao.NotaProdutoDAO;
import com.fundicao.model.NotaFiscal;
import com.fundicao.model.NotaProduto;

import java.sql.SQLException;
import java.util.List;

public class NotaFiscalService {

    private final NotaFiscalDAO notaFiscalDAO = new NotaFiscalDAO();
    private final NotaProdutoDAO notaProdutoDAO = new NotaProdutoDAO();

    public List<NotaFiscal> listarTodas() throws SQLException {
        return notaFiscalDAO.listarTodas();
    }

    public int inserir(NotaFiscal notaFiscal) throws SQLException {
        return notaFiscalDAO.inserir(notaFiscal);
    }

    public void atualizar(NotaFiscal notaFiscal) throws SQLException {
        notaFiscalDAO.atualizar(notaFiscal);
    }

    public int salvar(NotaFiscal nf, List<NotaProduto> produtos) throws SQLException {
        int id;
        if (nf.getId() == null || nf.getId() == 0) {
            id = notaFiscalDAO.inserir(nf);
        } else {
            notaFiscalDAO.atualizar(nf);
            id = nf.getId();
        }
        for (NotaProduto np : produtos) {
            np.setNotaId(id);
            notaProdutoDAO.salvar(np);
        }
        return id;
    }

    public int salvar(NotaFiscal nf) throws SQLException {
        return salvar(nf, List.of());
    }

    public List<NotaProduto> listarProdutosPorNota(int notaId) throws SQLException {
        return notaProdutoDAO.listarPorNota(notaId);
    }

    public void excluirProdutoDaNota(int notaProdutoId) throws SQLException {
        notaProdutoDAO.excluir(notaProdutoId);
    }

    public void excluir(int id) throws SQLException {
        notaFiscalDAO.excluir(id);
    }
}