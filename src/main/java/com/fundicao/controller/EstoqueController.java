package com.fundicao.controller;

import com.fundicao.model.Produto;
import com.fundicao.model.SaldoEstoque;
import com.fundicao.service.EstoqueService;
import com.fundicao.service.ProdutoService;
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
    @FXML private TableView<SaldoEstoque> tabela;
    @FXML private TableColumn<SaldoEstoque, String> colProduto;
    @FXML private TableColumn<SaldoEstoque, String> colQuantidade;
    @FXML private TableColumn<SaldoEstoque, String> colUltimoTipo;
    @FXML private TableColumn<SaldoEstoque, String> colUltimaMovimentacao;
    @FXML private Label labelTotal;

    @FXML private VBox painelDetalhes;
    @FXML private Label labelNomeDetalhe;
    @FXML private Label dQuantidade;
    @FXML private Label dUltimoTipo;
    @FXML private Label dUltimaMovimentacao;

    private final EstoqueService estoqueService = new EstoqueService();
    private final ProdutoService produtoService = new ProdutoService();
    private final ObservableList<SaldoEstoque> dados = FXCollections.observableArrayList();
    private List<SaldoEstoque> todosOsSaldos;

    @FXML
    public void initialize() {
        colProduto.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDescricao()));
        colQuantidade.setCellValueFactory(c ->
                new SimpleStringProperty(String.format("%.3f", c.getValue().getSaldo())));
        colUltimoTipo.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getUltimoTipo())));
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
            todosOsSaldos = estoqueService.getSaldoTodos();
            dados.setAll(todosOsSaldos);
            labelTotal.setText("Total: " + todosOsSaldos.size() + " itens");
        } catch (SQLException e) {
            AlertUtil.erro("Erro ao carregar estoque: " + e.getMessage());
        }
    }

    private void filtrar(String termo) {
        if (todosOsSaldos == null) return;
        if (termo == null || termo.isBlank()) {
            dados.setAll(todosOsSaldos);
        } else {
            String lower = termo.toLowerCase();
            dados.setAll(todosOsSaldos.stream()
                    .filter(s -> s.getDescricao().toLowerCase().contains(lower))
                    .toList());
        }
        labelTotal.setText("Total: " + dados.size() + " itens");
    }

    private void mostrarDetalhes(SaldoEstoque s) {
        labelNomeDetalhe.setText(s.getDescricao());
        dQuantidade.setText(String.format("%.3f", s.getSaldo()));
        dUltimoTipo.setText(nvl(s.getUltimoTipo()));
        dUltimaMovimentacao.setText(nvl(s.getUltimaMovimentacao()));

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
        SaldoEstoque sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtil.aviso("Selecione um item do estoque para movimentar.");
            return;
        }
        abrirDialogMovimentacao(sel);
    }

    private void abrirDialogMovimentacao(SaldoEstoque saldo) {
        try {
            // Busca o objeto Produto completo para passar ao dialog
            Produto produto = produtoService.listarTodos().stream()
                    .filter(p -> p.getId() == saldo.getProdutoId())
                    .findFirst().orElse(null);

            if (produto == null) {
                AlertUtil.erro("Produto n\u00e3o encontrado.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/fundicao/view/movimentacao-dialog.fxml"));
            VBox content = loader.load();
            MovimentacaoDialogController ctrl = loader.getController();
            ctrl.setTipo("Entrada");
            ctrl.setProduto(produto);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Movimentar Estoque \u2014 " + saldo.getDescricao());
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            // O dialog cuida do pr\u00f3prio bot\u00e3o salvar internamente,
            // mas se usar bot\u00f5es do DialogPane precisamos checar isSalvo()
            Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            okBtn.setVisible(false); // o formul\u00e1rio usa seu pr\u00f3prio bot\u00e3o Salvar

            dialog.showAndWait();
            if (ctrl.isSalvo()) carregarDados();

        } catch (IOException | SQLException e) {
            AlertUtil.erro("Erro ao abrir formul\u00e1rio: " + e.getMessage());
        }
    }

    private String nvl(String s) { return (s == null || s.isBlank()) ? "\u2014" : s; }
}
