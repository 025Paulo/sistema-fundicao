package com.fundicao.controller;

import com.fundicao.model.Entidade;
import com.fundicao.service.EntidadeService;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
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

public class EntidadeController {

    @FXML private TextField campoBusca;
    @FXML private TableView<Entidade> tabela;
    @FXML private TableColumn<Entidade, String> colRazaoSocial;
    @FXML private TableColumn<Entidade, String> colTipo;
    @FXML private TableColumn<Entidade, String> colCnpjCpf;
    @FXML private TableColumn<Entidade, String> colTelefone;
    @FXML private TableColumn<Entidade, String> colSituacao;
    @FXML private Label labelTotal;

    @FXML private VBox painelDetalhes;
    @FXML private Label labelNomeDetalhe;
    @FXML private Label dTelefone;
    @FXML private Label dEmail;
    @FXML private Label dCnpj;
    @FXML private Label dIe;
    @FXML private Label dEndereco;
    @FXML private Label dCidade;
    @FXML private Label dCep;

    private final EntidadeService service = new EntidadeService();
    private final ObservableList<Entidade> dados = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colRazaoSocial.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getRazaoSocial()));
        colTipo.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getTipo()));
        colCnpjCpf.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getCnpjCpf()));
        colTelefone.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getTelefone()));
        colSituacao.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getSituacao()));

        tabela.setItems(dados);
        carregarDados();

        campoBusca.textProperty().addListener((obs, antigo, novo) -> filtrar(novo));

        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) mostrarDetalhes(novo);
            else fecharDetalhes();
        });
    }

    private void carregarDados() {
        try {
            List<Entidade> lista = service.listarTodos();
            dados.setAll(lista);
            labelTotal.setText("Total: " + lista.size() + " registros");
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar entidades: " + e.getMessage());
        }
    }

    private void filtrar(String termo) {
        try {
            List<Entidade> lista = (termo == null || termo.isBlank())
                    ? service.listarTodos()
                    : service.buscar(termo.trim());
            dados.setAll(lista);
            labelTotal.setText("Total: " + lista.size() + " registros");
        } catch (SQLException e) {
            mostrarErro("Erro ao filtrar entidades: " + e.getMessage());
        }
    }

    private void mostrarDetalhes(Entidade e) {
        labelNomeDetalhe.setText(e.getRazaoSocial()
                + "  ·  " + nvl(e.getTipo())
                + "  ·  " + nvl(e.getSituacao()));

        dTelefone.setText(nvl(e.getTelefone()));
        dEmail.setText(nvl(e.getEmail()));
        dCnpj.setText(nvl(e.getCnpjCpf()));
        dIe.setText(nvl(e.getInscricaoEstadual()));

        String end = java.util.Arrays.stream(new String[]{e.getRua(), e.getNumero(), e.getComplemento(), e.getBairro()})
                .filter(s -> s != null && !s.isBlank())
                .collect(java.util.stream.Collectors.joining(", "));
        dEndereco.setText(end.isBlank() ? "—" : end);

        String cidade = java.util.Arrays.stream(new String[]{e.getCidade(), e.getUf()})
                .filter(s -> s != null && !s.isBlank())
                .collect(java.util.stream.Collectors.joining(" – "));
        dCidade.setText(cidade.isBlank() ? "—" : cidade);
        dCep.setText(nvl(e.getCep()));

        if (!painelDetalhes.isVisible()) {
            painelDetalhes.setVisible(true);
            painelDetalhes.setManaged(true);
            painelDetalhes.setOpacity(0);
            painelDetalhes.setScaleY(0.92);

            Timeline tl = new Timeline(
                    new KeyFrame(Duration.millis(180),
                            new KeyValue(painelDetalhes.opacityProperty(), 1),
                            new KeyValue(painelDetalhes.scaleYProperty(), 1)
                    )
            );
            tl.play();
        }
    }

    @FXML
    private void fecharDetalhes() {
        Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(150),
                        new KeyValue(painelDetalhes.opacityProperty(), 0),
                        new KeyValue(painelDetalhes.scaleYProperty(), 0.92)
                )
        );
        tl.setOnFinished(e -> {
            painelDetalhes.setVisible(false);
            painelDetalhes.setManaged(false);
            tabela.getSelectionModel().clearSelection();
        });
        tl.play();
    }

    private String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    @FXML
    private void adicionar() {
        abrirDialog(null);
    }

    @FXML
    private void alterar() {
        Entidade selecionada = tabela.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            mostrarAviso("Selecione uma entidade para alterar.");
            return;
        }
        abrirDialog(selecionada);
    }

    @FXML
    private void excluir() {
        Entidade selecionada = tabela.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            mostrarAviso("Selecione uma entidade para excluir.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar exclusão");
        confirm.setHeaderText(null);
        confirm.setContentText("Excluir \"" + selecionada.getRazaoSocial() + "\"?");

        Optional<ButtonType> resultado = confirm.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            try {
                service.excluir(selecionada.getId());
                fecharDetalhes();
                carregarDados();
            } catch (SQLException e) {
                mostrarErro("Erro ao excluir entidade: " + e.getMessage());
            }
        }
    }

    private void abrirDialog(Entidade entidade) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/fundicao/view/entidade-dialog.fxml")
            );
            VBox dialogContent = loader.load();
            EntidadeDialogController dialogCtrl = loader.getController();
            dialogCtrl.setEntidade(entidade);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(entidade == null ? "Nova Entidade" : "Editar Entidade");
            dialog.getDialogPane().setContent(dialogContent);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                Entidade novo = dialogCtrl.getEntidade();
                if (entidade != null) {
                    novo.setId(entidade.getId());
                }
                service.salvar(novo);
                carregarDados();
            }
        } catch (IOException e) {
            mostrarErro("Erro ao abrir formulário: " + e.getMessage());
        } catch (SQLException e) {
            mostrarErro("Erro ao salvar entidade: " + e.getMessage());
        }
    }

    private void mostrarAviso(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void mostrarErro(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}