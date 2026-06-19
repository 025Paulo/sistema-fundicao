package com.fundicao.controller;

import com.fundicao.dao.EntidadeDAO;
import com.fundicao.dao.ProdutoFornecedorDAO;
import com.fundicao.model.Entidade;
import com.fundicao.model.Produto;
import com.fundicao.model.ProdutoFornecedor;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private final EntidadeDAO entidadeDAO = new EntidadeDAO();
    private int produtoIdExistente = -1;

    @FXML
    public void initialize() {
        colFNome.setCellValueFactory(c  -> new SimpleStringProperty(c.getValue().getFornecedorNome()));
        colFPeso.setCellValueFactory(c  -> new SimpleStringProperty(fmt(c.getValue().getPesoKg(), "%.2f kg")));
        colFVrKg.setCellValueFactory(c  -> new SimpleStringProperty(fmtM(c.getValue().getVrKg())));
        colFVrPeca.setCellValueFactory(c -> new SimpleStringProperty(fmtM(c.getValue().getVrPeca())));
        colFVrTotal.setCellValueFactory(c -> new SimpleStringProperty(fmtM(c.getValue().getVrTotal())));

        tabelaForn.setItems(fornecedores);

        // carrega só fornecedores
        List<Entidade> fornList = entidadeDAO.listarTodos()
                .stream()
                .filter(e -> "Fornecedor".equals(e.getTipo()))
                .toList();
        comboFornecedor.setItems(FXCollections.observableArrayList(fornList));

        comboFornecedor.setConverter(new javafx.util.StringConverter<Entidade>() {
            @Override
            public String toString(Entidade e) {
                return e == null ? "" : e.getRazaoSocial();
            }
            @Override
            public Entidade fromString(String s) { return null; }
        });
    }



    public void setProduto(Produto p) {
        if (p == null) return;
        produtoIdExistente = p.getId();
        campoDescricao.setText(p.getDescricao());
        campoNcm.setText(p.getClassificacaoFiscal());

        // carrega vínculos existentes
        ProdutoFornecedorDAO pfDao = new ProdutoFornecedorDAO();
        fornecedores.setAll(pfDao.listarPorProduto(p.getId()));
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
        if (forn == null) return;

        // evita duplicata na lista local
        boolean jaExiste = fornecedores.stream()
                .anyMatch(pf -> pf.getFornecedorId() == forn.getId());
        if (jaExiste) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setHeaderText(null);
            a.setContentText("Esse fornecedor já foi adicionado.");
            a.showAndWait();
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

        // limpa campos
        comboFornecedor.setValue(null);
        campoPeso.clear(); campoVrKg.clear();
        campoVrPeca.clear(); campoVrTotal.clear();
    }

    @FXML
    private void removerFornecedor() {
        ProdutoFornecedor sel = tabelaForn.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText(null);
        confirm.setContentText("Remover vínculo com \"" + sel.getFornecedorNome() + "\"?");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            if (sel.getId() > 0) {
                new ProdutoFornecedorDAO().excluir(sel.getId());
            }
            fornecedores.remove(sel);
        }
    }

    private Double parseDouble(String s) {
        try { return (s == null || s.isBlank()) ? null : Double.parseDouble(s.replace(",", ".")); }
        catch (NumberFormatException e) { return null; }
    }

    private String fmt(Double v, String pattern) { return v != null ? String.format(pattern, v) : "—"; }
    private String fmtM(Double v) { return v != null ? String.format("R$ %.2f", v) : "—"; }
}