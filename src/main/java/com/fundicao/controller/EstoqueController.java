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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

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
    // Guarda a ultima movimentacao selecionada — evita perder referencia
    // quando o botao externo rouba o foco da tabelaHistorico
    private Movimentacao movimentacaoSelecionada;

    @FXML
    public void initialize() {
        colProduto.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDescricao()));
        colSaldo.setCellValueFactory(c ->
                new SimpleStringProperty(String.format("%.3f kg", c.getValue().getSaldo())));
        colUltimaMov.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getUltimaMovimentacao())));

        tabela.setItems(dados);
        tabela.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tabelaHistorico.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        configurarCopiar(tabela);

        colHistTipo.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getTipo())));
        colHistQtd.setCellValueFactory(c ->
                new SimpleStringProperty(String.format("%.3f", c.getValue().getQuantidade())));
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

        // Salva referencia da movimentacao selecionada no listener
        // assim o botao externo nao perde a referencia ao ganhar foco
        tabelaHistorico.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) movimentacaoSelecionada = novo;
        });

        filtroTipo.setItems(FXCollections.observableArrayList("Todos", "Entrada", "Saída"));
        filtroTipo.setValue("Todos");
        filtroTipo.valueProperty().addListener((obs, a, novo) -> filtrar(campoBusca.getText()));

        carregarDados();

        campoBusca.textProperty().addListener((obs, a, novo) -> filtrar(novo));

        tabela.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            SaldoEstoque sel = tabela.getSelectionModel().getSelectedItem();
            if (sel != null) mostrarHistorico(sel);
        });

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
                .filter(s -> "Todos".equals(tipo) || tipo == null
                        || tipo.equalsIgnoreCase(nvl(s.getUltimoTipo())))
                .toList());
        labelTotal.setText("Total: " + dados.size() + " produtos");
    }

    private void mostrarHistorico(SaldoEstoque s) {
        saldoAtual = s;
        movimentacaoSelecionada = null;
        tabelaHistorico.getSelectionModel().clearSelection();
        labelNomeProduto.setText(s.getDescricao());
        labelSaldoDetalhe.setText(String.format("%.3f kg", s.getSaldo()));
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
        movimentacaoSelecionada = null;
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
    private void excluirMovimentacao() {
        // Usa movimentacaoSelecionada porque clicar no botao externo
        // faz a tabelaHistorico perder a selecao antes deste metodo rodar
        Movimentacao sel = movimentacaoSelecionada;
        if (sel == null) {
            AlertUtil.aviso("Selecione uma movimentação no histórico para excluir.");
            return;
        }
        if (AlertUtil.confirmar("Excluir movimentação de " +
                String.format("%.3f kg", sel.getQuantidade()) +
                " (" + nvl(sel.getTipo()) + ")?")) {
            try {
                estoqueService.excluir(sel.getId());
                movimentacaoSelecionada = null;
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
            } catch (SQLException e) {
                AlertUtil.erro("Erro ao excluir movimentação: " + e.getMessage());
            }
        }
    }

    private String nvl(String s) { return (s == null || s.isBlank()) ? "—" : s; }
}
