module com.crabinc.crabinc {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;

    opens com.crabinc to javafx.fxml;
    exports com.crabinc;
    exports com.crabinc.app;
    opens com.crabinc.app to javafx.fxml;
    exports com.crabinc.controllers;
    opens com.crabinc.controllers to javafx.fxml;
}