package com.fundicao.controller;

import com.fundicao.model.Produto;
import com.fundicao.model.ProdutoFornecedor;
import com.fundicao.service.ProdutoService;
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
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ProdutoController {

    @FXML private TextField campoBusca;
    @FXML private TableView<Produto> tabela;
    @FXML private TableColumn<Produto, String> colDescricao;
    @FXML private TableColumn<Produto, String> colNCM;
    @FXML private TableColumn<Produto, String> colFornecedores;
    @FXML private Label labelTotal;

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

    private final ProdutoService service = new ProdutoService();
    private final ObservableList<Produto> dados = FXCollections.observableArrayList();
    private final ObservableList<ProdutoFornecedor> dadosFornecedores = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colDescricao.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDescricao()));
        colNCM.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getClassificacaoFiscal())));
        colFornecedores.setCellValueFactory(c -> {
            try {
                List<ProdutoFornecedor> forn = service.listarFornecedoresPorProduto(c.getValue().getId());
                return new SimpleStringProperty(forn.size() + " fornecedor(es)");
            } catch (SQLException e) {
                return new SimpleStringProperty("erro");
            }
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
        try {
            List<Produto> lista = service.listarTodos();
            dados.setAll(lista);
            labelTotal.setText("Total: " + lista.size() + " produtos");
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar produtos: " + e.getMessage());
        }
    }

    private void filtrar(String termo) {
        try {
            List<Produto> lista = (termo == null || termo.isBlank())
                    ? service.listarTodos()
                    : service.buscar(termo.trim());
            dados.setAll(lista);
            labelTotal.setText("Total: " + lista.size() + " produtos");
        } catch (SQLException e) {
            mostrarErro("Erro ao filtrar produtos: " + e.getMessage());
        }
    }

    private void mostrarDetalhes(Produto p) {
        try {
            labelNomeDetalhe.setText(p.getDescricao());
            dNcm.setText(nvl(p.getClassificacaoFiscal()));
            dCriadoEm.setText(nvl(p.getCriadoEm()));

            List<ProdutoFornecedor> forn = service.listarFornecedoresPorProduto(p.getId());
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
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar detalhes: " + e.getMessage());
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
        if (s == null) {
            mostrarAviso("Selecione um produto para alterar.");
            return;
        }
        abrirDialog(s);
    }

    @FXML
    private void excluir() {
        Produto s = tabela.getSelectionModel().getSelectedItem();
        if (s == null) {
            mostrarAviso("Selecione um produto para excluir.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar exclusão");
        confirm.setHeaderText(null);
        confirm.setContentText("Excluir \"" + s.getDescricao() + "\"?");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                service.excluirProduto(s.getId());
                fecharDetalhes();
                carregarDados();
            } catch (SQLException e) {
                mostrarErro("Erro ao excluir produto: " + e.getMessage());
            }
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
                if (produto != null) {
                    novo.setId(produto.getId());
                }
                service.salvarProduto(novo, ctrl.getFornecedores());
                carregarDados();
            }
        } catch (IOException e) {
            mostrarErro("Erro ao abrir formulário: " + e.getMessage());
        } catch (SQLException e) {
            mostrarErro("Erro ao salvar produto: " + e.getMessage());
        }
    }

    private String nvl(String s) { return (s == null || s.isBlank()) ? "—" : s; }
    private String formatDouble(Double v) { return v != null ? String.format("%.2f kg", v) : "—"; }
    private String formatMoeda(Double v) { return v != null ? String.format("R$ %.2f", v) : "—"; }

    private void mostrarAviso(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void mostrarErro(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erro");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}