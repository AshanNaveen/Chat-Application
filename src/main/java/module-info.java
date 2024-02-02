module lk.ijse.chatapplication {
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires com.jfoenix;

    requires org.controlsfx.controls;
    requires javafx.swing;
    requires ftpserver.core;

    opens lk.ijse.chatapplication to javafx.fxml;
    opens lk.ijse.chatapplication.controller to javafx.fxml,com.jfoenix;
    opens lk.ijse.chatapplication.assets.font to javafx.graphics;

    exports lk.ijse.chatapplication;
}