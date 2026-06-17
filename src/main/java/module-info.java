module com.fundicao {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.xerial.sqlitejdbc;
    requires atlantafx.base;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;

    opens com.fundicao to javafx.fxml;
    opens com.fundicao.controller to javafx.fxml;

    exports com.fundicao;
}