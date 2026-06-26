package com.fundicao.controller;

import com.fundicao.model.Entidade;
import com.fundicao.model.NotaFiscal;
import com.fundicao.service.EntidadeService;
import com.fundicao.service.NotaFiscalService;
import com.fundicao.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class NotaFiscalDialogController {

    @FXML private Label labelTitulo;
    @FXML private ComboBox<String> comboNatureza;
    @FXML private TextField campoNumero;
    @FXML private DatePicker campoData;
    @FXML private ComboBox<Entidade> comboEntidade;
    @FXML private TextField campoOrdemCompra;
    @FXML private TextField campoTransportadora;
    @FXML private TextField campoTransporteRs;
    @FXML private TextField campoDescontoRs;
    @FXML private TextField campoPesoBruto;
    @FXML private TextField campoPesoLiquido;
    @FXML private Button btnSalvar;

    private final NotaFiscalService notaFiscalService = new NotaFiscalService();
    private final EntidadeService entidadeService = new EntidadeService();

    private boolean salvo = false;
    private NotaFiscal notaEditando;

    @FXML
    public void initialize() {
        comboNatureza.setItems(FXCollections.observableArrayList("Entrada", "Saida"));
        campoData.setValue(LocalDate.now());
        carregarEntidades();
    }

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
    }

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

            notaFiscalService.salvar(nf);
            salvo = true;
            fecharJanela();

        } catch (NumberFormatException e) {
            AlertUtil.erro("Valores numéricos inválidos. Use formato como 10,5");
        } catch (SQLException e) {
            AlertUtil.erro("Erro ao salvar nota fiscal: " + e.getMessage());
        }
    }

    private boolean validar() {
        if (comboNatureza.getValue() == null) {
            AlertUtil.erro("Selecione a natureza.");
            return false;
        }
        if (campoNumero.getText() == null || campoNumero.getText().trim().isEmpty()) {
            AlertUtil.erro("Informe o número da nota.");
            return false;
        }
        if (campoData.getValue() == null) {
            AlertUtil.erro("Informe a data.");
            return false;
        }
        return true;
    }

    private Double parseNullableDouble(String texto) {
        String valor = texto == null ? "" : texto.trim().replace(",", ".");
        return valor.isEmpty() ? null : Double.parseDouble(valor);
    }

    private String formatarDouble(Double valor) {
        return valor == null ? "" : String.format("%.2f", valor);
    }

    private String textoOuNull(String texto) {
        if (texto == null) return null;
        String t = texto.trim();
        return t.isEmpty() ? null : t;
    }

    private String valorOuVazio(String valor) {
        return valor == null ? "" : valor;
    }

    @FXML
    private void cancelar() { fecharJanela(); }

    public boolean isSalvo() { return salvo; }

    private void fecharJanela() {
        ((Stage) btnSalvar.getScene().getWindow()).close();
    }
}