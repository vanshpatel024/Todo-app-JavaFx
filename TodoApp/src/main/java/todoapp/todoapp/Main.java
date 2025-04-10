package todoapp.todoapp;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        LoginPageController loginController = new LoginPageController();

        if (loginController.checkAutoLogin(stage)) {
            // Auto-login successful, main app is already loaded
            return;
        }

        Image icon = new Image(getClass().getResourceAsStream("/Todo.png"));
        stage.getIcons().add(icon);

        // Auto-login failed, load the login page
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("LoginPage.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 450);
        scene.setFill(Color.TRANSPARENT);

        stage.setTitle("Todo App");
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }


    public static void main(String[] args) {
        System.setProperty("prism.forceSw", "true");
        System.setProperty("prism.allowHiDPI", "true");
        System.setProperty("prism.lcdtext", "true");  // Smoother text rendering
        System.setProperty("prism.text", "t2k");      // Better text anti-aliasing
        System.setProperty("prism.forcePow2", "false"); // Prevents forced low-res textures
        System.setProperty("prism.order", "d3d, es2");
        System.setProperty("prism.vsync", "true");
        launch();
    }
}