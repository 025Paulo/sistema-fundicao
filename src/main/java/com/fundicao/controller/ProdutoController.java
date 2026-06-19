package com.fundicao.controller;

import com.fundicao.dao.ProdutoDAO;
import com.fundicao.dao.ProdutoFornecedorDAO;
import com.fundicao.model.Produto;
import com.fundicao.model.ProdutoFornecedor;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ProdutoController {

    @FXML private TextField campoBusca;
    @FXML private TableView<Produto> tabela;
    @FXML private TableColumn<Produto, String> colDescricao;
    @FXML private TableColumn<Produto, String> colNCM;
    @FXML private TableColumn<Produto, String> colFornecedores;
    @FXML private Label labelTotal;

    // Painel detalhes
    @FXML private VBox painelDetalhes;
    @FXML private Label labelNomeDetalhe;
    @FXML private Label dNcm;
    @FXML private Label dCriadoEm;
    @FXML private TableView<ProdutoFornecedor> tabelaFornecedores;
    @FXML private TableColumn<ProdutoFornecedor, String> colFornNome;
    @FXML private TableColumn<ProdutoFornecedor, String> colFornPeso;
    @FXML private TableColumn<ProdutoFornecedor, String> colFornVrKg;
    @FXML private TableColumn<ProdutoFornecedor, String> colFornVrPeca;
    @FXML private TableColumn<ProdutoFornecedor, String> colFornVrTotal;

    private final ProdutoDAO dao = new ProdutoDAO();
    private final ProdutoFornecedorDAO pfDao = new ProdutoFornecedorDAO();
    private final ObservableList<Produto> dados = FXCollections.observableArrayList();
    private final ObservableList<ProdutoFornecedor> dadosFornecedores = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colDescricao.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDescricao()));
        colNCM.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getClassificacaoFiscal())));
        colFornecedores.setCellValueFactory(c -> {
            List<ProdutoFornecedor> forn = pfDao.listarPorProduto(c.getValue().getId());
            return new SimpleStringProperty(forn.size() + " fornecedor(es)");
        });

        colFornNome.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getFornecedorNome()));
        colFornPeso.setCellValueFactory(c ->
                new SimpleStringProperty(formatDouble(c.getValue().getPesoKg())));
        colFornVrKg.setCellValueFactory(c ->
                new SimpleStringProperty(formatMoeda(c.getValue().getVrKg())));
        colFornVrPeca.setCellValueFactory(c ->
                new SimpleStringProperty(formatMoeda(c.getValue().getVrPeca())));
        colFornVrTotal.setCellValueFactory(c ->
                new SimpleStringProperty(formatMoeda(c.getValue().getVrTotal())));

        tabela.setItems(dados);
        tabelaFornecedores.setItems(dadosFornecedores);
        carregarDados();

        campoBusca.textProperty().addListener((obs, a, novo) -> filtrar(novo));

        tabela.getSelectionModel().selectedItemProperty().addListener((obs, a, novo) -> {
            if (novo != null) mostrarDetalhes(novo);
            else fecharDetalhes();
        });
    }

    private void carregarDados() {
        List<Produto> lista = dao.listarTodos();
        dados.setAll(lista);
        labelTotal.setText("Total: " + lista.size() + " produtos");
    }

    private void filtrar(String termo) {
        List<Produto> lista = (termo == null || termo.isBlank())
                ? dao.listarTodos()
                : dao.buscar(termo.trim());
        dados.setAll(lista);
        labelTotal.setText("Total: " + lista.size() + " produtos");
    }

    private void mostrarDetalhes(Produto p) {
        labelNomeDetalhe.setText(p.getDescricao());
        dNcm.setText(nvl(p.getClassificacaoFiscal()));
        dCriadoEm.setText(nvl(p.getCriadoEm()));

        List<ProdutoFornecedor> forn = pfDao.listarPorProduto(p.getId());
        dadosFornecedores.setAll(forn);

        if (!painelDetalhes.isVisible()) {
            painelDetalhes.setVisible(true);
            painelDetalhes.setManaged(true);
            painelDetalhes.setOpacity(0);
            painelDetalhes.setScaleY(0.92);
            new Timeline(new KeyFrame(Duration.millis(180),
                    new KeyValue(painelDetalhes.opacityProperty(), 1),
                    new KeyValue(painelDetalhes.scaleYProperty(), 1)
            )).play();
        }
    }

    @FXML
    private void fecharDetalhes() {
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(150),
                new KeyValue(painelDetalhes.opacityProperty(), 0),
                new KeyValue(painelDetalhes.scaleYProperty(), 0.92)
        ));
        tl.setOnFinished(e -> {
            painelDetalhes.setVisible(false);
            painelDetalhes.setManaged(false);
            tabela.getSelectionModel().clearSelection();
        });
        tl.play();
    }

    @FXML
    private void adicionar() { abrirDialog(null); }

    @FXML
    private void alterar() {
        Produto s = tabela.getSelectionModel().getSelectedItem();
        if (s == null) { mostrarAviso("Selecione um produto para alterar."); return; }
        abrirDialog(s);
    }

    @FXML
    private void excluir() {
        Produto s = tabela.getSelectionModel().getSelectedItem();
        if (s == null) { mostrarAviso("Selecione um produto para excluir."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar exclusão");
        confirm.setHeaderText(null);
        confirm.setContentText("Excluir \"" + s.getDescricao() + "\"?");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            dao.excluir(s.getId());
            fecharDetalhes();
            carregarDados();
        }
    }

    private void abrirDialog(Produto produto) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/fundicao/view/produto-dialog.fxml")
            );
            VBox content = loader.load();
            ProdutoDialogController ctrl = loader.getController();
            ctrl.setProduto(produto);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(produto == null ? "Novo Produto" : "Editar Produto");
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                Produto novo = ctrl.getProduto();
                int id;
                if (produto == null) {
                    id = dao.inserir(novo);
                } else {
                    novo.setId(produto.getId());
                    dao.atualizar(novo);
                    id = novo.getId();
                }
                // salva vínculos com fornecedores
                for (ProdutoFornecedor pf : ctrl.getFornecedores()) {
                    pf.setProdutoId(id);
                    pfDao.salvar(pf);
                }
                carregarDados();
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao abrir dialog: " + e.getMessage(), e);
        }
    }

    private String nvl(String s) { return (s == null || s.isBlank()) ? "—" : s; }
    private String formatDouble(Double v) { return v != null ? String.format("%.2f kg", v) : "—"; }
    private String formatMoeda(Double v) { return v != null ? String.format("R$ %.2f", v) : "—"; }
    private void mostrarAviso(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}