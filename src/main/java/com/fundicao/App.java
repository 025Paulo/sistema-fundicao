package com.fundicao;

import atlantafx.base.theme.PrimerLight;
import com.fundicao.util.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        DatabaseManager.getInstance().inicializar();

        URL fxmlUrl = App.class.getResource("/com/fundicao/view/main-view.fxml");
        URL cssUrl = App.class.getResource("/com/fundicao/css/style.css");

        System.out.println("FXML URL: " + fxmlUrl);
        System.out.println("CSS URL: " + cssUrl);

        FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
        Scene scene = new Scene(fxmlLoader.load(), 1100, 680);

        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        stage.setTitle("Fundição — Sistema de Controle");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void stop() {
        DatabaseManager.getInstance().fechar();
    }
}

