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
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class NotaFiscalController {

    @FXML private TextField campoBusca;
    @FXML private TableView<NotaFiscal> tabela;
    @FXML private TableColumn<NotaFiscal, String> colNumero;
    @FXML private TableColumn<NotaFiscal, String> colEmitente;
    @FXML private TableColumn<NotaFiscal, String> colValor;
    @FXML private TableColumn<NotaFiscal, String> colDataEmissao;
    @FXML private Label labelTotal;

    @FXML private VBox painelDetalhes;
    @FXML private Label labelNumeroDetalhe;
    @FXML private Label dEmitente;
    @FXML private Label dDestinatario;
    @FXML private Label dValor;
    @FXML private Label dDataEmissao;
    @FXML private Label dChave;

    private final NotaFiscalService service = new NotaFiscalService();
    private final ObservableList<NotaFiscal> dados = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colNumero.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getNumero())));
        colEmitente.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getEmitenteNome())));
        colValor.setCellValueFactory(c ->
                new SimpleStringProperty(fmtM(c.getValue().getValorTotal())));
        colDataEmissao.setCellValueFactory(c ->
                new SimpleStringProperty(nvl(c.getValue().getDataEmissao())));

        tabela.setItems(dados);
        carregarDados();

        campoBusca.textProperty().addListener((obs, a, novo) -> filtrar(novo));

        tabela.getSelectionModel().selectedItemProperty().addListener((obs, a, novo) -> {
            if (novo != null) mostrarDetalhes(novo);
            else fecharDetalhes();
        });
    }

    private void carregarDados() {
        try {
            List<NotaFiscal> lista = service.listarTodos();
            dados.setAll(lista);
            labelTotal.setText("Total: " + lista.size() + " notas");
        } catch (SQLException e) {
            AlertUtil.erro("Erro ao carregar notas fiscais: " + e.getMessage());
        }
    }

    private void filtrar(String termo) {
        try {
            List<NotaFiscal> lista = (termo == null || termo.isBlank())
                    ? service.listarTodos()
                    : service.buscar(termo.trim());
            dados.setAll(lista);
            labelTotal.setText("Total: " + lista.size() + " notas");
        } catch (SQLException e) {
            AlertUtil.erro("Erro ao filtrar notas fiscais: " + e.getMessage());
        }
    }

    private void mostrarDetalhes(NotaFiscal nf) {
        labelNumeroDetalhe.setText("NF " + nvl(nf.getNumero()));
        dEmitente.setText(nvl(nf.getEmitenteNome()));
        dDestinatario.setText(nvl(nf.getDestinatarioNome()));
        dValor.setText(fmtM(nf.getValorTotal()));
        dDataEmissao.setText(nvl(nf.getDataEmissao()));
        dChave.setText(nvl(nf.getChaveAcesso()));

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
        NotaFiscal sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtil.aviso("Selecione uma nota fiscal para alterar.");
            return;
        }
        abrirDialog(sel);
    }

    @FXML
    private void excluir() {
        NotaFiscal sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtil.aviso("Selecione uma nota fiscal para excluir.");
            return;
        }

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

    private void abrirDialog(NotaFiscal nf) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/fundicao/view/notafiscal-dialog.fxml")
            );
            VBox content = loader.load();
            NotaFiscalDialogController ctrl = loader.getController();
            ctrl.setNotaFiscal(nf);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(nf == null ? "Nova Nota Fiscal" : "Editar Nota Fiscal");
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    try {
                        service.salvar(ctrl.getNotaFiscal());
                        carregarDados();
                    } catch (SQLException e) {
                        AlertUtil.erro("Erro ao salvar nota fiscal: " + e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            AlertUtil.erro("Erro ao abrir formulário: " + e.getMessage());
        }
    }

    private String nvl(String s) { return (s == null || s.isBlank()) ? "—" : s; }
    private String fmtM(Double v) { return v != null ? String.format("R$ %.2f", v) : "—"; }
}