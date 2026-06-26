package com.fundicao.controller;

import com.fundicao.model.Entidade;
import com.fundicao.model.Produto;
import com.fundicao.model.ProdutoFornecedor;
import com.fundicao.service.EntidadeService;
import com.fundicao.service.ProdutoService;
import com.fundicao.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDialogController {

    @FXML private TextField campoDescricao;
    @FXML private TextField campoNcm;

    @FXML private TableView<ProdutoFornecedor> tabelaForn;
    @FXML private TableColumn<ProdutoFornecedor, String> colFNome;
    @FXML private TableColumn<ProdutoFornecedor, String> colFPeso;
    @FXML private TableColumn<ProdutoFornecedor, String> colFVrKg;
    @FXML private TableColumn<ProdutoFornecedor, String> colFVrPeca;
    @FXML private TableColumn<ProdutoFornecedor, String> colFVrTotal;

    @FXML private ComboBox<Entidade> comboFornecedor;
    @FXML private TextField campoPeso;
    @FXML private TextField campoVrKg;
    @FXML private TextField campoVrPeca;
    @FXML private TextField campoVrTotal;

    private final ObservableList<ProdutoFornecedor> fornecedores = FXCollections.observableArrayList();
    private final EntidadeService entidadeService = new EntidadeService();
    private final ProdutoService produtoService = new ProdutoService();
    private int produtoIdExistente = -1;

    @FXML
    public void initialize() {
        colFNome.setCellValueFactory(c  -> new SimpleStringProperty(c.getValue().getFornecedorNome()));
        colFPeso.setCellValueFactory(c  -> new SimpleStringProperty(fmt(c.getValue().getPesoKg(), "%.2f kg")));
        colFVrKg.setCellValueFactory(c  -> new SimpleStringProperty(fmtM(c.getValue().getVrKg())));
        colFVrPeca.setCellValueFactory(c -> new SimpleStringProperty(fmtM(c.getValue().getVrPeca())));
        colFVrTotal.setCellValueFactory(c -> new SimpleStringProperty(fmtM(c.getValue().getVrTotal())));

        tabelaForn.setItems(fornecedores);

        try {
            List<Entidade> fornList = entidadeService.listarTodos()
                    .stream()
                    .filter(e -> "Fornecedor".equals(e.getTipo()))
                    .toList();
            comboFornecedor.setItems(FXCollections.observableArrayList(fornList));
        } catch (SQLException e) {
            AlertUtil.erro("Erro ao carregar fornecedores: " + e.getMessage());
        }

        comboFornecedor.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Entidade e) { return e == null ? "" : e.getRazaoSocial(); }
            public Entidade fromString(String s) { return null; }
        });
    }

    public void setProduto(Produto p) {
        if (p == null) return;
        produtoIdExistente = p.getId();
        campoDescricao.setText(p.getDescricao());
        campoNcm.setText(p.getClassificacaoFiscal());

        try {
            fornecedores.setAll(produtoService.listarFornecedoresPorProduto(p.getId()));
        } catch (SQLException e) {
            AlertUtil.erro("Erro ao carregar fornecedores do produto: " + e.getMessage());
        }
    }

    public Produto getProduto() {
        Produto p = new Produto();
        p.setDescricao(campoDescricao.getText());
        p.setClassificacaoFiscal(campoNcm.getText());
        return p;
    }

    public List<ProdutoFornecedor> getFornecedores() {
        return new ArrayList<>(fornecedores);
    }

    @FXML
    private void adicionarFornecedor() {
        Entidade forn = comboFornecedor.getValue();
        if (forn == null) {
            AlertUtil.aviso("Selecione um fornecedor antes de adicionar.");
            return;
        }

        boolean jaExiste = fornecedores.stream()
                .anyMatch(pf -> pf.getFornecedorId() == forn.getId());
        if (jaExiste) {
            AlertUtil.aviso("Esse fornecedor já foi adicionado.");
            return;
        }

        ProdutoFornecedor pf = new ProdutoFornecedor();
        pf.setFornecedorId(forn.getId());
        pf.setFornecedorNome(forn.getRazaoSocial());
        pf.setPesoKg(parseDouble(campoPeso.getText()));
        pf.setVrKg(parseDouble(campoVrKg.getText()));
        pf.setVrPeca(parseDouble(campoVrPeca.getText()));
        pf.setVrTotal(parseDouble(campoVrTotal.getText()));
        fornecedores.add(pf);

        comboFornecedor.setValue(null);
        campoPeso.clear();
        campoVrKg.clear();
        campoVrPeca.clear();
        campoVrTotal.clear();
    }

    @FXML
    private void removerFornecedor() {
        ProdutoFornecedor sel = tabelaForn.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtil.aviso("Selecione um fornecedor na tabela para remover.");
            return;
        }

        if (AlertUtil.confirmar("Remover vínculo com \"" + sel.getFornecedorNome() + "\"?")) {
            try {
                if (sel.getId() > 0) {
                    produtoService.excluirFornecedorDoProduto(sel.getId());
                }
                fornecedores.remove(sel);
            } catch (SQLException e) {
                AlertUtil.erro("Erro ao remover fornecedor: " + e.getMessage());
            }
        }
    }

    private Double parseDouble(String s) {
        try { return (s == null || s.isBlank()) ? null : Double.parseDouble(s.replace(",", ".")); }
        catch (NumberFormatException e) { return null; }
    }

    private String fmt(Double v, String pattern) { return v != null ? String.format(pattern, v) : "—"; }
    private String fmtM(Double v) { return v != null ? String.format("R$ %.2f", v) : "—"; }
}