package com.example.javafx;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


import java.awt.*;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SmartWasteManagementHomePage extends Application {

    private static final List<String> userIssues = new ArrayList<>();
    private static final HashMap<String, String> assignedTasks = new HashMap<>();
    private static final HashMap<String, File> uploadedPhotos = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {
        Text title = new Text("Smart Waste Management System");
        title.setFont(Font.font("Arial", 28));
        title.setFill(Color.DARKBLUE);

        Button adminButton = createStyledButton("Administrator", "#4CAF50");
        Button driverButton = createStyledButton("Driver", "#2196F3");
        Button userButton = createStyledButton("General User", "#FF5722");

        VBox buttonLayout = new VBox(20, adminButton, driverButton, userButton);
        buttonLayout.setAlignment(Pos.CENTER);

        Text footer = new Text("\u00A9 2025 Smart Waste Management System. All rights reserved.");
        footer.setFont(Font.font("Arial", 12));
        footer.setFill(Color.GRAY);

        VBox mainLayout = new VBox(30, title, buttonLayout, footer);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setStyle("-fx-padding: 30; -fx-background-color: linear-gradient(to bottom, #ffffff, #e0e0e0);");

        Scene scene = new Scene(mainLayout, 500, 400);
        primaryStage.setTitle("Smart Waste Management Home");
        primaryStage.setScene(scene);
        primaryStage.show();

        userButton.setOnAction(e -> openUserForm(primaryStage));
        adminButton.setOnAction(e -> openAdminDashboard(primaryStage));
        driverButton.setOnAction(e -> openDriverDashboard(primaryStage));
    }

    private void openUserForm(Stage primaryStage) {
        Text formTitle = createStyledText("User Issue Submission", 22, Color.DARKBLUE);

        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField();

        Label mobileLabel = new Label("Mobile:");
        TextField mobileField = new TextField();

        Label problemLabel = new Label("Problem:");
        TextArea problemArea = new TextArea();
        problemArea.setPrefRowCount(3);

        Label locationLabel = new Label("Location:");
        TextField locationField = new TextField();

        Label photoLabel = new Label("Upload Photo:");
        Button uploadPhotoButton = createStyledButton("Choose File", "#2196F3");
        Label photoPathLabel = new Label("No file chosen");

        final FileChooser fileChooser = new FileChooser();
        final File[] selectedFile = new File[1];

        uploadPhotoButton.setOnAction(e -> {
            selectedFile[0] = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile[0] != null) {
                photoPathLabel.setText(selectedFile[0].getName());
            }
        });

        Button submitButton = createStyledButton("Submit", "#4CAF50");
        Button backToHomeButton = createStyledButton("Back to Home", "#FF5722");



        GridPane formLayout = new GridPane();
        formLayout.setHgap(15);
        formLayout.setVgap(15);
        formLayout.setAlignment(Pos.CENTER);
        formLayout.setStyle("-fx-padding: 30; -fx-background-color: #f8f9fa; -fx-border-color: #cccccc; -fx-border-width: 1px;");

        formLayout.add(formTitle, 0, 0, 2, 1);
        formLayout.add(nameLabel, 0, 1);
        formLayout.add(nameField, 1, 1);
        formLayout.add(mobileLabel, 0, 2);
        formLayout.add(mobileField, 1, 2);
        formLayout.add(problemLabel, 0, 3);
        formLayout.add(problemArea, 1, 3);
        formLayout.add(locationLabel, 0, 4);
        formLayout.add(locationField, 1, 4);
        formLayout.add(photoLabel, 0, 5);
        formLayout.add(uploadPhotoButton, 1, 5);
        formLayout.add(photoPathLabel, 1, 6);
        formLayout.add(submitButton, 0, 7);
        formLayout.add(backToHomeButton, 1, 7);

        Scene userFormScene = new Scene(formLayout, 500, 500);
        primaryStage.setScene(userFormScene);

        submitButton.setOnAction(e -> {
            String name = nameField.getText();
            String mobile = mobileField.getText();
            String problem = problemArea.getText();
            String location = locationField.getText();

            String issueKey = name + mobile;
            userIssues.add("Name: " + name + "\nMobile: " + mobile + "\nProblem: " + problem + "\nLocation: " + location + "\n---");

            if (selectedFile[0] != null) {
                uploadedPhotos.put(issueKey, selectedFile[0]);
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Submission Successful");
            alert.setHeaderText(null);
            alert.setContentText("Your issue has been submitted successfully.");
            alert.showAndWait();

            start(primaryStage);
        });

        backToHomeButton.setOnAction(e -> start(primaryStage));
    }

    private void openAdminDashboard(Stage primaryStage) {
        Text dashboardTitle = createStyledText("Administrator Dashboard", 22, Color.DARKBLUE);

        TextArea issueDisplayArea = new TextArea();
        issueDisplayArea.setEditable(false);
        issueDisplayArea.setPrefRowCount(10);
        issueDisplayArea.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px; -fx-padding: 10;");

        loadIssuesIntoTextArea(issueDisplayArea);

        Button viewPhotoButton = createStyledButton("View Uploaded Photo", "#2196F3");
        Button assignTaskButton = createStyledButton("Assign Task to Driver", "#4CAF50");
        Button clearIssuesButton = createStyledButton("Clear Issues", "#F44336");
        Button backButton = createStyledButton("Back to Home", "#FF5722");

        viewPhotoButton.setOnAction(e -> {
            if (!userIssues.isEmpty()) {
                String issueKey = userIssues.get(0).split("\n")[0].replace("Name: ", "") + userIssues.get(0).split("\n")[1].replace("Mobile: ", "");
                File photo = uploadedPhotos.get(issueKey);

                if (photo != null) {
                    Image image = new Image(photo.toURI().toString());
                    ImageView imageView = new ImageView(image);
                    imageView.setFitHeight(300);
                    imageView.setPreserveRatio(true);

                    Alert imageAlert = new Alert(Alert.AlertType.INFORMATION);
                    imageAlert.setTitle("Uploaded Photo");
                    imageAlert.setGraphic(imageView);
                    imageAlert.setHeaderText(null);
                    imageAlert.showAndWait();
                } else {
                    showAlert(Alert.AlertType.WARNING, "No Photo", "No photo uploaded for this issue.");
                }
            }
        });

        assignTaskButton.setOnAction(e -> {
            if (!userIssues.isEmpty()) {
                assignedTasks.put("Driver", userIssues.remove(0));
                loadIssuesIntoTextArea(issueDisplayArea);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Task Assigned");
                alert.setHeaderText(null);
                alert.setContentText("Task successfully assigned to the driver.");
                alert.showAndWait();
            } else {
                showAlert(Alert.AlertType.WARNING, "No Issues", "No issues available to assign.");
            }
        });

        clearIssuesButton.setOnAction(e -> {
            userIssues.clear();
            assignedTasks.clear();
            uploadedPhotos.clear();
            loadIssuesIntoTextArea(issueDisplayArea);
        });

        backButton.setOnAction(e -> start(primaryStage));

        VBox dashboardLayout = new VBox(20, dashboardTitle, issueDisplayArea, viewPhotoButton, assignTaskButton, clearIssuesButton, backButton);
        dashboardLayout.setAlignment(Pos.CENTER);
        dashboardLayout.setStyle("-fx-padding: 30; -fx-background-color: #f8f9fa;");

        Scene adminScene = new Scene(dashboardLayout, 600, 500);
        primaryStage.setScene(adminScene);
    }
    private void openDriverDashboard(Stage primaryStage) {
        Text dashboardTitle = createStyledText("Driver Dashboard", 22, Color.DARKBLUE);

        TextArea taskDisplayArea = new TextArea();
        taskDisplayArea.setEditable(false);
        taskDisplayArea.setPrefRowCount(10);
        taskDisplayArea.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px; -fx-padding: 10;");

        if (assignedTasks.containsKey("Driver")) {
            taskDisplayArea.setText("Assigned Task:\n" + assignedTasks.get("Driver"));
        } else {
            taskDisplayArea.setText("No tasks assigned yet.");
        }

        Button viewPhotoButton = createStyledButton("View Uploaded Photo", "#2196F3");
        Button problemSolvedButton = createStyledButton("Problem Solved", "#4CAF50");
        Button uploadProofButton = createStyledButton("Upload Proof", "#2196F3");
        Button backButton = createStyledButton("Back to Home", "#FF5722");
        Button locationTrackButton = createStyledButton("Location Track", "#FF9800");

        FileChooser fileChooser = new FileChooser();

        viewPhotoButton.setOnAction(e -> {
            if (assignedTasks.containsKey("Driver")) {
                String task = assignedTasks.get("Driver");
                String[] taskDetails = task.split("\n");
                String issueKey = taskDetails[0].replace("Name: ", "") + taskDetails[1].replace("Mobile: ", "");

                File photo = uploadedPhotos.get(issueKey);
                if (photo != null) {
                    Image image = new Image(photo.toURI().toString());
                    ImageView imageView = new ImageView(image);
                    imageView.setFitHeight(300);
                    imageView.setPreserveRatio(true);

                    Alert imageAlert = new Alert(Alert.AlertType.INFORMATION);
                    imageAlert.setTitle("Uploaded Photo");
                    imageAlert.setGraphic(imageView);
                    imageAlert.setHeaderText(null);
                    imageAlert.showAndWait();
                } else {
                    showAlert(Alert.AlertType.WARNING, "No Photo", "No photo uploaded for this issue.");
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "No Task", "No task assigned to view photo.");
            }
        });

        uploadProofButton.setOnAction(e -> {
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                showAlert(Alert.AlertType.INFORMATION, "Upload Successful", "Proof uploaded: " + file.getName());
            }
        });

        problemSolvedButton.setOnAction(e -> {
            assignedTasks.remove("Driver");
            taskDisplayArea.setText("No tasks assigned yet.");
            showAlert(Alert.AlertType.INFORMATION, "Problem Solved", "The issue has been marked as resolved.");
        });

        backButton.setOnAction(e -> start(primaryStage));

        locationTrackButton.setOnAction(e -> {
            if (assignedTasks.containsKey("Driver")) {
                String task = assignedTasks.get("Driver");
                String[] taskDetails = task.split("\n");
                String location = taskDetails[3].replace("Location: ", "").trim();

                if (!location.isEmpty()) {
                    try {
                        String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8.toString());
                        String url = "https://www.google.com/maps?q=" + encodedLocation;
                        Desktop.getDesktop().browse(new URI(url));
                    } catch (Exception ex) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to open Google Maps: " + ex.getMessage());
                    }
                } else {
                    showAlert(Alert.AlertType.WARNING, "No Location", "No location available for this task.");
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "No Task", "No task assigned to track location.");
            }
        });

        // Group buttons for better layout
        HBox taskButtons = new HBox(10, viewPhotoButton, problemSolvedButton);
        taskButtons.setAlignment(Pos.CENTER);

        HBox actionButtons = new HBox(10, uploadProofButton, locationTrackButton);
        actionButtons.setAlignment(Pos.CENTER);

        VBox driverLayout = new VBox(20, dashboardTitle, taskDisplayArea, taskButtons, actionButtons, backButton);
        driverLayout.setAlignment(Pos.CENTER);
        driverLayout.setStyle("-fx-padding: 30; -fx-background-color: #f8f9fa;");

        Scene driverScene = new Scene(driverLayout, 600, 500);
        primaryStage.setScene(driverScene);
    }



    private Button createStyledButton(String text, String backgroundColor) {
        Button button = new Button(text);
        button.setStyle("-fx-font-size: 14px; -fx-background-color: " + backgroundColor + "; -fx-text-fill: white; -fx-padding: 10;");
        return button;
    }

    private Text createStyledText(String content, int fontSize, Color color) {
        Text text = new Text(content);
        text.setFont(Font.font("Arial", fontSize));
        text.setFill(color);
        return text;
    }

    private void loadIssuesIntoTextArea(TextArea textArea) {
        if (userIssues.isEmpty()) {
            textArea.setText("No issues reported yet.");
        } else {
            textArea.setText(String.join("\n", userIssues));
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
