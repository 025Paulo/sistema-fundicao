package com.fundicao.controller;

import com.fundicao.model.NotaFiscal;
import com.fundicao.service.NotaFiscalService;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NotaFiscalController {

    @FXML private TextField campoBusca;
    @FXML private ComboBox<String> filtroNatureza;
    @FXML private TableView<NotaFiscal> tabela;
    @FXML private TableColumn<NotaFiscal, String> colNumero;
    @FXML private TableColumn<NotaFiscal, String> colNatureza;
    @FXML private TableColumn<NotaFiscal, String> colData;
    @FXML private TableColumn<NotaFiscal, String> colEntidade;
    @FXML private TableColumn<NotaFiscal, String> colPesoLiquido;
    @FXML private TableColumn<NotaFiscal, String> colTransportadora;
    @FXML private Label labelTotal;

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
    private List<NotaFiscal> todasNotas = new ArrayList<>();

    @FXML
    public void initialize() {
        configurarColunas();
        configurarFiltros();
        configurarSelecao();
        carregar();
    }

    private void configurarColunas() {
        colNumero.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getNumero())));
        colNatureza.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getNatureza())));
        colData.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getData() != null
                        ? c.getValue().getData().toString() : "—"));
        colEntidade.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getEntidadeNome())));
        colPesoLiquido.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getPesoLiquido() != null
                        ? String.format("%.2f kg", c.getValue().getPesoLiquido()) : "—"));
        colTransportadora.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getTransportadora())));
    }

    private void configurarFiltros() {
        filtroNatureza.getItems().addAll("Todas", "Entrada", "Saida");
        filtroNatureza.setValue("Todas");
        filtroNatureza.setOnAction(e -> filtrar());
        campoBusca.textProperty().addListener((obs, old, novo) -> filtrar());
    }

    private void configurarSelecao() {
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) mostrarDetalhes(novo);
            else fecharDetalhes();
        });
    }

    private void carregar() {
        try {
            todasNotas = service.listarTodas();
            filtrar();
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar notas fiscais: " + e.getMessage());
        }
    }

    private void filtrar() {
        String busca = campoBusca.getText() == null ? "" : campoBusca.getText().trim().toLowerCase();
        String natureza = filtroNatureza.getValue() == null ? "Todas" : filtroNatureza.getValue();

        List<NotaFiscal> filtradas = todasNotas.stream()
                .filter(n -> {
                    boolean matchBusca = busca.isBlank()
                            || contem(n.getNumero(), busca)
                            || contem(n.getEntidadeNome(), busca);
                    boolean matchNatureza = "Todas".equals(natureza)
                            || (n.getNatureza() != null && n.getNatureza().equalsIgnoreCase(natureza));
                    return matchBusca && matchNatureza;
                })
                .toList();

        tabela.setItems(FXCollections.observableArrayList(filtradas));
        labelTotal.setText("Total: " + filtradas.size() + " nota(s)");
    }

    private void mostrarDetalhes(NotaFiscal nf) {
        labelTituloDetalhe.setText("NF " + nvl(nf.getNumero()) + "  ·  " + nvl(nf.getNatureza()));
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
                    new KeyValue(painelDetalhes.scaleYProperty(), 1)
            )).play();
        }
    }

    @FXML
    private void fecharDetalhes() {
        if (!painelDetalhes.isVisible()) return;
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
    private void novaNota() { abrirDialog(null); }

    @FXML
    private void alterarNota() {
        NotaFiscal s = tabela.getSelectionModel().getSelectedItem();
        if (s == null) { mostrarAviso("Selecione uma nota para alterar."); return; }
        abrirDialog(s);
    }

    @FXML
    private void excluirNota() {
        NotaFiscal s = tabela.getSelectionModel().getSelectedItem();
        if (s == null) { mostrarAviso("Selecione uma nota para excluir."); return; }

        new Alert(Alert.AlertType.CONFIRMATION,
                "Excluir a nota \"" + s.getNumero() + "\"?",
                ButtonType.YES, ButtonType.NO)
                .showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) {
                        try {
                            service.excluir(s.getId());
                            fecharDetalhes();
                            carregar();
                        } catch (SQLException e) {
                            mostrarErro("Erro ao excluir nota: " + e.getMessage());
                        }
                    }
                });
    }

    private void abrirDialog(NotaFiscal nota) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/fundicao/view/nota-fiscal-dialog.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(nota == null ? "Nova Nota Fiscal" : "Alterar Nota Fiscal");
            stage.setScene(new Scene(loader.load()));

            NotaFiscalDialogController controller = loader.getController();
            if (nota != null) controller.setNotaFiscal(nota);

            stage.showAndWait();
            if (controller.isSalvo()) carregar();
        } catch (IOException e) {
            mostrarErro("Erro ao abrir janela: " + e.getMessage());
        }
    }

    private boolean contem(String texto, String busca) {
        return texto != null && texto.toLowerCase().contains(busca);
    }

    private String nvl(String s) { return (s == null || s.isBlank()) ? "—" : s; }

    private void mostrarErro(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private void mostrarAviso(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }
}