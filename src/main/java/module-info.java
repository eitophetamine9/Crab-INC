module com.example.crabinc {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.crabinc to javafx.fxml;
    exports com.example.crabinc;
}