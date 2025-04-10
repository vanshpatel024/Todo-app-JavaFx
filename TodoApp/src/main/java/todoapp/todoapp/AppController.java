package todoapp.todoapp;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.ColorInput;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.bson.Document;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static com.mongodb.client.model.Sorts.descending;

public class AppController implements Initializable {

    MongoClient mongoClient = MongoClients.create(
            "mongodb+srv://suchomartin2:IsuLrbYZTrhrQdaX@cluster1.wunkv.mongodb.net/?authSource=admin"
    );

    @FXML AnchorPane primPane;
    @FXML AnchorPane navBar;
    @FXML AnchorPane bgBlur;
    @FXML AnchorPane createNewTaskPane;
    @FXML AnchorPane displayAreaPane;
    @FXML AnchorPane userMenuPane;
    @FXML AnchorPane logoutPane;
    @FXML AnchorPane notiPane;
    @FXML AnchorPane editTaskPane;
    @FXML AnchorPane changeUsnmPane;
    @FXML AnchorPane changePassPane;

    @FXML TextField searchBar;
    @FXML TextField changeUsnmTxtField;

    @FXML PasswordField changeUsnmPassField;
    @FXML PasswordField changePassOldField;
    @FXML PasswordField changePassNewField;

    @FXML ImageView logoApp;

    @FXML Label notiMsgLabel;
    @FXML Label usnmLabel;
    @FXML Label remainingLabel;

    @FXML TextArea taskInput;
    @FXML TextArea editArea;

    @FXML Button editTaskBtn;
    @FXML Button addNewTaskBtn;
    @FXML Button accBtn;
    @FXML Button createNewTaskBtn;
    @FXML Button refreshBtn;

    private EventHandler<MouseEvent> outsideClickHandler;

    private SequentialTransition currentAnimation; // Store current animation

    private String userName;
    private String oldTask = "";

    private double windowX = 0, windowY = 0;

