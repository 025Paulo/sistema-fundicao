package com.fundicao.controller;

import com.fundicao.model.NotaFiscal;
import com.fundicao.service.NotaFiscalService;
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
import java.util.List;

public class NotaFiscalController {

    // ── Barra de filtros ─────────────────────────────────────────────
    @FXML private TextField campoBusca;
    @FXML private ComboBox<String> filtroNatureza;

    // ── Tabela principal ─────────────────────────────────────────────
    @FXML private TableView<NotaFiscal> tabela;
    @FXML private TableColumn<NotaFiscal, String> colNumero;
    @FXML private TableColumn<NotaFiscal, String> colNatureza;
    @FXML private TableColumn<NotaFiscal, String> colData;
    @FXML private TableColumn<NotaFiscal, String> colEntidade;
    @FXML private TableColumn<NotaFiscal, String> colPesoLiquido;
    @FXML private TableColumn<NotaFiscal, String> colTransportadora;
    @FXML private Label labelTotal;

    // ── Painel de detalhes ───────────────────────────────────────────
    @FXML private VBox painelDetalhes;
    @FXML private Label labelTituloDetalhe;
    @FXML private Label dData;
    @FXML private Label dOrdemCompra;
    @FXML private Label dTransportadora;
    @FXML private Label dTransporteRs;
    @FXML private Label dPesoBruto;
    @FXML private Label dPesoLiquido;
    @FXML private Label dDescontoRs;
    @FXML private Label dEntidade;

    private final NotaFiscalService service = new NotaFiscalService();
    private final ObservableList<NotaFiscal> dados = FXCollections.observableArrayList();
    private List<NotaFiscal> todasAsNotas;

    @FXML
    public void initialize() {
        // Colunas
        colNumero.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getNumero())));
        colNatureza.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getNatureza())));
        colData.setCellValueFactory(c -> {
            NotaFiscal nf = c.getValue();
            return new SimpleStringProperty(nf.getData() != null ? nf.getData().toString() : "—");
        });
        colEntidade.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getEntidadeNome())));
        colPesoLiquido.setCellValueFactory(c -> {
            Double v = c.getValue().getPesoLiquido();
            return new SimpleStringProperty(v != null ? String.format("%.2f kg", v) : "—");
        });
        colTransportadora.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getTransportadora())));

        tabela.setItems(dados);
        tabela.getSelectionModel().setCellSelectionEnabled(true);
        configurarCopiar(tabela);

        // Filtro de natureza
        filtroNatureza.setItems(FXCollections.observableArrayList(
                "Todos", "Entrada", "Saída", "Transferência"));
        filtroNatureza.setValue("Todos");
        filtroNatureza.valueProperty().addListener((obs, a, novo) -> filtrar(campoBusca.getText()));

        carregarDados();

        campoBusca.textProperty().addListener((obs, a, novo) -> filtrar(novo));

        tabela.getSelectionModel().selectedItemProperty().addListener((obs, a, novo) -> {
            if (novo != null) mostrarDetalhes(novo);
            else fecharDetalhes();
        });
    }

    private <T> void configurarCopiar(TableView<T> tv) {
        tv.setOnKeyPressed(event -> {
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
            todasAsNotas = service.listarTodas();
            dados.setAll(todasAsNotas);
            labelTotal.setText("Total: " + todasAsNotas.size() + " nota(s)");
        } catch (SQLException e) {
            AlertUtil.erro("Erro ao carregar notas fiscais: " + e.getMessage());
        }
    }

    private void filtrar(String termo) {
        if (todasAsNotas == null) return;
        String lower = (termo == null) ? "" : termo.toLowerCase();
        String nat = filtroNatureza.getValue();

        dados.setAll(todasAsNotas.stream()
                .filter(nf -> lower.isBlank()
                        || nvl(nf.getNumero()).toLowerCase().contains(lower)
                        || nvl(nf.getEntidadeNome()).toLowerCase().contains(lower))
                .filter(nf -> "Todos".equals(nat) || nat == null
                        || nat.equalsIgnoreCase(nvl(nf.getNatureza())))
                .toList());
        labelTotal.setText("Total: " + dados.size() + " nota(s)");
    }

    private void mostrarDetalhes(NotaFiscal nf) {
        labelTituloDetalhe.setText("NF " + nvl(nf.getNumero()) + "  —  " + nvl(nf.getNatureza()));
        dData.setText(nf.getData() != null ? nf.getData().toString() : "—");
        dOrdemCompra.setText(nvl(nf.getOrdemCompra()));
        dTransportadora.setText(nvl(nf.getTransportadora()));
        dTransporteRs.setText(nf.getTransporteRs() != null
                ? String.format("R$ %.2f", nf.getTransporteRs()) : "—");
        dPesoBruto.setText(nf.getPesoBruto() != null
                ? String.format("%.2f kg", nf.getPesoBruto()) : "—");
        dPesoLiquido.setText(nf.getPesoLiquido() != null
                ? String.format("%.2f kg", nf.getPesoLiquido()) : "—");
        dDescontoRs.setText(nf.getDescontoRs() != null
                ? String.format("R$ %.2f", nf.getDescontoRs()) : "—");
        dEntidade.setText(nvl(nf.getEntidadeNome()));

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

    // ── Ações ────────────────────────────────────────────────────────
    @FXML private void adicionar()   { abrirDialog(null); }
    @FXML private void novaNota()    { abrirDialog(null); }

    @FXML
    private void alterar() {
        NotaFiscal sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) { AlertUtil.aviso("Selecione uma nota fiscal para alterar."); return; }
        abrirDialog(sel);
    }
    @FXML private void alterarNota() { alterar(); }

    @FXML
    private void excluir() {
        NotaFiscal sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) { AlertUtil.aviso("Selecione uma nota fiscal para excluir."); return; }
        if (AlertUtil.confirmar("Excluir NF \"" + nvl(sel.getNumero()) + "\"?")) {
            try {
                service.excluir(sel.getId());
                fecharDetalhes();
                carregarDados();
            } catch (SQLException e) {
                AlertUtil.erro("Erro ao excluir nota fiscal: " + e.getMessage());
            }
        }
    }
    @FXML private void excluirNota() { excluir(); }

    // ── Dialog ───────────────────────────────────────────────────────
    private void abrirDialog(NotaFiscal nf) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/fundicao/view/notafiscal-dialog.fxml"));
            Parent root = loader.load();
            NotaFiscalDialogController ctrl = loader.getController();
            if (nf != null) ctrl.setNotaFiscal(nf);

            Stage stage = new Stage();
            stage.setTitle(nf == null ? "Nova Nota Fiscal" : "Editar Nota Fiscal");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            if (ctrl.isSalvo()) carregarDados();
        } catch (IOException e) {
            AlertUtil.erro("Erro ao abrir formulário: " + e.getMessage());
        }
    }

    private String nvl(String s) { return (s == null || s.isBlank()) ? "—" : s; }
}