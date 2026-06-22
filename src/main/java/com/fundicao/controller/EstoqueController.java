package com.fundicao.controller;

import com.fundicao.dao.EstoqueDAO;
import com.fundicao.dao.ProdutoDAO;
import com.fundicao.model.Movimentacao;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class EstoqueController {

    @FXML private TextField campoBusca;
    @FXML private ComboBox<String> filtroTipo;
    @FXML private TableView<Object[]> tabela;
    @FXML private TableColumn<Object[], String> colProduto;
    @FXML private TableColumn<Object[], String> colSaldo;
    @FXML private TableColumn<Object[], String> colUltimaMov;

    @FXML private VBox painelHistorico;
    @FXML private Label labelNomeProduto;
    @FXML private Label labelSaldoDetalhe;
    @FXML private TableView<Movimentacao> tabelaHistorico;
    @FXML private TableColumn<Movimentacao, String> colHistTipo;
    @FXML private TableColumn<Movimentacao, String> colHistQtd;
    @FXML private TableColumn<Movimentacao, String> colHistEntidade;
    @FXML private TableColumn<Movimentacao, String> colHistData;
    @FXML private TableColumn<Movimentacao, String> colHistObs;
    @FXML private TableColumn<Movimentacao, String> colHistValor;
    @FXML private TableColumn<Movimentacao, String> colHistOrdem;

    @FXML private Label labelTotal;

    private final EstoqueDAO estoqueDAO = new EstoqueDAO();
    private List<Object[]> todosSaldos;

    @FXML
    public void initialize() {
        configurarColunas();
        configurarFiltroTipo();
        configurarBusca();
        configurarSelecao();
        carregar();
    }

    private void configurarColunas() {
        colProduto.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue()[1]));
        colSaldo.setCellValueFactory(c -> {
            double saldo = (double) c.getValue()[2];
            return new SimpleStringProperty(String.format("%.2f kg", saldo));
        });
        colUltimaMov.setCellValueFactory(c -> {
            String data = (String) c.getValue()[3];
            return new SimpleStringProperty(data != null ? data : "—");
        });

        // Coluna Tipo como primeira coluna (verde/vermelho)
        TableColumn<Object[], String> colTipo = new TableColumn<>("Tipo");
        colTipo.setPrefWidth(80);
        colTipo.setCellValueFactory(c -> {
            String tipo = (String) c.getValue()[4];
            return new SimpleStringProperty(tipo != null ? tipo : "");
        });
        colTipo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("Entrada".equals(item)
                        ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;"
                        : "-fx-text-fill: #c62828; -fx-font-weight: bold;");
            }
        });
        colHistValor.setCellValueFactory(c -> {
            Double vr = c.getValue().getValorUnitario();
            return new SimpleStringProperty(vr != null ? String.format("R$ %.2f", vr) : "—");
        });

        colHistOrdem.setCellValueFactory(c -> {
            String ordem = c.getValue().getOrdemCompra();
            return new SimpleStringProperty(ordem != null && !ordem.isBlank() ? ordem : "—");
        });
        tabela.getColumns().add(0, colTipo);

        // Histórico
        colHistTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTipo()));
        colHistQtd.setCellValueFactory(c ->
                new SimpleStringProperty(String.format("%.2f kg", c.getValue().getQuantidade())));
        colHistEntidade.setCellValueFactory(c -> {
            String nome = c.getValue().getEntidadeNome();
            return new SimpleStringProperty(nome != null ? nome : "—");
        });
        colHistData.setCellValueFactory(c -> {
            var data = c.getValue().getDataMovimentacao();
            return new SimpleStringProperty(data != null ? data.toString() : "—");
        });
        colHistObs.setCellValueFactory(c -> {
            String obs = c.getValue().getObservacoes();
            return new SimpleStringProperty(obs != null ? obs : "");
        });
    }

    private void configurarFiltroTipo() {
        filtroTipo.getItems().addAll("Todos", "Entrada", "Saida");
        filtroTipo.setValue("Todos");
        filtroTipo.setOnAction(e -> filtrar(campoBusca.getText()));
    }

    private void configurarBusca() {
        campoBusca.textProperty().addListener((obs, old, novo) -> filtrar(novo));
    }

    private void configurarSelecao() {
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, old, novo) -> {
            if (novo != null) mostrarHistorico(novo);
        });
    }

    private void carregar() {
        try {
            todosSaldos = estoqueDAO.getSaldoTodos();
            filtrar(campoBusca.getText());
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar estoque: " + e.getMessage());
        }
    }

    private void filtrar(String texto) {
        String lower = texto == null ? "" : texto.toLowerCase();
        String tipo = filtroTipo != null ? filtroTipo.getValue() : "Todos";

        List<Object[]> filtrado = todosSaldos.stream()
                .filter(row -> {
                    boolean matchTexto = ((String) row[1]).toLowerCase().contains(lower);
                    boolean matchTipo = "Todos".equals(tipo)
                            || tipo.equals(row[4]);
                    return matchTexto && matchTipo;
                })
                .toList();

        tabela.setItems(FXCollections.observableArrayList(filtrado));
        atualizarRodape(filtrado);
    }

    private void atualizarRodape(List<Object[]> dados) {
        double totalKg = dados.stream()
                .mapToDouble(row -> (double) row[2])
                .sum();
        labelTotal.setText(String.format(
                "Total: %d produto(s)   |   Saldo total: %.2f kg", dados.size(), totalKg));
    }

    private void mostrarHistorico(Object[] row) {
        try {
            int produtoId = (int) row[0];
            String nome = (String) row[1];
            double saldo = (double) row[2];

            labelNomeProduto.setText(nome);
            labelSaldoDetalhe.setText(String.format("%.2f kg", saldo));

            List<Movimentacao> historico = estoqueDAO.getHistorico(produtoId);
            tabelaHistorico.setItems(FXCollections.observableArrayList(historico));

            painelHistorico.setVisible(true);
            painelHistorico.setManaged(true);
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar histórico: " + e.getMessage());
        }
    }

    @FXML
    private void fecharHistorico() {
        painelHistorico.setVisible(false);
        painelHistorico.setManaged(false);
        tabela.getSelectionModel().clearSelection();
    }

    @FXML
    private void novaEntrada() { abrirDialog("Entrada"); }

    @FXML
    private void novaSaida() { abrirDialog("Saida"); }

    private void abrirDialog(String tipo) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/fundicao/view/movimentacao-dialog.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Entrada".equals(tipo) ? "Nova Entrada" : "Nova Saída");
            stage.setScene(new Scene(loader.load()));

            MovimentacaoDialogController ctrl = loader.getController();
            ctrl.setTipo(tipo);

            Object[] selecionado = tabela.getSelectionModel().getSelectedItem();
            if (selecionado != null) {
                var produto = new ProdutoDAO().buscarPorId((int) selecionado[0]);
                if (produto != null) ctrl.setProduto(produto);
            }

            stage.showAndWait();
            if (ctrl.isSalvo()) carregar();

        } catch (IOException e) {
            mostrarErro("Erro ao abrir janela: " + e.getMessage());
        }
    }

    @FXML
    private void excluirMovimentacao() {
        Movimentacao selecionada = tabelaHistorico.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Selecione uma movimentação no histórico para excluir.", ButtonType.OK)
                    .showAndWait();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Excluir esta movimentação? O saldo será recalculado.", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    estoqueDAO.excluir(selecionada.getId());
                    carregar();
                    Object[] row = tabela.getSelectionModel().getSelectedItem();
                    if (row != null) mostrarHistorico(row);
                } catch (SQLException e) {
                    mostrarErro("Erro ao excluir: " + e.getMessage());
                }
            }
        });
    }

    private void mostrarErro(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}