    private int currentlyEditingTaskId = -1;
    private int remainingTasks = 0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        //initializing values
        Platform.runLater(() -> {

            userName = LoginPageController.getSavedUsername();

            usnmLabel.setText(userName);

            InitializeIcons();

            searchBar.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.isEmpty()) {
                    DisplayTasksWithoutAnimation();
                } else {
                    DisplayFilteredTasks(newValue);
                }
            });

            DisplayTasks();

        });

        taskInput.setStyle(
                "-fx-control-inner-background: #222631; " +
                        "-fx-text-fill: #E0E0E0; " +
                        "-fx-border-radius: 5px; " +
                        "-fx-font-size: 14px; "
        );
        taskInput.borderProperty().setValue(Border.EMPTY);

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

    public void DisplayTasksWithoutAnimation() {
        searchBar.clear();
        displayAreaPane.getChildren().clear(); // Clear previous content

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(displayAreaPane.getPrefWidth(), displayAreaPane.getPrefHeight());
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: #0D0F14; -fx-border-width: 0px;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox taskContainer = new VBox(10);
        taskContainer.setPadding(new Insets(10));
        taskContainer.setStyle("-fx-background-color: transparent; -fx-background-color: #0D0F14;");

        if (userName == null) {
            return;
        }

        MongoDatabase database = mongoClient.getDatabase("TodoDB");
        MongoCollection<Document> collection = database.getCollection(userName);

        FindIterable<Document> tasks = collection.find()
                .sort(Sorts.orderBy(
                        Sorts.descending("status"),  // Pending first, Completed last
                        Sorts.descending("taskId")  // Sort by taskId within each status
                ));

        for (Document taskDoc : tasks) {
            String taskText = taskDoc.getString("task");
            String status = taskDoc.getString("status"); // Read task status
            if (taskText == null || status == null) {
                continue;  // Skip if invalid
            }

            // ** Base Pane for Task Item **
            AnchorPane taskPane = new AnchorPane();
            taskPane.setStyle("-fx-background-color: #222631; -fx-background-radius: 5px; -fx-border-radius: 5px; -fx-padding: 10px;");
            taskPane.setPrefWidth(displayAreaPane.getPrefWidth() - 35);
            taskPane.setCache(true);
            taskPane.setCacheHint(CacheHint.QUALITY);

            // ** Left HBox (Checkbox) **
            CheckBox checkBox = new CheckBox();
            checkBox.setSelected(status.equals("completed"));
            checkBox.getStyleClass().add("custom-checkbox");

            // ** Update DB when checkbox is clicked **
            checkBox.setOnAction(event -> {
                String newStatus = checkBox.isSelected() ? "completed" : "pending";
                collection.updateOne(
                        Filters.eq("task", taskText),
                        new Document("$set", new Document("status", newStatus))
                );
                RefreshRemainingTaskLabel();
            });

            HBox leftBox = new HBox(checkBox);
            leftBox.setPadding(new Insets(5, 10, 5, 10));
            leftBox.setAlignment(Pos.CENTER_LEFT);

            // ** Middle HBox (Task Label) **
            Label taskLabel = new Label(taskText);
            taskLabel.setWrapText(true);
            taskLabel.setStyle("-fx-text-fill: #E0E0E0; -fx-font-size: 14px;");
            taskLabel.setMaxWidth(displayAreaPane.getPrefWidth() - 200);

            HBox middleBox = new HBox(taskLabel);
            middleBox.setPadding(new Insets(5, 10, 5, 10));
            middleBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(middleBox, Priority.ALWAYS); // Allow task text to expand

            // ** Right HBox (Edit & Delete Buttons) **
            Image deleteIcon = new Image(getClass().getResourceAsStream("/delete.png"));
            ImageView deleteView = new ImageView(deleteIcon);
            deleteView.setFitWidth(16);
            deleteView.setFitHeight(16);

            Button deleteBtn = new Button();
            deleteBtn.setGraphic(deleteView);
            deleteBtn.setStyle("-fx-background-color: #222631; -fx-background-radius: 5px; -fx-border-radius: 5px;");
            deleteBtn.setOnMouseEntered(event -> {
                deleteBtn.setStyle("-fx-background-color: #7C3AED; -fx-background-radius: 5px; -fx-border-radius: 5px;");
            });
            deleteBtn.setOnMouseExited(event -> {
                deleteBtn.setStyle("-fx-background-color: #222631; -fx-background-radius: 5px; -fx-border-radius: 5px;");
            });

            deleteBtn.setOnAction(event -> {
                collection.deleteOne(Filters.eq("taskId", taskDoc.getInteger("taskId")));
                RefreshTaskIDs(collection);
                DisplayTasks();
                ShowNotification("Deleted Successfully");
            });

            Image editIcon = new Image(getClass().getResourceAsStream("/edit.png"));
            ImageView editView = new ImageView(editIcon);
            editView.setFitWidth(16);
            editView.setFitHeight(16);

            Button editBtn = new Button();
            editBtn.setGraphic(editView);
            editBtn.setStyle("-fx-background-color: #222631; -fx-background-radius: 5px; -fx-border-radius: 5px;");
            editBtn.setOnMouseEntered(event -> {
                editBtn.setStyle("-fx-background-color: #7C3AED; -fx-background-radius: 5px; -fx-border-radius: 5px;");
            });
            editBtn.setOnMouseExited(event -> {
                editBtn.setStyle("-fx-background-color: #222631; -fx-background-radius: 5px; -fx-border-radius: 5px;");
            });
            editBtn.setOnAction(event -> {
                bgBlur.setVisible(true);
                editTaskPane.setVisible(true);  // Show edit panel
                editArea.setText(taskText);  // Set current task text
                currentlyEditingTaskId = taskDoc.getInteger("taskId");  // Store task ID
                oldTask = editArea.getText().trim();
            });

            HBox rightBox = new HBox(10, editBtn, deleteBtn);
            rightBox.setPadding(new Insets(5, 10, 5, 10));
            rightBox.setAlignment(Pos.CENTER_RIGHT);

            // ** Wrapping Everything in One Main HBox **
            HBox mainBox = new HBox(leftBox, middleBox, rightBox);
            mainBox.setSpacing(10);
            mainBox.setAlignment(Pos.CENTER_LEFT); // Ensures vertical centering

            // ** Attach MainBox to AnchorPane **
            taskPane.getChildren().add(mainBox);
            AnchorPane.setTopAnchor(mainBox, 5.0);
            AnchorPane.setLeftAnchor(mainBox, 5.0);
            AnchorPane.setRightAnchor(mainBox, 5.0); // Ensures buttons stay at the right
            AnchorPane.setBottomAnchor(mainBox, 5.0);

            // ** Add TaskPane to Task Container **
            taskContainer.getChildren().add(taskPane);

        }

        scrollPane.setContent(taskContainer);
        displayAreaPane.getChildren().add(scrollPane);
        RefreshRemainingTaskLabel();
    }

    public void DisplayTasks() {
        searchBar.clear();
        displayAreaPane.getChildren().clear(); // Clear previous content

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(displayAreaPane.getPrefWidth(), displayAreaPane.getPrefHeight());
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: #0D0F14; -fx-border-width: 0px;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox taskContainer = new VBox(10);
        taskContainer.setPadding(new Insets(10));
        taskContainer.setStyle("-fx-background-color: transparent; -fx-background-color: #0D0F14;");

        if (userName == null) {
            ShowNotification("No user logged in!");
            return;
        }

        MongoDatabase database = mongoClient.getDatabase("TodoDB");
        MongoCollection<Document> collection = database.getCollection(userName);

        FindIterable<Document> tasks = collection.find()
                .sort(Sorts.orderBy(
                        Sorts.descending("status"),  // Pending first, Completed last
                        Sorts.descending("taskId")  // Sort by taskId within each status
                ));

        List<AnchorPane> taskPanes = new ArrayList<>(); // Store task panes for animation

        for (Document taskDoc : tasks) {
            String taskText = taskDoc.getString("task");
            String status = taskDoc.getString("status");
            if (taskText == null || status == null) continue;

            // ** Task Pane **
            AnchorPane taskPane = new AnchorPane();
            taskPane.setStyle("-fx-background-color: #222631; -fx-background-radius: 5px; -fx-border-radius: 5px; -fx-padding: 10px;");
            taskPane.setPrefWidth(displayAreaPane.getPrefWidth() - 35);
            taskPane.setCache(true);
            taskPane.setCacheHint(CacheHint.QUALITY);

            // ** Left HBox (Checkbox) **
            CheckBox checkBox = new CheckBox();
            checkBox.setSelected(status.equals("completed"));
            checkBox.getStyleClass().add("custom-checkbox");

            checkBox.setOnAction(event -> {
                String newStatus = checkBox.isSelected() ? "completed" : "pending";
                collection.updateOne(
                        Filters.eq("task", taskText),
                        new Document("$set", new Document("status", newStatus))
                );
                RefreshRemainingTaskLabel();
            });

            HBox leftBox = new HBox(checkBox);
            leftBox.setPadding(new Insets(5, 10, 5, 10));
            leftBox.setAlignment(Pos.CENTER_LEFT);

            // ** Middle HBox (Task Label) **
            Label taskLabel = new Label(taskText);
            taskLabel.setWrapText(true);
            taskLabel.setStyle("-fx-text-fill: #E0E0E0; -fx-font-size: 14px;");
            taskLabel.setMaxWidth(displayAreaPane.getPrefWidth() - 200);

            HBox middleBox = new HBox(taskLabel);
            middleBox.setPadding(new Insets(5, 10, 5, 10));
            middleBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(middleBox, Priority.ALWAYS);

            // ** Right HBox (Edit & Delete Buttons) **
            Button deleteBtn = createIconButton("/delete.png");
            Button editBtn = createIconButton("/edit.png");

            deleteBtn.setOnAction(event -> {
                collection.deleteOne(Filters.eq("taskId", taskDoc.getInteger("taskId")));
                RefreshTaskIDs(collection);
                DisplayTasks();
                ShowNotification("Deleted Successfully");
            });

            editBtn.setOnAction(event -> {
                bgBlur.setVisible(true);
                editTaskPane.setVisible(true);
                editArea.setText(taskText);
                currentlyEditingTaskId = taskDoc.getInteger("taskId");
                oldTask = editArea.getText().trim();
            });

            HBox rightBox = new HBox(10, editBtn, deleteBtn);
            rightBox.setPadding(new Insets(5, 10, 5, 10));
            rightBox.setAlignment(Pos.CENTER_RIGHT);

            // ** Wrapping Everything in One Main HBox **
            HBox mainBox = new HBox(leftBox, middleBox, rightBox);
            mainBox.setSpacing(10);
            mainBox.setAlignment(Pos.CENTER_LEFT);

            taskPane.getChildren().add(mainBox);
            AnchorPane.setTopAnchor(mainBox, 5.0);
            AnchorPane.setLeftAnchor(mainBox, 5.0);
            AnchorPane.setRightAnchor(mainBox, 5.0);
            AnchorPane.setBottomAnchor(mainBox, 5.0);

            // ** Store Pane for Animation **
            taskPanes.add(taskPane);
            taskContainer.getChildren().add(taskPane);
        }

        animateTaskPanes(taskPanes);

        scrollPane.setContent(taskContainer);
        displayAreaPane.getChildren().add(scrollPane);
        RefreshRemainingTaskLabel();
    }

    private Button createIconButton(String iconPath) {
        Image icon = new Image(getClass().getResourceAsStream(iconPath));
        ImageView iconView = new ImageView(icon);
        iconView.setFitWidth(16);
        iconView.setFitHeight(16);

        Button button = new Button();
        button.setGraphic(iconView);
        button.setStyle("-fx-background-color: #222631; -fx-background-radius: 5px; -fx-border-radius: 5px;");
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #7C3AED;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #222631;"));
        return button;
    }

    private void animateTaskPanes(List<AnchorPane> taskPanes) {
        double delay = 0; // Start delay

        for (AnchorPane taskPane : taskPanes) {
            // Set initial position (off-screen right) and opacity
            taskPane.setTranslateX(50); // Start 50px to the right
            taskPane.setOpacity(0); // Start fully transparent

            // ** Move in Animation **
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(400), taskPane);
            slideIn.setToX(0); // Move to final position
            slideIn.setInterpolator(Interpolator.EASE_OUT); // Smooth easing

            // ** Fade in Animation **
            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), taskPane);
            fadeIn.setToValue(1); // Fade to full opacity

            // ** Play animations sequentially **
            ParallelTransition parallelTransition = new ParallelTransition(slideIn, fadeIn);
            parallelTransition.setDelay(Duration.millis(delay));
            parallelTransition.play();

            delay += 80; // Stagger animations (80ms delay per task)
        }
    }

    public void DisplayFilteredTasks(String filter) {
        displayAreaPane.getChildren().clear();

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(displayAreaPane.getPrefWidth(), displayAreaPane.getPrefHeight());
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: #0D0F14; -fx-border-width: 0px;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox taskContainer = new VBox(10);
        taskContainer.setPadding(new Insets(10));
        taskContainer.setStyle("-fx-background-color: transparent; -fx-background-color: #0D0F14;");

        if (userName == null) {
            ShowNotification("No user logged in.");
            return;
        }

        MongoDatabase database = mongoClient.getDatabase("TodoDB");
        MongoCollection<Document> collection = database.getCollection(userName);

        FindIterable<Document> tasks = collection.find()
                .sort(Sorts.orderBy(
                        Sorts.descending("status"),  // Pending first, Completed last
                        Sorts.descending("taskId")  // Sort by taskId within each status
                ));

        for (Document taskDoc : tasks) {
            String taskText = taskDoc.getString("task");
            String status = taskDoc.getString("status");
            if (taskText == null || status == null || !taskText.toLowerCase().contains(filter.toLowerCase())) {
                continue;
            }

            // ** Task Pane **
            AnchorPane taskPane = new AnchorPane();
            taskPane.setStyle("-fx-background-color: #222631; -fx-background-radius: 5px; -fx-border-radius: 5px; -fx-padding: 10px;");
            taskPane.setPrefWidth(displayAreaPane.getPrefWidth() - 35);
            taskPane.setCache(true);
            taskPane.setCacheHint(CacheHint.QUALITY);

            // ** Checkbox **
            CheckBox checkBox = new CheckBox();
            checkBox.setSelected(status.equals("completed"));
            checkBox.getStyleClass().add("custom-checkbox");

            // ** Checkbox Action to update DB **
            checkBox.setOnAction(event -> {
                String newStatus = checkBox.isSelected() ? "completed" : "pending";
                collection.updateOne(
                        Filters.eq("task", taskText),
                        new Document("$set", new Document("status", newStatus))
                );
                RefreshRemainingTaskLabel();
            });

            HBox leftBox = new HBox(checkBox);
            leftBox.setPadding(new Insets(5, 10, 5, 10));
            leftBox.setAlignment(Pos.CENTER_LEFT);

            // ** Task Label **
            Label taskLabel = new Label(taskText);
            taskLabel.setWrapText(true);
            taskLabel.setStyle("-fx-text-fill: #E0E0E0; -fx-font-size: 14px;");
            taskLabel.setMaxWidth(displayAreaPane.getPrefWidth() - 200);

            HBox middleBox = new HBox(taskLabel);
            middleBox.setPadding(new Insets(5, 10, 5, 10));
            middleBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(middleBox, Priority.ALWAYS);

            // ** Edit and Delete Buttons **
            Button deleteBtn = new Button();
            Button editBtn = new Button();

            setupButton(deleteBtn, "/delete.png", () -> {
                collection.deleteOne(Filters.eq("taskId", taskDoc.getInteger("taskId")));
                RefreshTaskIDs(collection);
                DisplayTasks();
                ShowNotification("Deleted Successfully");
            });

            setupButton(editBtn, "/edit.png", () -> {
                bgBlur.setVisible(true);
                editTaskPane.setVisible(true);  // Show edit panel
                editArea.setText(taskText);  // Set current task text
                currentlyEditingTaskId = taskDoc.getInteger("taskId");  // Store task ID
                oldTask = editArea.getText().trim();
            });

            HBox rightBox = new HBox(10, editBtn, deleteBtn);
            rightBox.setPadding(new Insets(5, 10, 5, 10));
            rightBox.setAlignment(Pos.CENTER_RIGHT);

            // ** Main HBox Layout **
            HBox mainBox = new HBox(leftBox, middleBox, rightBox);
            mainBox.setSpacing(10);
            mainBox.setAlignment(Pos.CENTER_LEFT);

            taskPane.getChildren().add(mainBox);
            AnchorPane.setTopAnchor(mainBox, 5.0);
            AnchorPane.setLeftAnchor(mainBox, 5.0);
            AnchorPane.setRightAnchor(mainBox, 5.0);
            AnchorPane.setBottomAnchor(mainBox, 5.0);

            taskContainer.getChildren().add(taskPane);
        }

        scrollPane.setContent(taskContainer);
        displayAreaPane.getChildren().add(scrollPane);
        RefreshRemainingTaskLabel();
    }

    private void setupButton(Button button, String iconPath, Runnable action) {
        Image icon = new Image(getClass().getResourceAsStream(iconPath));
        ImageView iconView = new ImageView(icon);
        iconView.setFitWidth(16);
        iconView.setFitHeight(16);

        button.setGraphic(iconView);
        button.setStyle("-fx-background-color: #222631; -fx-background-radius: 5px; -fx-border-radius: 5px;");
        button.setOnMouseEntered(event -> button.setStyle("-fx-background-color: #7C3AED; -fx-background-radius: 5px; -fx-border-radius: 5px;"));
        button.setOnMouseExited(event -> button.setStyle("-fx-background-color: #222631; -fx-background-radius: 5px; -fx-border-radius: 5px;"));
        button.setOnAction(event -> action.run());
    }

    public void AddTask() {
        bgBlur.setVisible(true);
        createNewTaskPane.setVisible(true);

        createNewTaskBtn.setOnAction(e -> {
            String taskText = taskInput.getText().trim();
            if (!taskText.isEmpty()) {
                saveTaskToDatabase(taskText);
            }
        });

    }

    public void ConfirmEditTask() {
        String updatedText = editArea.getText().trim();

        if (updatedText.isEmpty()) {
            oldTask = "";
            ShowNotification("Task cannot be empty!");
            currentlyEditingTaskId = -1;
            bgBlur.setVisible(false);
            editTaskPane.setVisible(false);
            editArea.clear();
            return;
        }

        if (updatedText.equals(oldTask)) {
            ShowNotification("No changes made!");
            oldTask = "";
            currentlyEditingTaskId = -1;
            bgBlur.setVisible(false);
            editTaskPane.setVisible(false);
            editArea.clear();
            return;
        }

        MongoDatabase database = mongoClient.getDatabase("TodoDB");
        MongoCollection<Document> collection = database.getCollection(userName);

        collection.updateOne(
                Filters.eq("taskId", currentlyEditingTaskId),
                new Document("$set", new Document("task", updatedText))
        );

        oldTask = "";
        editArea.clear();
        currentlyEditingTaskId = -1;
        bgBlur.setVisible(false);
        editTaskPane.setVisible(false); // Hide edit panel
        DisplayTasks(); // Refresh task list
        ShowNotification("Task updated successfully!");
        RefreshRemainingTaskLabel();
    }

    public void ShowChangePass(){
        userMenuPane.setVisible(false);
        bgBlur.setVisible(true);
        changePassPane.setVisible(true);
    }

    public void ChangePass(){
        String oldPassword = changePassOldField.getText().trim();
        String newPassword = changePassNewField.getText().trim();

        if (oldPassword.isEmpty()) {
            ShowNotification("Enter your Current Password!");
            return;
        }
        if (newPassword.isEmpty()) {
            ShowNotification("Enter your New Password!");
            return;
        }

        // Connect to MongoDB
        MongoDatabase database = mongoClient.getDatabase("TodoDB");
        MongoCollection<Document> collection = database.getCollection(userName);

        // Find user login info
        Document userDoc = collection.find(Filters.eq("title", "LoginInfo")).first();
        if (userDoc == null || !userDoc.getString("Password").equals(oldPassword)) {
            ShowNotification("Your current password is incorrect!");
            return;
        }

        // Update password in the database
        collection.updateOne(
                Filters.eq("title", "LoginInfo"),
                new Document("$set", new Document("Password", newPassword))
        );

        //LOCALLY SAVING
        try {
            // Get AppData Roaming directory
            String appDataPath = System.getenv("APPDATA"); // Windows
            if (appDataPath == null) {
                appDataPath = System.getProperty("user.home") + "/.config"; // Linux/macOS
            }

            // Define directory and file path
            java.nio.file.Path todoAppDir = Paths.get(appDataPath, "TodoApp");
            Path loginFile = todoAppDir.resolve("loginInfo.txt");

            loginFile.toFile().delete();

            // Create directory if it doesn’t exist
            if (!Files.exists(todoAppDir)) {
                Files.createDirectories(todoAppDir);
            }

            // Use FileWriter in append mode instead of Files.newBufferedWriter (fixes errors)
            try (FileWriter writer = new FileWriter(loginFile.toFile(), true);
                 BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
                bufferedWriter.write(userName + ":" + newPassword);
                bufferedWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        ShowNotification("Password changed successfully!");

        changePassOldField.clear();
        changePassNewField.clear();
        bgBlur.setVisible(false);
        changePassPane.setVisible(false);
    }

    public void CancelChangePass(){
        changePassOldField.clear();
        changePassNewField.clear();
        bgBlur.setVisible(false);
        changePassPane.setVisible(false);
    }

    public void ShowChangeUsnm(){
        userMenuPane.setVisible(false);
        bgBlur.setVisible(true);
        changeUsnmPane.setVisible(true);
    }

    public void ChangeUsnm() {
        String newUsername = changeUsnmTxtField.getText().trim();
        String enteredPassword = changeUsnmPassField.getText().trim();

        if (newUsername.isEmpty()){
            ShowNotification("Username cannot be empty!");
            return;
        }

        if (enteredPassword.isEmpty()){
            ShowNotification("Enter your password!");
            return;
        }

        MongoDatabase database = mongoClient.getDatabase("TodoDB");
        MongoCollection<Document> collection = database.getCollection(userName);

        Document userDoc = collection.find(Filters.eq("title", "LoginInfo")).first();
        String storedPassword = userDoc.getString("Password");
        if (userDoc == null || !storedPassword.equals(enteredPassword)) {
            ShowNotification("Incorrect password!");
            return;
        }

        for (String collectionName : database.listCollectionNames()) {
            if (collectionName.equals(newUsername)) {
                ShowNotification("Username already taken!");
                return;
            }
        }

        collection.updateOne(
                Filters.eq("username", userName),
                new Document("$set", new Document("username", newUsername))
        );

        MongoCollection<Document> oldUserTasks = database.getCollection(userName);
        MongoCollection<Document> newUserTasks = database.getCollection(newUsername);

        for (Document task : oldUserTasks.find()) {
            newUserTasks.insertOne(task);
        }
        oldUserTasks.drop();

        userName = newUsername;
        usnmLabel.setText(userName);

        //LOCALLY SAVING
        try {
            // Get AppData Roaming directory
            String appDataPath = System.getenv("APPDATA"); // Windows
            if (appDataPath == null) {
                appDataPath = System.getProperty("user.home") + "/.config"; // Linux/macOS
            }

            // Define directory and file path
            java.nio.file.Path todoAppDir = Paths.get(appDataPath, "TodoApp");
            Path loginFile = todoAppDir.resolve("loginInfo.txt");

            loginFile.toFile().delete();

            // Create directory if it doesn’t exist
            if (!Files.exists(todoAppDir)) {
                Files.createDirectories(todoAppDir);
            }

            // Use FileWriter in append mode instead of Files.newBufferedWriter (fixes errors)
            try (FileWriter writer = new FileWriter(loginFile.toFile(), true);
                 BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
                bufferedWriter.write(newUsername + ":" + enteredPassword);
                bufferedWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        ShowNotification("Username changed successfully!");

        changeUsnmPassField.clear();
        changeUsnmTxtField.clear();
        bgBlur.setVisible(false);
        changeUsnmPane.setVisible(false);
        DisplayTasks();
    }

    public void CancelAddTask(){
        taskInput.clear();
        bgBlur.setVisible(false);
        createNewTaskPane.setVisible(false);
        RefreshRemainingTaskLabel();
    }

    public void CancelChangeUsnm(){
        changeUsnmPassField.clear();
        changeUsnmTxtField.clear();
        bgBlur.setVisible(false);
        changeUsnmPane.setVisible(false);
    }

    private void saveTaskToDatabase(String taskText) {
        taskInput.clear();
        bgBlur.setVisible(false);
        createNewTaskPane.setVisible(false);

        if (userName == null) {
            return;
        }

        MongoDatabase database = mongoClient.getDatabase("TodoDB");
        MongoCollection<Document> collection = database.getCollection(userName);

        Document lastTask = collection.find()
                .sort(descending("taskId"))
                .first();

        int newTaskId = (lastTask != null && lastTask.containsKey("taskId") && lastTask.getInteger("taskId") != null)
                ? lastTask.getInteger("taskId") + 1
                : 1;


        Document task = new Document("taskId", newTaskId)
                .append("task", taskText)
                .append("status", "pending")
                .append("timestamp", System.currentTimeMillis());

        collection.insertOne(task);

        DisplayTasks();
        ShowNotification("Task Added!");
        RefreshRemainingTaskLabel();
    }

    private void RefreshTaskIDs(MongoCollection<Document> collection) {
        FindIterable<Document> tasks = collection.find().sort(Sorts.ascending("taskId")); // Get sorted tasks

        int newId = 1;
        for (Document task : tasks) {
            Integer oldId = task.getInteger("taskId"); // Get task ID

            if (oldId == null) continue; // Skip if null (prevents NullPointerException)

            if (!oldId.equals(newId)) { // Update only if ID is inconsistent
                collection.updateOne(Filters.eq("taskId", oldId), Updates.set("taskId", newId));
            }
            newId++; // Increment ID for next task
        }
        RefreshRemainingTaskLabel();
    }

    private void RefreshRemainingTaskLabel(){
        MongoDatabase database = mongoClient.getDatabase("TodoDB");
        MongoCollection<Document> collection = database.getCollection(userName);

        remainingTasks = (int) collection.countDocuments(Filters.eq("status", "pending"));

        Platform.runLater(() -> {

            remainingLabel.setText(remainingTasks + " Tasks Remaining");

        });
    }

    public void ShowNotification(String notificationMessage) {
        if (notiPane == null || notiMsgLabel == null) return;

        notiMsgLabel.setText(notificationMessage);

        if (currentAnimation != null && currentAnimation.getStatus() == Animation.Status.RUNNING) {
            currentAnimation.stop();
        }

        notiPane.setVisible(true);
        notiPane.setTranslateY(100);
        notiPane.setOpacity(0);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), notiPane);
        slideIn.setToY(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), notiPane);
        fadeIn.setToValue(1);
        fadeIn.setInterpolator(Interpolator.EASE_BOTH);

        ParallelTransition showAnimation = new ParallelTransition(slideIn, fadeIn);

        PauseTransition stayTime = new PauseTransition(Duration.seconds(1.5));

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), notiPane);
        slideOut.setToY(100);
        slideOut.setInterpolator(Interpolator.EASE_IN);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), notiPane);
        fadeOut.setToValue(0);
        fadeOut.setInterpolator(Interpolator.EASE_BOTH);

        ParallelTransition hideAnimation = new ParallelTransition(slideOut, fadeOut);

        hideAnimation.setOnFinished(event -> notiPane.setVisible(false));

        currentAnimation = new SequentialTransition(showAnimation, stayTime, hideAnimation);
        currentAnimation.play();
    }

    private void InitializeIcons(){

        //color tint effect
        ColorAdjust grayscale = new ColorAdjust();
        grayscale.setSaturation(-1); // Convert image to grayscale

        ColorInput colorOverlay = new ColorInput(0, 0, 16, 16, javafx.scene.paint.Color.web("#E0E0E0"));
        Blend blend = new Blend(BlendMode.SRC_ATOP, grayscale, colorOverlay);

        //add new task button------------------------------------------------------------------------
        Image iconImage1 = new Image(getClass().getResourceAsStream("/plus.png"));
        ImageView icon1 = new ImageView(iconImage1);
        icon1.setFitWidth(16);
        icon1.setFitHeight(16);

        icon1.setEffect(blend);

        // Set button text and icon
        addNewTaskBtn.setText("New Task");
        addNewTaskBtn.setGraphic(icon1);

        // Apply styling
        addNewTaskBtn.setFont(Font.font("Roboto", FontWeight.BOLD, 12)); // Bold font
        addNewTaskBtn.setStyle("-fx-text-fill: #E0E0E0; -fx-graphic-text-gap: 8px;");


        //more button------------------------------------------------------------------------
        Image iconImage2 = new Image(getClass().getResourceAsStream("/user.png"));
        ImageView icon2 = new ImageView(iconImage2);
        icon2.setFitWidth(16);
        icon2.setSmooth(true);  // Enables smooth scaling
        icon2.setPreserveRatio(true);  // Maintains aspect ratio
        icon2.setCache(true);
        icon2.setCacheHint(CacheHint.QUALITY);
        icon2.setEffect(blend);
        accBtn.setGraphic(icon2);

        //refresh button-------------------------------------------------------------------
        Image refreshImage = new Image(getClass().getResourceAsStream("/refresh.png"));
        ImageView imageView = new ImageView(refreshImage);

        imageView.setFitWidth(16);
        imageView.setFitHeight(16);

        refreshBtn.setText("Refresh");
        refreshBtn.setGraphic(imageView);

        refreshBtn.setFont(Font.font("Roboto", FontWeight.BOLD, 12));
        refreshBtn.setStyle("-fx-text-fill: #E0E0E0; -fx-graphic-text-gap: 8px;");

        refreshBtn.setOnAction(event -> {
            DisplayTasks();
        });
    }

    public void OpenUserMenu() {
        if (!userMenuPane.isVisible()){
            userMenuPane.setVisible(true);
        }

        // Create event handler to close menu when clicking outside
        outsideClickHandler = event -> {
            if (!userMenuPane.getBoundsInParent().contains(event.getSceneX(), event.getSceneY())) {
                userMenuPane.setVisible(false);
                userMenuPane.getScene().removeEventFilter(MouseEvent.MOUSE_PRESSED, outsideClickHandler); // Remove event handler
            }
        };

        // Add the event filter dynamically
        userMenuPane.getScene().addEventFilter(MouseEvent.MOUSE_PRESSED, outsideClickHandler);
    }

    public void ShowLogoutConfirmation() {
        userMenuPane.setVisible(false);
        bgBlur.setVisible(true);
        logoutPane.setVisible(true);
    }

    public void Logout() {
        try {
            // Get AppData Roaming directory (Cross-platform)
            String appDataPath = System.getenv("APPDATA"); // Windows
            if (appDataPath == null) {
                appDataPath = System.getProperty("user.home") + "/.config"; // Linux/macOS
            }

            // Define the file path as a simple string
            String loginFilePath = appDataPath + "/TodoApp/loginInfo.txt";

            // Delete the login file if it exists
            File file = new File(loginFilePath);
            if (file.exists()) {
                file.delete();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginPage.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);

            Stage stage = (Stage) primPane.getScene().getWindow();
            Image icon = new Image(getClass().getResourceAsStream("/Todo.png"));
            stage.getIcons().add(icon);
            stage.setScene(scene);
            stage.setResizable(false);
            stage.setTitle("Todo App");
            stage.centerOnScreen();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void CloseLogoutConfirmation() {
        bgBlur.setVisible(false);
        logoutPane.setVisible(false);
    }

    public void CancelEdit(){
        oldTask = "";
        currentlyEditingTaskId = -1;
        editArea.clear();
        bgBlur.setVisible(false);
        editTaskPane.setVisible(false);
        RefreshRemainingTaskLabel();
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
