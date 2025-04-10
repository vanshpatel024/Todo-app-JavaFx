module todoapp.todoapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.mongodb.driver.sync.client;
    requires org.mongodb.driver.core;
    requires org.mongodb.bson;


    opens todoapp.todoapp to javafx.fxml;
    exports todoapp.todoapp;
}