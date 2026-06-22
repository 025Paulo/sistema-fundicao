package com.fundicao.controller;

import com.fundicao.dao.EntidadeDAO;
import com.fundicao.dao.EstoqueDAO;
import com.fundicao.dao.ProdutoDAO;
import com.fundicao.model.Entidade;
import com.fundicao.model.Movimentacao;
import com.fundicao.model.Produto;
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

    private final EstoqueDAO estoqueDAO = new EstoqueDAO();
    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final EntidadeDAO entidadeDAO = new EntidadeDAO();

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

    private void carregarCombos() {
        try {
            todosProdutos = produtoDAO.listarTodos();
            comboProduto.setItems(FXCollections.observableArrayList(todosProdutos));
            comboProduto.setConverter(new javafx.util.StringConverter<>() {
                public String toString(Produto p) { return p == null ? "" : p.getDescricao(); }
                public Produto fromString(String s) { return null; }
            });

            todasEntidades = entidadeDAO.listarTodos();
            comboEntidade.setItems(FXCollections.observableArrayList(todasEntidades));
            comboEntidade.setConverter(new javafx.util.StringConverter<>() {
                public String toString(Entidade e) { return e == null ? "" : e.getRazaoSocial(); }
                public Entidade fromString(String s) { return null; }
            });
        } catch (RuntimeException e) {
            mostrarErro("Erro ao carregar dados: " + e.getMessage());
        }
    }

    // Filtro por digitação no combo de Produto
    private void configurarFiltroProduto() {
        comboProduto.setEditable(true);
        comboProduto.getEditor().textProperty().addListener((obs, old, novo) -> {
            // Ignora se a mudança veio de uma seleção (não de digitação manual)
            Produto selecionado = comboProduto.getValue();
            if (selecionado != null && selecionado.getDescricao().equals(novo)) return;

            String lower = novo == null ? "" : novo.toLowerCase();
            ObservableList<Produto> filtrado = FXCollections.observableArrayList(
                    todosProdutos.stream()
                            .filter(p -> p.getDescricao().toLowerCase().contains(lower))
                            .toList()
            );
            comboProduto.setItems(filtrado);
            comboProduto.show(); // abre o dropdown com os resultados
        });

        // Ao perder foco, se o texto não bater com nenhum produto, limpa
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

    // Filtro por digitação no combo de Entidade
    private void configurarFiltroEntidade() {
        comboEntidade.setEditable(true);
        comboEntidade.getEditor().textProperty().addListener((obs, old, novo) -> {
            Entidade selecionada = comboEntidade.getValue();
            if (selecionada != null && selecionada.getRazaoSocial().equals(novo)) return;

            String lower = novo == null ? "" : novo.toLowerCase();
            ObservableList<Entidade> filtrado = FXCollections.observableArrayList(
                    todasEntidades.stream()
                            .filter(e -> e.getRazaoSocial().toLowerCase().contains(lower))
                            .toList()
            );
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
        if (!validar()) return;

        try {
            Movimentacao m = new Movimentacao();
            m.setProdutoId(comboProduto.getValue().getId());
            m.setEntidadeId(comboEntidade.getValue().getId());
            m.setTipo(tipo);
            m.setQuantidade(Double.parseDouble(
                    campoQuantidade.getText().trim().replace(",", ".")));
            m.setDataMovimentacao(campoData.getValue());

            String vrUnit = campoValorUnitario.getText().trim().replace(",", ".");
            if (!vrUnit.isEmpty()) m.setValorUnitario(Double.parseDouble(vrUnit));

            m.setOrdemCompra(campoOrdemCompra.getText().trim());
            m.setObservacoes(campoObservacoes.getText().trim());

            if ("Saida".equals(tipo)) {
                double saldo = estoqueDAO.getSaldo(m.getProdutoId());
                if (m.getQuantidade() > saldo) {
                    mostrarErro(String.format("Saldo insuficiente! Disponível: %.2f kg", saldo));
                    return;
                }
            }

            estoqueDAO.registrar(m);
            salvo = true;
            fecharJanela();

        } catch (NumberFormatException e) {
            mostrarErro("Quantidade inválida. Use números (ex: 10,5)");
        } catch (SQLException e) {
            mostrarErro("Erro ao salvar: " + e.getMessage());
        }
    }

    private boolean validar() {
        if (comboProduto.getValue() == null) {
            mostrarErro("Selecione um produto da lista.");
            return false;
        }
        if (comboEntidade.getValue() == null) {
            mostrarErro("Selecione um fornecedor/cliente da lista.");
            return false;
        }
        String qtd = campoQuantidade.getText().trim();
        if (qtd.isEmpty()) {
            mostrarErro("Informe a quantidade.");
            return false;
        }
        try {
            double v = Double.parseDouble(qtd.replace(",", "."));
            if (v <= 0) { mostrarErro("Quantidade deve ser maior que zero."); return false; }
        } catch (NumberFormatException e) {
            mostrarErro("Quantidade inválida.");
            return false;
        }
        if (campoData.getValue() == null) {
            mostrarErro("Informe a data.");
            return false;
        }
        return true;
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