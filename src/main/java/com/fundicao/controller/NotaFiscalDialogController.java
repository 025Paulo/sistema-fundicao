package com.fundicao.controller;

import com.fundicao.model.Entidade;
import com.fundicao.model.NotaFiscal;
import com.fundicao.model.NotaProduto;
import com.fundicao.model.Produto;
import com.fundicao.service.EntidadeService;
import com.fundicao.service.NotaFiscalService;
import com.fundicao.service.ProdutoService;
import com.fundicao.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class NotaFiscalDialogController {

    // ── Campos da nota ──────────────────────────────────────────────────────
    @FXML private Label              labelTitulo;
    @FXML private ComboBox<String>   comboNatureza;
    @FXML private TextField          campoNumero;
    @FXML private DatePicker         campoData;
    @FXML private ComboBox<Entidade> comboEntidade;
    @FXML private TextField          campoOrdemCompra;
    @FXML private TextField          campoTransportadora;
    @FXML private TextField          campoTransporteRs;
    @FXML private TextField          campoDescontoRs;
    @FXML private TextField          campoPesoBruto;
    @FXML private TextField          campoPesoLiquido;
    @FXML private Button             btnSalvar;
    @FXML private TableColumn<NotaProduto, String> colProdUnidade;
    @FXML private ComboBox<String> comboUnidade;

    // ── Tabela de produtos da nota ──────────────────────────────────────────
    @FXML private TableView<NotaProduto>           tabelaProdutos;
    @FXML private TableColumn<NotaProduto, String> colProdNome;
    @FXML private TableColumn<NotaProduto, String> colProdQtd;
    @FXML private TableColumn<NotaProduto, String> colProdVrUnit;
    @FXML private TableColumn<NotaProduto, String> colProdVrTotal;

    @FXML private ComboBox<Produto> comboProduto;
    @FXML private TextField         campoQtd;
    @FXML private TextField         campoVrUnit;
    @FXML private TextField         campoVrTotalItem;

    // ── Services ────────────────────────────────────────────────────────────
    private final NotaFiscalService notaFiscalService = new NotaFiscalService();
    private final EntidadeService   entidadeService   = new EntidadeService();
    private final ProdutoService    produtoService    = new ProdutoService();

    private final ObservableList<NotaProduto> produtos = FXCollections.observableArrayList();
    private boolean    salvo = false;
    private NotaFiscal notaEditando;

    // ── Inicialização ───────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        comboNatureza.setItems(FXCollections.observableArrayList(
                "Entrada", "Demonstração", "Retorno de Conserto",
                "Retorno de Mercadoria", "Saida"));
        campoData.setValue(LocalDate.now());

        colProdNome.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getProdutoDescricao()));
        colProdQtd.setCellValueFactory(c ->
                new SimpleStringProperty(fmt(c.getValue().getQuantidade(), "%.2f")));
        colProdVrUnit.setCellValueFactory(c ->
                new SimpleStringProperty(fmtM(c.getValue().getVrUnitario())));
        colProdVrTotal.setCellValueFactory(c ->
                new SimpleStringProperty(fmtM(c.getValue().getVrTotal())));
        colProdUnidade.setCellValueFactory(c -> {
            String un = c.getValue().getUnidadeMedida();
            return new SimpleStringProperty(un != null ? un : "UND");
        });
        tabelaProdutos.setItems(produtos);

        // Calcula Vr Total em tempo real enquanto o usuário digita
        campoVrUnit.textProperty().addListener((obs, old, novo) -> calcularTotal());
        campoQtd.textProperty().addListener((obs, old, novo) -> calcularTotal());

        carregarEntidades();
        carregarProdutos();
        comboUnidade.setItems(FXCollections.observableArrayList("UND", "KG", "PC", "CJ"));
        comboUnidade.setValue("UND");
    }

    // ── Carregamentos ───────────────────────────────────────────────────────
    private void carregarEntidades() {
        try {
            List<Entidade> entidades = entidadeService.listarTodos();
            comboEntidade.setItems(FXCollections.observableArrayList(entidades));
            comboEntidade.setConverter(new StringConverter<>() {
                public String toString(Entidade e) { return e == null ? "" : e.getRazaoSocial(); }
                public Entidade fromString(String s) { return null; }
            });
        } catch (SQLException e) {
            AlertUtil.erro("Erro ao carregar entidades: " + e.getMessage());
        }
    }

    private void carregarProdutos() {
        try {
            comboProduto.setItems(FXCollections.observableArrayList(produtoService.listarTodos()));
            comboProduto.setConverter(new StringConverter<>() {
                public String toString(Produto p) { return p == null ? "" : p.getDescricao(); }
                public Produto fromString(String s) { return null; }
            });
        } catch (SQLException e) {
            AlertUtil.erro("Erro ao carregar produtos: " + e.getMessage());
        }
    }

    // ── Edição (modo alterar) ───────────────────────────────────────────────
    public void setNotaFiscal(NotaFiscal nota) {
        this.notaEditando = nota;
        labelTitulo.setText("Alterar Nota Fiscal");
        btnSalvar.setText("Salvar Alteração");

        comboNatureza.setValue(nota.getNatureza());
        campoNumero.setText(valorOuVazio(nota.getNumero()));
        campoData.setValue(nota.getData());
        campoOrdemCompra.setText(valorOuVazio(nota.getOrdemCompra()));
        campoTransportadora.setText(valorOuVazio(nota.getTransportadora()));
        campoTransporteRs.setText(formatarDouble(nota.getTransporteRs()));
        campoDescontoRs.setText(formatarDouble(nota.getDescontoRs()));
        campoPesoBruto.setText(formatarDouble(nota.getPesoBruto()));
        campoPesoLiquido.setText(formatarDouble(nota.getPesoLiquido()));

        if (nota.getEntidadeId() != null) {
            comboEntidade.getItems().stream()
                    .filter(e -> e.getId() == nota.getEntidadeId())
                    .findFirst()
                    .ifPresent(comboEntidade::setValue);
        }

        try {
            produtos.setAll(notaFiscalService.listarProdutosPorNota(nota.getId()));
        } catch (SQLException e) {
            AlertUtil.erro("Erro ao carregar produtos da nota: " + e.getMessage());
        }
    }

    // ── Ações da tabela de produtos ─────────────────────────────────────────
    @FXML
    private void adicionarProduto() {
        Produto prod = comboProduto.getValue();
        if (prod == null) {
            AlertUtil.aviso("Selecione um produto antes de adicionar.");
            return;
        }

        Double qtd    = parseDouble(campoQtd.getText());
        Double vrUnit = parseDouble(campoVrUnit.getText());

        if (qtd == null || qtd <= 0) {
            AlertUtil.aviso("Informe uma quantidade válida (maior que zero).");
            return;
        }
        if (vrUnit == null || vrUnit < 0) {
            AlertUtil.aviso("Informe um valor unitário válido (maior ou igual a zero).");
            return;
        }

        boolean jaExiste = produtos.stream()
                .anyMatch(np -> np.getProdutoId() == prod.getId());
        if (jaExiste) {
            AlertUtil.aviso("Esse produto já foi adicionado à nota.");
            return;
        }

        double vrTotal = qtd * vrUnit;

        NotaProduto np = new NotaProduto();
        np.setProdutoId(prod.getId());
        np.setProdutoDescricao(prod.getDescricao());
        np.setQuantidade(qtd);
        np.setVrUnitario(vrUnit);
        np.setVrTotal(vrTotal);
        np.setUnidadeMedida(comboUnidade.getValue() != null ? comboUnidade.getValue() : "UND");
        produtos.add(np);

        comboProduto.setValue(null);
        campoQtd.clear();
        campoVrUnit.clear();
        campoVrTotalItem.clear();
        comboUnidade.setValue("UND");
    }

    @FXML
    private void removerProduto() {
        NotaProduto sel = tabelaProdutos.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtil.aviso("Selecione um produto na tabela para remover.");
            return;
        }
        if (AlertUtil.confirmar("Remover \"" + sel.getProdutoDescricao() + "\" da nota?")) {
            try {
                if (sel.getId() > 0) {
                    notaFiscalService.excluirProdutoDaNota(sel.getId());
                }
                produtos.remove(sel);
            } catch (SQLException e) {
                AlertUtil.erro("Erro ao remover produto: " + e.getMessage());
            }
        }
    }

    private void calcularTotal() {
        Double qtd  = parseDouble(campoQtd.getText());
        Double unit = parseDouble(campoVrUnit.getText());
        if (qtd != null && unit != null) {
            campoVrTotalItem.setText(String.format("%.2f", qtd * unit));
        } else {
            campoVrTotalItem.clear();
        }
    }

    // ── Salvar ──────────────────────────────────────────────────────────────
    @FXML
    private void salvar() {
        if (!validar()) return;
        try {
            NotaFiscal nf = notaEditando != null ? notaEditando : new NotaFiscal();

            nf.setNatureza(comboNatureza.getValue());
            nf.setNumero(campoNumero.getText().trim());
            nf.setData(campoData.getValue());
            nf.setOrdemCompra(textoOuNull(campoOrdemCompra.getText()));
            nf.setTransportadora(textoOuNull(campoTransportadora.getText()));
            nf.setTransporteRs(parseNullableDouble(campoTransporteRs.getText()));
            nf.setDescontoRs(parseNullableDouble(campoDescontoRs.getText()));
            nf.setPesoBruto(parseNullableDouble(campoPesoBruto.getText()));
            nf.setPesoLiquido(parseNullableDouble(campoPesoLiquido.getText()));

            if (comboEntidade.getValue() != null) {
                nf.setEntidadeId(comboEntidade.getValue().getId());
                nf.setEntidadeNome(comboEntidade.getValue().getRazaoSocial());
            } else {
                nf.setEntidadeId(null);
                nf.setEntidadeNome(null);
            }

            notaFiscalService.salvar(nf, new ArrayList<>(produtos));
            salvo = true;
            fecharJanela();

        } catch (NumberFormatException e) {
            AlertUtil.erro("Valores numéricos inválidos. Use formato como 10,5");
        } catch (SQLException e) {
            AlertUtil.erro("Erro ao salvar nota fiscal: " + e.getMessage());
        }
    }

    // ── Validação ───────────────────────────────────────────────────────────
    private boolean validar() {
        if (comboNatureza.getValue() == null) {
            AlertUtil.erro("Selecione a natureza."); return false;
        }
        if (campoNumero.getText() == null || campoNumero.getText().trim().isEmpty()) {
            AlertUtil.erro("Informe o número da nota."); return false;
        }
        if (campoData.getValue() == null) {
            AlertUtil.erro("Informe a data."); return false;
        }
        if (produtos.isEmpty()) {
            AlertUtil.erro("Adicione ao menos um produto à nota fiscal."); return false;
        }
        return true;
    }

    // ── Utilitários ─────────────────────────────────────────────────────────
    private Double parseDouble(String s) {
        try { return (s == null || s.isBlank()) ? null : Double.parseDouble(s.replace(",", ".")); }
        catch (NumberFormatException e) { return null; }
    }

    private Double parseNullableDouble(String texto) {
        String v = texto == null ? "" : texto.trim().replace(",", ".");
        return v.isEmpty() ? null : Double.parseDouble(v);
    }

    private String formatarDouble(Double valor) {
        return valor == null ? "" : String.format("%.2f", valor);
    }

    private String textoOuNull(String texto) {
        if (texto == null) return null;
        String t = texto.trim();
        return t.isEmpty() ? null : t;
    }

    private String valorOuVazio(String valor) { return valor == null ? "" : valor; }

    private String fmt(Double v, String pattern) { return v != null ? String.format(pattern, v) : "—"; }
    private String fmtM(Double v)               { return v != null ? String.format("R$ %.2f", v) : "—"; }

    @FXML private void cancelar() { fecharJanela(); }
    public boolean isSalvo()      { return salvo; }
    private void fecharJanela()   { ((Stage) btnSalvar.getScene().getWindow()).close(); }
}
