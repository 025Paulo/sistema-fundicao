package com.fundicao.controller;

import com.fundicao.model.Entidade;
import com.fundicao.model.Movimentacao;
import com.fundicao.model.Produto;
import com.fundicao.service.EntidadeService;
import com.fundicao.service.EstoqueService;
import com.fundicao.service.ProdutoService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class MovimentacaoDialogController {

    @FXML private Label labelTitulo;
    @FXML private ComboBox<Produto> comboProduto;
    @FXML private ComboBox<Entidade> comboEntidade;
    @FXML private TextField campoQuantidade;
    @FXML private DatePicker campoData;
    @FXML private TextField campoValorUnitario;
    @FXML private TextField campoOrdemCompra;
    @FXML private TextArea campoObservacoes;
    @FXML private Button btnSalvar;

    private String tipo;
    private boolean salvo = false;
    private List<Produto> todosProdutos;
    private List<Entidade> todasEntidades;

    private final EstoqueService estoqueService = new EstoqueService();
    private final ProdutoService produtoService = new ProdutoService();
    private final EntidadeService entidadeService = new EntidadeService();

    @FXML
    public void initialize() {
        campoData.setValue(LocalDate.now());
        carregarCombos();
        configurarFiltroProduto();
        configurarFiltroEntidade();
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
        if ("Entrada".equals(tipo)) {
            labelTitulo.setText("Nova Entrada");
            btnSalvar.setText("Salvar Entrada");
        } else {
            labelTitulo.setText("Nova Saída");
            btnSalvar.setText("Salvar Saída");
        }
    }

    public void setProduto(Produto produto) {
        comboProduto.setValue(produto);
    }

    public void setProdutoById(int produtoId) {
        if (todosProdutos == null) return;
        todosProdutos.stream()
                .filter(p -> p.getId() == produtoId)
                .findFirst()
                .ifPresent(comboProduto::setValue);
    }

    private void carregarCombos() {
        try {
            todosProdutos = produtoService.listarTodos();
            comboProduto.setItems(FXCollections.observableArrayList(todosProdutos));
            comboProduto.setConverter(new javafx.util.StringConverter<>() {
                public String toString(Produto p) { return p == null ? "" : p.getDescricao(); }
                public Produto fromString(String s) { return null; }
            });

            todasEntidades = entidadeService.listarTodos();
            comboEntidade.setItems(FXCollections.observableArrayList(todasEntidades));
            comboEntidade.setConverter(new javafx.util.StringConverter<>() {
                public String toString(Entidade e) { return e == null ? "" : e.getRazaoSocial(); }
                public Entidade fromString(String s) { return null; }
            });
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar dados: " + e.getMessage());
        }
    }

    private void configurarFiltroProduto() {
        comboProduto.setEditable(true);
        comboProduto.getEditor().textProperty().addListener((obs, old, novo) -> {
            Produto selecionado = comboProduto.getValue();
            if (selecionado != null && selecionado.getDescricao().equals(novo)) return;
            String lower = novo == null ? "" : novo.toLowerCase();
            ObservableList<Produto> filtrado = FXCollections.observableArrayList(
                    todosProdutos.stream()
                            .filter(p -> p.getDescricao().toLowerCase().contains(lower))
                            .toList());
            comboProduto.setItems(filtrado);
            comboProduto.show();
        });
        comboProduto.getEditor().focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                String texto = comboProduto.getEditor().getText();
                boolean valido = todosProdutos.stream()
                        .anyMatch(p -> p.getDescricao().equalsIgnoreCase(texto));
                if (!valido) {
                    comboProduto.setValue(null);
                    comboProduto.getEditor().clear();
                    comboProduto.setItems(FXCollections.observableArrayList(todosProdutos));
                }
            }
        });
    }

    private void configurarFiltroEntidade() {
        comboEntidade.setEditable(true);
        comboEntidade.getEditor().textProperty().addListener((obs, old, novo) -> {
            Entidade selecionada = comboEntidade.getValue();
            if (selecionada != null && selecionada.getRazaoSocial().equals(novo)) return;
            String lower = novo == null ? "" : novo.toLowerCase();
            ObservableList<Entidade> filtrado = FXCollections.observableArrayList(
                    todasEntidades.stream()
                            .filter(e -> e.getRazaoSocial().toLowerCase().contains(lower))
                            .toList());
            comboEntidade.setItems(filtrado);
            comboEntidade.show();
        });
        comboEntidade.getEditor().focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                String texto = comboEntidade.getEditor().getText();
                boolean valido = todasEntidades.stream()
                        .anyMatch(e -> e.getRazaoSocial().equalsIgnoreCase(texto));
                if (!valido) {
                    comboEntidade.setValue(null);
                    comboEntidade.getEditor().clear();
                    comboEntidade.setItems(FXCollections.observableArrayList(todasEntidades));
                }
            }
        });
    }

    @FXML
    private void salvar() {
        try {
            Movimentacao m = new Movimentacao();
            m.setProdutoId(comboProduto.getValue() == null ? 0 : comboProduto.getValue().getId());
            m.setEntidadeId(comboEntidade.getValue() == null ? null : comboEntidade.getValue().getId());
            m.setTipo(tipo);
            m.setQuantidade(Double.parseDouble(
                    campoQuantidade.getText().trim().replace(",", ".")));
            m.setDataMovimentacao(campoData.getValue());

            String vrUnit = campoValorUnitario.getText().trim().replace(",", ".");
            if (!vrUnit.isEmpty()) m.setValorUnitario(Double.parseDouble(vrUnit));

            m.setOrdemCompra(campoOrdemCompra.getText().trim());
            m.setObservacoes(campoObservacoes.getText().trim());

            // validações de negócio + saldo ficam no service
            estoqueService.registrar(m);
            salvo = true;
            fecharJanela();

        } catch (NumberFormatException e) {
            mostrarErro("Quantidade inválida. Use números (ex: 10,5)");
        } catch (IllegalArgumentException e) {
            mostrarErro(e.getMessage());
        } catch (SQLException e) {
            mostrarErro("Erro ao salvar: " + e.getMessage());
        }
    }

    @FXML
    private void cancelar() { fecharJanela(); }

    public boolean isSalvo() { return salvo; }

    private void fecharJanela() {
        ((Stage) btnSalvar.getScene().getWindow()).close();
    }

    private void mostrarErro(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}