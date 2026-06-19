package com.fundicao.controller;

import com.fundicao.model.Entidade;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class EntidadeDialogController {

    @FXML private TextField campoRazaoSocial;
    @FXML private ComboBox<String> comboTipo;
    @FXML private ComboBox<String> comboTipoPessoa;
    @FXML private TextField campoCnpjCpf;
    @FXML private TextField campoInscricaoEstadual;
    @FXML private TextField campoTelefone;
    @FXML private TextField campoEmail;
    @FXML private TextField campoRua;
    @FXML private TextField campoNumero;
    @FXML private TextField campoBairro;
    @FXML private TextField campoCidade;
    @FXML private TextField campoUf;
    @FXML private TextField campoCep;
    @FXML private ComboBox<String> comboSituacao;

    @FXML
    public void initialize() {
        comboTipo.getItems().addAll("Cliente", "Fornecedor");
        comboTipoPessoa.getItems().addAll("PJ", "PF");
        comboSituacao.getItems().addAll("Ativo", "Inativo");
        comboTipo.setValue("Cliente");
        comboTipoPessoa.setValue("PJ");
        comboSituacao.setValue("Ativo");
    }

    public void setEntidade(Entidade e) {
        if (e == null) return;
        campoRazaoSocial.setText(e.getRazaoSocial());
        comboTipo.setValue(e.getTipo());
        comboTipoPessoa.setValue(e.getTipoPessoa());
        campoCnpjCpf.setText(e.getCnpjCpf());
        campoInscricaoEstadual.setText(e.getInscricaoEstadual());
        campoTelefone.setText(e.getTelefone());
        campoEmail.setText(e.getEmail());
        campoRua.setText(e.getRua());
        campoNumero.setText(e.getNumero());
        campoBairro.setText(e.getBairro());
        campoCidade.setText(e.getCidade());
        campoUf.setText(e.getUf());
        campoCep.setText(e.getCep());
        comboSituacao.setValue(e.getSituacao());
    }

    public Entidade getEntidade() {
        Entidade e = new Entidade();
        e.setRazaoSocial(campoRazaoSocial.getText());
        e.setTipo(comboTipo.getValue());
        e.setTipoPessoa(comboTipoPessoa.getValue());
        e.setCnpjCpf(campoCnpjCpf.getText());
        e.setInscricaoEstadual(campoInscricaoEstadual.getText());
        e.setTelefone(campoTelefone.getText());
        e.setEmail(campoEmail.getText());
        e.setRua(campoRua.getText());
        e.setNumero(campoNumero.getText());
        e.setBairro(campoBairro.getText());
        e.setCidade(campoCidade.getText());
        e.setUf(campoUf.getText());
        e.setCep(campoCep.getText());
        e.setSituacao(comboSituacao.getValue());
        return e;
    }
}