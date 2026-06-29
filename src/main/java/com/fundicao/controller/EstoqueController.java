package com.fundicao.controller;

import com.fundicao.model.Estoque;
import com.fundicao.service.EstoqueService;
import com.fundicao.util.AlertUtil;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class EstoqueController {

    @FXML private TextField campoBusca;
    @FXML private TableView<Estoque> tabela;
    @FXML private TableColumn<Estoque, String> colProduto;
    @FXML private TableColumn<Estoque, String> colQuantidade;
    @FXML private TableColumn<Estoque, String> colUnidade;
    @FXML private TableColumn<Estoque, String> colUltimaMovimentacao;
    @FXML private Label labelTotal;

    @FXML private VBox painelDetalhes;
    @FXML private Label labelNomeDetalhe;
    @FXML private Label dQuantidade;
    @FXML private Label dUnidade;
    @FXML private Label dUltimaMovimentacao;

    private final EstoqueService service = new EstoqueService();
    private final ObservableList<Estoque> dados = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colProduto.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getProdutoNome()));
        colQuantidade.setCellValueFactory(c ->
                new SimpleStringProperty(formatQtd(c.getValue().getQuantidade())));
        colUnidade.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getUnidade())));
        colUltimaMovimentacao.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getUltimaMovimentacao())));

        tabela.setItems(dados);
        tabela.getSelectionModel().setCellSelectionEnabled(true);
        configurarCopiarCelula(tabela);
        carregarDados();

        campoBusca.textProperty().addListener((obs, a, novo) -> filtrar(novo));

        tabela.getSelectionModel().selectedItemProperty().addListener((obs, a, novo) -> {
            if (novo != null) mostrarDetalhes(novo);
            else fecharDetalhes();
        });
    }

    private <T> void configurarCopiarCelula(TableView<T> tv) {
        tv.setOnKeyPressed(event -> {
            if (new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN).match(event)) {
                copiarCelulaSelecionada(tv);
            }
        });
        MenuItem itemCopiar = new MenuItem("Copiar");
        itemCopiar.setOnAction(e -> copiarCelulaSelecionada(tv));
        tv.setContextMenu(new ContextMenu(itemCopiar));
    }

    private <T> void copiarCelulaSelecionada(TableView<T> tv) {
        TablePosition<?, ?> pos = tv.getFocusModel().getFocusedCell();
        if (pos == null || pos.getTableColumn() == null) return;
        @SuppressWarnings("unchecked")
        TableColumn<T, ?> col = (TableColumn<T, ?>) pos.getTableColumn();
        T item = tv.getItems().get(pos.getRow());
        Object valor = col.getCellObservableValue(item).getValue();
        if (valor != null) {
            ClipboardContent content = new ClipboardContent();
            content.putString(valor.toString());
            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    private void carregarDados() {
        try {
            List<Estoque> lista = service.listarTodos();
            dados.setAll(lista);
            labelTotal.setText("Total: " + lista.size() + " itens");
        } catch (SQLException e) {
            AlertUtil.erro("Erro ao carregar estoque: " + e.getMessage());
        }
    }

    private void filtrar(String termo) {
        try {
            List<Estoque> lista = (termo == null || termo.isBlank())
                    ? service.listarTodos()
                    : service.buscar(termo.trim());
            dados.setAll(lista);
            labelTotal.setText("Total: " + lista.size() + " itens");
        } catch (SQLException e) {
            AlertUtil.erro("Erro ao filtrar estoque: " + e.getMessage());
        }
    }

    private void mostrarDetalhes(Estoque e) {
        labelNomeDetalhe.setText(e.getProdutoNome());
        dQuantidade.setText(formatQtd(e.getQuantidade()));
        dUnidade.setText(nvl(e.getUnidade()));
        dUltimaMovimentacao.setText(nvl(e.getUltimaMovimentacao()));

        if (!painelDetalhes.isVisible()) {
            painelDetalhes.setVisible(true);
            painelDetalhes.setManaged(true);
            painelDetalhes.setOpacity(0);
            painelDetalhes.setScaleY(0.92);
            new Timeline(new KeyFrame(Duration.millis(180),
                    new KeyValue(painelDetalhes.opacityProperty(), 1),
                    new KeyValue(painelDetalhes.scaleYProperty(), 1))).play();
        }
    }

    @FXML
    private void fecharDetalhes() {
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(150),
                new KeyValue(painelDetalhes.opacityProperty(), 0),
                new KeyValue(painelDetalhes.scaleYProperty(), 0.92)));
        tl.setOnFinished(e -> {
            painelDetalhes.setVisible(false);
            painelDetalhes.setManaged(false);
            tabela.getSelectionModel().clearSelection();
        });
        tl.play();
    }

    @FXML
    private void movimentar() {
        Estoque sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtil.aviso("Selecione um item do estoque para movimentar.");
            return;
        }
        abrirDialogMovimentacao(sel);
    }

    private void abrirDialogMovimentacao(Estoque estoque) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/fundicao/view/movimentacao-dialog.fxml"));
            VBox content = loader.load();
            MovimentacaoDialogController ctrl = loader.getController();
            ctrl.setEstoque(estoque);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Movimentar Estoque");
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    try {
                        service.registrarMovimentacao(ctrl.getMovimentacao());
                        carregarDados();
                    } catch (SQLException e) {
                        AlertUtil.erro("Erro ao registrar movimentação: " + e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            AlertUtil.erro("Erro ao abrir formulário: " + e.getMessage());
        }
    }

    private String nvl(String s) { return (s == null || s.isBlank()) ? "—" : s; }
    private String formatQtd(Double v) { return v != null ? String.format("%.3f", v) : "—"; }
}
