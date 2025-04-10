package todoapp.todoapp;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.bson.Document;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class LoginPageController implements Initializable {

    //connecting to mongo client
    MongoClient mongoClient = MongoClients.create(
            "mongodb+srv://suchomartin2:IsuLrbYZTrhrQdaX@cluster1.wunkv.mongodb.net/?authSource=admin"
    );

    @FXML TextField usnmFieldC;
    @FXML TextField usnmFieldL;

    @FXML PasswordField passFieldC;
    @FXML PasswordField cfmPassFieldC;
    @FXML PasswordField passFieldL;

    @FXML ImageView logoLogin;

    @FXML Label errorLabelC;
    @FXML Label errorLabelL;

    @FXML AnchorPane createNewAccPane;
    @FXML AnchorPane loginPane;
    @FXML AnchorPane navBar;
    @FXML AnchorPane primPane;

    private double windowX = 0, windowY = 0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        //dragging effect
        Platform.runLater(() -> {
            Stage stage = (Stage) primPane.getScene().getWindow();

            navBar.setOnMousePressed(e -> {
                windowX = e.getSceneX();
                windowY = e.getSceneY();
            });

            navBar.setOnMouseDragged(e -> {
                stage.setX(e.getScreenX() - windowX);
                stage.setY(e.getScreenY() - windowY);
            });
        });
    }

    public boolean checkAutoLogin(Stage primaryStage) {
        try {
            String appDataPath = System.getenv("APPDATA");
            if (appDataPath == null) {
                appDataPath = System.getProperty("user.home") + "/.config";
            }

            Path loginFile = Paths.get(appDataPath, "TodoApp", "loginInfo.txt");

            if (Files.exists(loginFile)) {
                List<String> lines = Files.readAllLines(loginFile);
                if (!lines.isEmpty()) {
                    String[] credentials = lines.get(0).split(":");
                    if (credentials.length == 2) {
                        String savedUsername = credentials[0];
                        String savedPassword = credentials[1];

                        if (attemptLogin(savedUsername, savedPassword)) {
                            openMainApp(primaryStage);
                            return true;  // Auto-login successful
                        }
                    }
                }
            }

        } catch (IOException e) {
        }

        return false; // Auto-login failed
    }

    private boolean attemptLogin(String username, String password) {
        try {
            MongoDatabase database = mongoClient.getDatabase("TodoDB");

            // Check if collection exists
            boolean collectionExists = database.listCollectionNames()
                    .into(new ArrayList<>())
                    .contains(username);
            if (!collectionExists) {
                return false;
            }

            // Retrieve user data
            MongoCollection<Document> collection = database.getCollection(username);
            Document userDoc = collection.find(Filters.eq("title", "LoginInfo")).first();

            if (userDoc != null) {
                String storedPassword = userDoc.getString("Password");

                // If passwords match, login is successful
                return password.equals(storedPassword);
            }
        } catch (Exception e) {
        }
        return false;
    }

    public void CreateAccount(){

        if (usnmFieldC.getText().isEmpty()){
            ShowMsgLabel("Please enter username!", 0, errorLabelC);
            return;
        }
        if (passFieldC.getText().isEmpty()){
            ShowMsgLabel("Please enter password!", 0, errorLabelC);
            return;
        }
        if (cfmPassFieldC.getText().isEmpty()){
            ShowMsgLabel("Please confirm your password!", 0, errorLabelC);
            return;
        }
        if (!passFieldC.getText().equals(cfmPassFieldC.getText())) {
            ShowMsgLabel("Passwords do not match!", 0, errorLabelC);
            return;
        }

        MongoDatabase database = mongoClient.getDatabase("TodoDB");

        //check if a user with the same username already exists
        boolean userExists = database.listCollectionNames()
                .into(new ArrayList<>())
                .contains(usnmFieldC.getText());

        if (userExists) {
            ShowMsgLabel("User already exists! Please login.", 0, errorLabelC);
            usnmFieldC.clear();
            passFieldC.clear();
            cfmPassFieldC.clear();
            return;
        }

        database.createCollection(usnmFieldC.getText());
        MongoCollection<Document> collection = database.getCollection(usnmFieldC.getText());

        Document data = new Document("title", "LoginInfo")
                .append("Username", usnmFieldC.getText())
                .append("Password", passFieldC.getText());
        collection.insertOne(data);

        // Clear fields and show success message
        usnmFieldC.clear();
        passFieldC.clear();
        cfmPassFieldC.clear();
        ShowMsgLabel("Account Created Successfully! Please Login.", 1, errorLabelL);

        GoToLoginPage();
    }

    public void Login() {
        // Check if fields are empty
        if (usnmFieldL.getText().isEmpty()) {
            ShowMsgLabel("Please enter username!", 0, errorLabelL);
            return;
        }
        if (passFieldL.getText().isEmpty()) {
            ShowMsgLabel("Please enter password!", 0, errorLabelL);
            return;
        }

        // Get entered username & password
        String enteredUsername = usnmFieldL.getText();
        String enteredPassword = passFieldL.getText();

        try {
            // Access database
            MongoDatabase database = mongoClient.getDatabase("TodoDB");

            // Check if the collection (username) exists
            boolean collectionExists = database.listCollectionNames()
                    .into(new ArrayList<>())
                    .contains(enteredUsername);
            if (!collectionExists) {
                ShowMsgLabel("User does not exist!", 0, errorLabelL);
                usnmFieldL.clear();
                passFieldL.clear();
                return;
            }

            // Access the collection (user's document)
            MongoCollection<Document> collection = database.getCollection(enteredUsername);

            // Retrieve the user's login info
            Document userDoc = collection.find(Filters.eq("title", "LoginInfo")).first();

            if (userDoc != null) {
                // Get stored password
                String storedPassword = userDoc.getString("Password");

                // Check if entered password matches stored password
                if (enteredPassword.equals(storedPassword)) {
                    ShowMsgLabel("Login Successful!", 1, errorLabelL);

                    //SAVING LOCALLY
                    try {
                        // Get AppData Roaming directory
                        String appDataPath = System.getenv("APPDATA"); // Windows
                        if (appDataPath == null) {
                            appDataPath = System.getProperty("user.home") + "/.config"; // Linux/macOS
                        }

                        // Define directory and file path
                        Path todoAppDir = Paths.get(appDataPath, "TodoApp");
                        Path loginFile = todoAppDir.resolve("loginInfo.txt");

                        // Create directory if it doesn’t exist
                        if (!Files.exists(todoAppDir)) {
                            Files.createDirectories(todoAppDir);
                        }

                        // Use FileWriter in append mode instead of Files.newBufferedWriter (fixes errors)
                        try (FileWriter writer = new FileWriter(loginFile.toFile(), true);
                             BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
                            bufferedWriter.write(usnmFieldL.getText() + ":" + passFieldL.getText());
                            bufferedWriter.newLine();
                        }
                    } catch (IOException e) {
                        ShowMsgLabel("Failed to save login details locally!", 0, errorLabelL);
                        e.printStackTrace();
                        return;
                    }

                    // Redirect user to next screen or perform desired action
                    // Get current stage and open main app
                    Stage currentStage = (Stage) usnmFieldL.getScene().getWindow();
                    openMainApp(currentStage);

                } else {
                    ShowMsgLabel("Incorrect password!", 0, errorLabelL);
                    passFieldL.clear();
                    return;
                }
            } else {
                ShowMsgLabel("User not found!", 0, errorLabelL);
                usnmFieldL.clear();
                return;
            }
        } catch (Exception e) {
            ShowMsgLabel("Error: " + e.getMessage(), 0, errorLabelL);
        }

        // Clear input fields
        usnmFieldL.clear();
        passFieldL.clear();
    }

    public static String getSavedUsername() {
        try {
            // Get the AppData Roaming directory (cross-platform)
            String appDataPath = System.getenv("APPDATA"); // Windows
            if (appDataPath == null) {
                appDataPath = System.getProperty("user.home") + "/.config"; // Linux/macOS
            }

            // Define file path
            Path loginFile = Paths.get(appDataPath, "TodoApp", "loginInfo.txt");

            // Check if file exists
            if (!Files.exists(loginFile)) {
                return null; // No saved login
            }

            // Read the last line of the file (most recent saved login)
            String lastLine = null;
            try (BufferedReader reader = Files.newBufferedReader(loginFile)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lastLine = line;
                }
            }

            // Extract username from "username:password" format
            if (lastLine != null && lastLine.contains(":")) {
                return lastLine.split(":")[0]; // Return username
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null; // Return null if an error occurs or no username found
    }

    private void openMainApp(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("App.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);

            Stage stage = new Stage();
            Image icon = new Image(getClass().getResourceAsStream("/Todo.png"));
            stage.getIcons().add(icon);
            stage.setScene(scene);
            stage.setTitle("Main Application");
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setResizable(false);
            stage.show();

            // Close the login window if it exists
            if (primaryStage != null) {
                primaryStage.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void ShowMsgLabel(String err, int type, Label label){
        
        String color = (type == 0) ? "red" : "green";

        Platform.runLater(() -> {
            // Apply the style
            label.setStyle("-fx-text-fill: " + color + ";");
            label.setOpacity(1);
            label.setText(err);

            // Pause for 2 seconds before fading out
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(event -> {
                // Fade-out animation
                FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), label);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> label.setText("")); // Clear text after fading
                fadeOut.play();
            });

            pause.play(); // Start the delay before fade-out
        });

    }

    public void GoToCreatePage(){
        HideAllPages();
        createNewAccPane.setVisible(true);
    }

    public void GoToLoginPage(){
        HideAllPages();
        loginPane.setVisible(true);
    }

    public void HideAllPages(){
        createNewAccPane.setVisible(false);
        loginPane.setVisible(false);
    }

    public void CloseWindow(){
        Stage stage = (Stage) primPane.getScene().getWindow();
        stage.close();
    }

    public void MinimizeWindow(){
        Stage stage = (Stage) primPane.getScene().getWindow();
        stage.setIconified(true);
    }
}