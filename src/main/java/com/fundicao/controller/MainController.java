package com.fundicao.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.io.IOException;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private VBox btnEstoque;
    @FXML private VBox btnNotas;
    @FXML private VBox btnProdutos;
    @FXML private VBox btnEntidades;

    private VBox activeButton;

    @FXML
    public void initialize() {
        carregarTela("estoque");
        setActive(btnEstoque);
    }

    @FXML private void abrirEstoque()   { carregarTela("estoque");   setActive(btnEstoque);   }
    @FXML private void abrirNotas()     { carregarTela("notas");     setActive(btnNotas);     }
    @FXML private void abrirProdutos()  { carregarTela("produtos");  setActive(btnProdutos);  }
    @FXML private void abrirEntidades() { carregarTela("entidades"); setActive(btnEntidades); }

    private void carregarTela(String nome) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/fundicao/view/" + nome + "-view.fxml")
            );
            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());
        } catch (IOException e) {
            System.err.println("Erro ao carregar tela: " + nome + " — " + e.getMessage());
        }
    }

    private void setActive(VBox button) {
        if (activeButton != null) activeButton.getStyleClass().remove("nav-card-active");
        button.getStyleClass().add("nav-card-active");
        activeButton = button;
    }
}