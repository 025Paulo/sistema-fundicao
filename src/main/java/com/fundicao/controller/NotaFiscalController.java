package com.fundicao.controller;

import com.fundicao.dao.NotaFiscalDAO;
import com.fundicao.model.NotaFiscal;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

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

    private final NotaFiscalDAO notaFiscalDAO = new NotaFiscalDAO();
    private List<NotaFiscal> todasNotas = new ArrayList<>();

    @FXML
    public void initialize() {
        configurarColunas();
        configurarFiltros();
        carregar();
    }

    private void configurarColunas() {
        colNumero.setCellValueFactory(c ->
                new SimpleStringProperty(valorOuTraco(c.getValue().getNumero())));

        colNatureza.setCellValueFactory(c ->
                new SimpleStringProperty(valorOuTraco(c.getValue().getNatureza())));

        colData.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getData() != null
                        ? c.getValue().getData().toString() : "—"));

        colEntidade.setCellValueFactory(c ->
                new SimpleStringProperty(valorOuTraco(c.getValue().getEntidadeNome())));

        colPesoLiquido.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getPesoLiquido() != null
                        ? String.format("%.2f kg", c.getValue().getPesoLiquido()) : "—"));

        colTransportadora.setCellValueFactory(c ->
                new SimpleStringProperty(valorOuTraco(c.getValue().getTransportadora())));
    }

    private void configurarFiltros() {
        filtroNatureza.getItems().addAll("Todas", "Entrada", "Saida");
        filtroNatureza.setValue("Todas");
        filtroNatureza.setOnAction(e -> filtrar());

        campoBusca.textProperty().addListener((obs, old, novo) -> filtrar());
    }

    private void carregar() {
        try {
            todasNotas = notaFiscalDAO.listarTodas();
            System.out.println("Notas carregadas: " + todasNotas.size()); // ← adiciona isso
            filtrar();
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar notas fiscais: " + e.getMessage());
            e.printStackTrace();
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

    @FXML
    private void novaNota() {
        abrirDialog(null);
    }

    @FXML
    private void alterarNota() {
        NotaFiscal selecionada = tabela.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            new Alert(Alert.AlertType.WARNING, "Selecione uma nota para alterar.", ButtonType.OK).showAndWait();
            return;
        }
        abrirDialog(selecionada);
    }

    @FXML
    private void excluirNota() {
        NotaFiscal selecionada = tabela.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            new Alert(Alert.AlertType.WARNING, "Selecione uma nota para excluir.", ButtonType.OK).showAndWait();
            return;
        }

        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Excluir esta nota fiscal?",
                ButtonType.YES, ButtonType.NO
        );

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    notaFiscalDAO.excluir(selecionada.getId());
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
            if (nota != null) {
                controller.setNotaFiscal(nota);
            }

            stage.showAndWait();

            if (controller.isSalvo()) {
                carregar();
            }

        } catch (IOException e) {
            mostrarErro("Erro ao abrir janela: " + e.getMessage());
        }
    }

    private boolean contem(String texto, String busca) {
        return texto != null && texto.toLowerCase().contains(busca);
    }

    private String valorOuTraco(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }

    private void mostrarErro(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}