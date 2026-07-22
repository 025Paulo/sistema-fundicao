package com.fundicao.controller;

import com.fundicao.model.Movimentacao;
import com.fundicao.model.SaldoEstoque;
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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class EstoqueController {

    @FXML private TextField campoBusca;
    @FXML private ComboBox<String> filtroTipo;
    @FXML private TableView<SaldoEstoque> tabela;
    @FXML private TableColumn<SaldoEstoque, String> colProduto;
    @FXML private TableColumn<SaldoEstoque, String> colSaldo;
    @FXML private TableColumn<SaldoEstoque, String> colUltimaMov;
    @FXML private Label labelTotal;

    @FXML private VBox painelHistorico;
    @FXML private Label labelNomeProduto;
    @FXML private Label labelSaldoDetalhe;
    @FXML private TableView<Movimentacao> tabelaHistorico;
    @FXML private TableColumn<Movimentacao, String> colHistTipo;
    @FXML private TableColumn<Movimentacao, String> colHistQtd;
    @FXML private TableColumn<Movimentacao, String> colHistEntidade;
    @FXML private TableColumn<Movimentacao, String> colHistData;
    @FXML private TableColumn<Movimentacao, String> colHistValor;
    @FXML private TableColumn<Movimentacao, String> colHistOrdem;
    @FXML private TableColumn<Movimentacao, String> colHistObs;

    private final EstoqueService estoqueService = new EstoqueService();
    private final ObservableList<SaldoEstoque> dados = FXCollections.observableArrayList();
    private final ObservableList<Movimentacao> dadosHistorico = FXCollections.observableArrayList();
    private List<SaldoEstoque> todosOsSaldos;
    private SaldoEstoque saldoAtual;

    /** Formata um valor em kg sem zeros decimais desnecessários. Ex: 150.0 → "150 kg", 150.5 → "150,5 kg" */
    private static String formatarKg(double valor) {
        DecimalFormat df = new DecimalFormat("#,##0.###", new DecimalFormatSymbols(new Locale("pt", "BR")));
        return df.format(valor) + " kg";
    }

    /** Formata quantidade sem unidade e sem zeros decimais desnecessários. Ex: 150.0 → "150", 150.5 → "150,5" */
    private static String formatarQtd(double valor) {
        DecimalFormat df = new DecimalFormat("#,##0.###", new DecimalFormatSymbols(new Locale("pt", "BR")));
        return df.format(valor);
    }

    @FXML
    public void initialize() {
        colProduto.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDescricao()));
        colSaldo.setCellValueFactory(c ->
                new SimpleStringProperty(formatarKg(c.getValue().getSaldo())));
        colUltimaMov.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getUltimaMovimentacao())));

        tabela.setItems(dados);
        tabela.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tabelaHistorico.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        configurarCopiar(tabela);

        colHistTipo.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getTipo())));
        colHistQtd.setCellValueFactory(c ->
                new SimpleStringProperty(formatarQtd(c.getValue().getQuantidade())));
        colHistEntidade.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getEntidadeNome())));
        colHistData.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getDataMovimentacao() != null
                                ? c.getValue().getDataMovimentacao().toString() : "—"));
        colHistValor.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getValorUnitario() != null
                                ? String.format("R$ %.2f", c.getValue().getValorUnitario()) : "—"));
        colHistOrdem.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getOrdemCompra())));
        colHistObs.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getObservacoes())));

        tabelaHistorico.setItems(dadosHistorico);
        configurarCopiar(tabelaHistorico);

        filtroTipo.setItems(FXCollections.observableArrayList(
                "Todos",
                "Com movimentação",
                "Sem movimentação",
                "Última Entrada",
                "Última Saída"
        ));
        filtroTipo.setValue("Todos");
        filtroTipo.valueProperty().addListener((obs, a, novo) ->
                filtrar(campoBusca.getText()));

        carregarDados();

        campoBusca.textProperty().addListener((obs, a, novo) -> filtrar(novo));

        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) mostrarHistorico(novo);
            else fecharHistorico();
        });
    }

    private <T> void configurarCopiar(TableView<T> tv) {
        tv.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN).match(event))
                copiarCelula(tv);
        });
        MenuItem item = new MenuItem("Copiar");
        item.setOnAction(e -> copiarCelula(tv));
        tv.setContextMenu(new ContextMenu(item));
    }

    private <T> void copiarCelula(TableView<T> tv) {
        TablePosition<?, ?> pos = tv.getFocusModel().getFocusedCell();
        if (pos == null || pos.getTableColumn() == null) return;
        @SuppressWarnings("unchecked")
        TableColumn<T, ?> col = (TableColumn<T, ?>) pos.getTableColumn();
        T item = tv.getItems().get(pos.getRow());
        Object valor = col.getCellObservableValue(item).getValue();
        if (valor != null) {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(valor.toString());
            Clipboard.getSystemClipboard().setContent(cc);
        }
    }

    private void carregarDados() {
        try {
            todosOsSaldos = estoqueService.getSaldoTodos();
            dados.setAll(todosOsSaldos);
            labelTotal.setText("Total: " + todosOsSaldos.size() + " produtos");
        } catch (SQLException e) {
            AlertUtil.erro("Erro ao carregar estoque: " + e.getMessage());
        }
    }

    private void filtrar(String termo) {
        if (todosOsSaldos == null) return;

        String lower = (termo == null) ? "" : termo.toLowerCase();
        String tipo = filtroTipo.getValue();

        dados.setAll(todosOsSaldos.stream()
                .filter(s -> lower.isBlank() || s.getDescricao().toLowerCase().contains(lower))
                .filter(s -> {
                    if (tipo == null || "Todos".equals(tipo)) return true;
                    if ("Com movimentação".equals(tipo)) return s.getUltimaMovimentacao() != null;
                    if ("Sem movimentação".equals(tipo)) return s.getUltimaMovimentacao() == null;
                    if ("Última Entrada".equals(tipo)) return "Entrada".equalsIgnoreCase(nvl(s.getUltimoTipo()));
                    if ("Última Saída".equals(tipo)) return "Saida".equalsIgnoreCase(nvl(s.getUltimoTipo()));
                    return true;
                })
                .toList());

        labelTotal.setText("Total: " + dados.size() + " produtos");
    }

    private void mostrarHistorico(SaldoEstoque s) {
        saldoAtual = s;
        tabelaHistorico.getSelectionModel().clearSelection();
        labelNomeProduto.setText(s.getDescricao());
        labelSaldoDetalhe.setText(formatarKg(s.getSaldo()));

        try {
            List<Movimentacao> hist = estoqueService.listarMovimentacoes(s.getProdutoId());
            dadosHistorico.setAll(hist);
        } catch (SQLException e) {
            AlertUtil.erro("Erro ao carregar histórico: " + e.getMessage());
        }

        if (!painelHistorico.isVisible()) {
            painelHistorico.setVisible(true);
            painelHistorico.setManaged(true);
            painelHistorico.setOpacity(0);
            painelHistorico.setScaleY(0.92);
            new Timeline(new KeyFrame(Duration.millis(180),
                    new KeyValue(painelHistorico.opacityProperty(), 1),
                    new KeyValue(painelHistorico.scaleYProperty(), 1))).play();
        }
    }

    @FXML
    private void fecharHistorico() {
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(150),
                new KeyValue(painelHistorico.opacityProperty(), 0),
                new KeyValue(painelHistorico.scaleYProperty(), 0.92)));
        tl.setOnFinished(e -> {
            painelHistorico.setVisible(false);
            painelHistorico.setManaged(false);
            tabela.getSelectionModel().clearSelection();
            saldoAtual = null;
        });
        tl.play();
    }

    @FXML
    private void novaEntrada() { abrirDialog("Entrada"); }

    @FXML
    private void novaSaida() { abrirDialog("Saida"); }

    private void abrirDialog(String tipo) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/fundicao/view/movimentacao-dialog.fxml"));
            Parent root = loader.load();
            MovimentacaoDialogController ctrl = loader.getController();
            ctrl.setTipo(tipo);
            if (saldoAtual != null) ctrl.setProdutoById(saldoAtual.getProdutoId());
            Stage stage = new Stage();
            stage.setTitle(tipo.equals("Entrada") ? "Nova Entrada" : "Nova Saída");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            if (ctrl.isSalvo()) {
                carregarDados();
                if (saldoAtual != null) {
                    int idAtual = saldoAtual.getProdutoId();
                    todosOsSaldos.stream()
                            .filter(s -> s.getProdutoId() == idAtual)
                            .findFirst()
                            .ifPresent(s -> {
                                tabela.getSelectionModel().select(s);
                                mostrarHistorico(s);
                            });
                }
            }
        } catch (IOException e) {
            AlertUtil.erro("Erro ao abrir formulário: " + e.getMessage());
        }
    }

    @FXML
    private void alterarMovimentacao() {
        Movimentacao sel = tabelaHistorico.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtil.aviso("Selecione uma movimentação no histórico para alterar.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/fundicao/view/movimentacao-dialog.fxml"));
            Parent root = loader.load();
            MovimentacaoDialogController ctrl = loader.getController();
            ctrl.setMovimentacao(sel);
            Stage stage = new Stage();
            stage.setTitle("Editar Movimentação");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            if (ctrl.isSalvo()) {
                int idAtual = saldoAtual != null ? saldoAtual.getProdutoId() : -1;
                carregarDados();
                if (idAtual > 0) {
                    todosOsSaldos.stream()
                            .filter(s -> s.getProdutoId() == idAtual)
                            .findFirst()
                            .ifPresentOrElse(s -> {
                                tabela.getSelectionModel().select(s);
                                mostrarHistorico(s);
                            }, this::fecharHistorico);
                }
            }
        } catch (IOException e) {
            AlertUtil.erro("Erro ao abrir formulário: " + e.getMessage());
        }
    }

    @FXML
    private void excluirMovimentacao() {
        Movimentacao sel = tabelaHistorico.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtil.aviso("Selecione uma movimentação no histórico para excluir.");
            return;
        }

        if (AlertUtil.confirmar("Excluir movimentação de " +
                formatarKg(sel.getQuantidade()) +
                " (" + nvl(sel.getTipo()) + ")?")) {
            try {
                int idAtual = saldoAtual != null ? saldoAtual.getProdutoId() : -1;

                estoqueService.excluir(sel.getId());
                carregarDados();

                if (idAtual > 0) {
                    todosOsSaldos.stream()
                            .filter(s -> s.getProdutoId() == idAtual)
                            .findFirst()
                            .ifPresentOrElse(s -> {
                                tabela.getSelectionModel().select(s);
                                mostrarHistorico(s);
                            }, this::fecharHistorico);
                }

            } catch (SQLException e) {
                AlertUtil.erro("Erro ao excluir movimentação: " + e.getMessage());
            }
        }
    }

    private String nvl(String s) { return (s == null || s.isBlank()) ? "—" : s; }
}
