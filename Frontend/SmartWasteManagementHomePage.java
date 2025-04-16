package com.example.javafx;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class SmartWasteManagementHomePage extends Application {

    private static final HashMap<String, TaskWithId> assignedTasks = new HashMap<>();
    private static final int DRIVER_PASSCODE = 1713;
    private static final int ADMIN_PASSCODE = 1121;
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Z])(?=.*[!@#$%^&*])(?=.{6,}).*$");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[a-zA-Z\\s]*$"); // Pattern to check for alphabets and spaces only
    private static final HashMap<String, String> loggedInUsers = new HashMap<>();
    private static final String PROFILE_FILE = "user_profile.txt";
    private static final String[] DRIVERS = {"Driver 1", "Driver 2", "Driver 3", "Driver 4"};

    // Twilio credentials
    private static final String TWILIO_ACCOUNT_SID = "AC****************************59";
    private static final String TWILIO_AUTH_TOKEN = "aa*****************************39";
    private static final String TWILIO_PHONE_NUMBER = "+1***********7";

    // Initialize Twilio
    static {
        Twilio.init(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN);
    }

    private String arole;

    private static class TaskWithId {
        String taskText;
        ObjectId issueId;
        File proofPhoto;

        TaskWithId(String taskText, ObjectId issueId) {
            this.taskText = taskText;
            this.issueId = issueId;
            this.proofPhoto = null;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        loadLoggedInUsersFromFile();

        Background background = new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#1e3c72")),
                        new Stop(1, Color.web("#2a5298"))),
                CornerRadii.EMPTY, Insets.EMPTY));

        Text title = new Text("CleanMitra: Smart Waste Management System");
        title.setFont(Font.font("Montserrat", FontWeight.BOLD, 36));
        title.setFill(Color.WHITE);
        title.setTextAlignment(TextAlignment.CENTER);
        title.setWrappingWidth(600);

        StackPane titleContainer = new StackPane(title);
        titleContainer.setPadding(new Insets(0, 20, 0, 20));
        title.setWrappingWidth(500);

        Button adminButton = createModernButton("Administrator", "#4CAF50");
        Button driverButton = createModernButton("Driver", "#2196F3");
        Button userButton = createModernButton("General User", "#FF5722");

        addButtonAnimation(adminButton);
        addButtonAnimation(driverButton);
        addButtonAnimation(userButton);

        VBox buttonLayout = new VBox(30, adminButton, driverButton, userButton);
        buttonLayout.setAlignment(Pos.CENTER);
        buttonLayout.setPadding(new Insets(20));
        buttonLayout.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 5);");

        Text footer = new Text("© 2025 Smart Waste Management System. All rights reserved.");
        footer.setFont(Font.font("Roboto", FontWeight.LIGHT, 14));
        footer.setFill(Color.web("#ffffff", 0.7));

        Pane particlePane = createParticleEffectPane();

        VBox mainLayout = new VBox(40, titleContainer, buttonLayout, footer);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(40));
        mainLayout.setBackground(background);

        StackPane root = new StackPane(particlePane, mainLayout);
        Scene scene = new Scene(root, 600, 500);
        scene.getStylesheets().add("https://fonts.googleapis.com/css2?family=Montserrat:wght@700&family=Roboto:wght@300&display=swap");

        primaryStage.setTitle("Smart Waste Management Home");
        primaryStage.setScene(scene);
        primaryStage.show();

        userButton.setOnAction(e -> {
            String loggedInUser = getLoggedInUserByRole("General User");
            if (loggedInUser != null) {
                openUserForm(primaryStage);
            } else {
                openLoginSignup(primaryStage, "General User");
            }
        });

        adminButton.setOnAction(e -> {
            String loggedInUser = getLoggedInUserByRole("Administrator");
            if (loggedInUser != null) {
                openAdminDashboard(primaryStage);
            } else {
                openPasscodeVerification(primaryStage, "Administrator", null);
            }
        });

        driverButton.setOnAction(e -> {
            String loggedInUser = getLoggedInUserByRole("Driver");
            if (loggedInUser != null) {
                openDriverDashboard(primaryStage);
            } else {
                openPasscodeVerification(primaryStage, "Driver", null);
            }
        });
    }

    private void openUserForm(Stage primaryStage) {
        Background background = new Background(new BackgroundFill(Color.web("#ecf0f1"), CornerRadii.EMPTY, Insets.EMPTY));

        Text formTitle = new Text("User Issue Submission");
        formTitle.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        formTitle.setFill(Color.web("#2c3e50"));

        ImageView profileIcon = createProfileIcon(primaryStage, "General User");

        BorderPane titlePane = new BorderPane();
        titlePane.setCenter(formTitle);
        titlePane.setRight(profileIcon);
        BorderPane.setAlignment(formTitle, Pos.CENTER);
        BorderPane.setAlignment(profileIcon, Pos.CENTER_RIGHT);

        Label nameLabel = new Label("Name:");
        nameLabel.setTextFill(Color.web("#2c3e50"));
        TextField nameField = new TextField();
        nameField.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #2c3e50; -fx-background-radius: 5; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");

        Label mobileLabel = new Label("Mobile:");
        mobileLabel.setTextFill(Color.web("#2c3e50"));
        TextField mobileField = new TextField();
        mobileField.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #2c3e50; -fx-background-radius: 5; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");

        Button verifyMobileButton = createEnhancedModernButton("Verify Number", "#f39c12");
        Label verificationStatus = new Label("");
        verificationStatus.setTextFill(Color.web("#e74c3c"));

        Label problemLabel = new Label("Problem:");
        problemLabel.setTextFill(Color.web("#2c3e50"));
        TextArea problemArea = new TextArea();
        problemArea.setPrefRowCount(3);
        problemArea.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #2c3e50; -fx-background-radius: 5; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");

        Label locationLabel = new Label("Location:");
        locationLabel.setTextFill(Color.web("#2c3e50"));
        TextField locationField = new TextField();
        locationField.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #2c3e50; -fx-background-radius: 5; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");

        Label photoLabel = new Label("Upload Photo (Optional):");
        photoLabel.setTextFill(Color.web("#2c3e50"));
        Button uploadPhotoButton = createEnhancedModernButton("Choose File", "#3498db");
        Label photoPathLabel = new Label("No file chosen");
        photoPathLabel.setTextFill(Color.web("#7f8c8d"));

        final FileChooser fileChooser = new FileChooser();
        final File[] selectedFile = new File[1];
        final boolean[] isMobileVerified = {false};
        final String[] generatedOTP = {""};

        uploadPhotoButton.setOnAction(e -> {
            selectedFile[0] = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile[0] != null) {
                photoPathLabel.setText(selectedFile[0].getName());
            }
        });

        Button submitButton = createEnhancedModernButton("Submit", "#2ecc71");
        Button backToHomeButton = createEnhancedModernButton("Back to Home", "#e74c3c");
        Button logoutButton = createEnhancedModernLogoutButton(primaryStage, "General User");

        GridPane formLayout = new GridPane();
        formLayout.setHgap(15);
        formLayout.setVgap(15);
        formLayout.setAlignment(Pos.CENTER);
        formLayout.setPadding(new Insets(30));
        formLayout.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        formLayout.add(titlePane, 0, 0, 2, 1);
        formLayout.add(nameLabel, 0, 1);
        formLayout.add(nameField, 1, 1);
        formLayout.add(mobileLabel, 0, 2);
        formLayout.add(mobileField, 1, 2);
        formLayout.add(verifyMobileButton, 0, 3);
        formLayout.add(verificationStatus, 1, 3);
        formLayout.add(problemLabel, 0, 4);
        formLayout.add(problemArea, 1, 4);
        formLayout.add(locationLabel, 0, 5);
        formLayout.add(locationField, 1, 5);
        formLayout.add(photoLabel, 0, 6);
        formLayout.add(uploadPhotoButton, 1, 6);
        formLayout.add(photoPathLabel, 1, 7);
        formLayout.add(submitButton, 0, 8);
        formLayout.add(backToHomeButton, 1, 8);
        formLayout.add(logoutButton, 0, 9, 2, 1);

        VBox root = new VBox(formLayout);
        root.setBackground(background);
        root.setAlignment(Pos.CENTER);
        Scene userFormScene = new Scene(root, 600, 650);
        primaryStage.setScene(userFormScene);

        verifyMobileButton.setOnAction(e -> {
            String mobile = mobileField.getText().trim();
            if (!mobile.matches("\\d{10}")) {
                showAlert(Alert.AlertType.ERROR, "Invalid Mobile Number", "Please enter a valid 10-digit mobile number.");
                return;
            }

            generatedOTP[0] = String.format("%06d", (int)(Math.random() * 1000000));
            String formattedMobile = "+91" + mobile;

            try {
                Message message = Message.creator(
                                new PhoneNumber(formattedMobile),
                                new PhoneNumber(TWILIO_PHONE_NUMBER),
                                "Your CleanMitra OTP is: " + generatedOTP[0] + ". Valid for 5 minutes.")
                        .create();

                Dialog<String> otpDialog = new Dialog<>();
                otpDialog.setTitle("Mobile Verification");
                otpDialog.setHeaderText("Enter the OTP sent to " + mobile);

                TextField otpField = new TextField();
                otpField.setPromptText("Enter 6-digit OTP");
                Button verifyOTPButton = createEnhancedModernButton("Verify OTP", "#2ecc71");

                VBox dialogContent = new VBox(10, otpField, verifyOTPButton);
                dialogContent.setAlignment(Pos.CENTER);
                dialogContent.setPadding(new Insets(20));
                otpDialog.getDialogPane().setContent(dialogContent);
                otpDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

                verifyOTPButton.setOnAction(event -> {
                    String enteredOTP = otpField.getText().trim();
                    if (enteredOTP.equals(generatedOTP[0])) {
                        isMobileVerified[0] = true;
                        verificationStatus.setText("Verified");
                        verificationStatus.setTextFill(Color.web("#2ecc71"));
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Mobile number verified successfully!");
                        otpDialog.close();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Invalid OTP", "Please enter the correct OTP.");
                    }
                });

                otpDialog.showAndWait();

            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "SMS Failed", "Failed to send OTP: " + ex.getMessage());
            }
        });

        submitButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String mobile = mobileField.getText().trim();
            String problem = problemArea.getText().trim();
            String location = locationField.getText().trim();

            if (name.isEmpty() || mobile.isEmpty() || problem.isEmpty() || location.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Form Incomplete", "Please fill all required fields.");
                return;
            }

            if (!NAME_PATTERN.matcher(name).matches()) {
                showAlert(Alert.AlertType.ERROR, "Invalid Name", "Please enter only alphabets and spaces in the name field.");
                return;
            }

            if (!mobile.matches("\\d{10}")) {
                showAlert(Alert.AlertType.ERROR, "Invalid Mobile Number", "Please enter a valid 10-digit mobile number.");
                return;
            }

            if (!isMobileVerified[0]) {
                showAlert(Alert.AlertType.ERROR, "Verification Required", "Please verify your mobile number first.");
                return;
            }

            Document issueDoc = new Document("name", name)
                    .append("mobile", mobile)
                    .append("problem", problem)
                    .append("location", location)
                    .append("status", "Pending");

            MongoDBUtil.insertIssue(issueDoc, selectedFile[0]);
            showAlert(Alert.AlertType.INFORMATION, "Submission Successful", "Your issue has been submitted.");
            nameField.clear();
            mobileField.clear();
            problemArea.clear();
            locationField.clear();
            photoPathLabel.setText("No file chosen");
            selectedFile[0] = null;
            isMobileVerified[0] = false;
            verificationStatus.setText("");
            verificationStatus.setTextFill(Color.web("#e74c3c"));
            start(primaryStage);
        });

        backToHomeButton.setOnAction(e -> start(primaryStage));
    }

    private void openAdminDashboard(Stage primaryStage) {
        Background background = new Background(new BackgroundFill(Color.web("#ecf0f1"), CornerRadii.EMPTY, Insets.EMPTY));

        Text dashboardTitle = new Text("Administrator Dashboard");
        dashboardTitle.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        dashboardTitle.setFill(Color.web("#2c3e50"));

        ImageView profileIcon = createProfileIcon(primaryStage, "Administrator");

        BorderPane titlePane = new BorderPane();
        titlePane.setCenter(dashboardTitle);
        titlePane.setRight(profileIcon);
        BorderPane.setAlignment(dashboardTitle, Pos.CENTER);
        BorderPane.setAlignment(profileIcon, Pos.CENTER_RIGHT);

        TextArea issueDisplayArea = new TextArea();
        issueDisplayArea.setEditable(false);
        issueDisplayArea.setPrefRowCount(10);
        issueDisplayArea.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #2c3e50; -fx-background-radius: 5; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
        loadIssuesIntoTextArea(issueDisplayArea);

        Button viewPhotoButton = createEnhancedModernButton("View Uploaded Photo", "#3498db");
        Button assignTaskButton = createEnhancedModernButton("Assign Task to Driver", "#2ecc71");
        Button clearIssuesButton = createEnhancedModernButton("Clear Issues", "#e74c3c");
        Button backButton = createEnhancedModernButton("Back to Home", "#e74c3c");
        Button logoutButton = createEnhancedModernLogoutButton(primaryStage, "Administrator");

        viewPhotoButton.setOnAction(e -> {
            List<Document> issues = MongoDBUtil.getPendingIssues();
            if (!issues.isEmpty()) {
                Document issue = issues.get(0);
                ObjectId photoId = issue.getObjectId("photoId");
                if (photoId != null) {
                    InputStream photoStream = MongoDBUtil.getPhotoStream(photoId);
                    if (photoStream != null) {
                        Image image = new Image(photoStream);
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
                    showAlert(Alert.AlertType.WARNING, "No Photo", "No photo uploaded for this issue.");
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "No Issues", "No pending issues to view photo.");
            }
        });

        assignTaskButton.setOnAction(e -> {
            List<Document> issues = MongoDBUtil.getPendingIssues();
            if (!issues.isEmpty()) {
                Document issue = issues.get(0);
                String issueText = "Name: " + issue.getString("name") + "\nMobile: " + issue.getString("mobile") +
                        "\nProblem: " + issue.getString("problem") + "\nLocation: " + issue.getString("location") + "\n---";
                ObjectId issueId = issue.getObjectId("_id");

                ChoiceDialog<String> dialog = new ChoiceDialog<>(DRIVERS[0], DRIVERS);
                dialog.setTitle("Assign Task");
                dialog.setHeaderText("Select Driver to Assign Task");
                dialog.setContentText("Choose a driver:");

                dialog.showAndWait().ifPresent(selectedDriver -> {
                    if (assignedTasks.containsKey(selectedDriver)) {
                        showAlert(Alert.AlertType.WARNING, "Driver Busy",
                                selectedDriver + " already has an assigned task.");
                    } else {
                        assignedTasks.put(selectedDriver, new TaskWithId(issueText, issueId));
                        MongoDBUtil.updateIssueStatus(issueId, "Assigned");
                        loadIssuesIntoTextArea(issueDisplayArea);
                        showAlert(Alert.AlertType.INFORMATION, "Task Assigned",
                                "Task assigned to " + selectedDriver + ".");
                    }
                });
            } else {
                showAlert(Alert.AlertType.WARNING, "No Issues", "No issues available to assign.");
            }
        });

        clearIssuesButton.setOnAction(e -> {
            MongoDBUtil.clearIssues();
            assignedTasks.clear();
            loadIssuesIntoTextArea(issueDisplayArea);
        });

        backButton.setOnAction(e -> start(primaryStage));

        HBox topButtons = new HBox(15, viewPhotoButton, assignTaskButton);
        topButtons.setAlignment(Pos.CENTER);
        topButtons.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-padding: 10;");

        HBox bottomButtons = new HBox(15, clearIssuesButton, backButton);
        bottomButtons.setAlignment(Pos.CENTER);
        bottomButtons.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-padding: 10;");

        VBox dashboardLayout = new VBox(20, titlePane, issueDisplayArea, topButtons, bottomButtons, logoutButton);
        dashboardLayout.setAlignment(Pos.CENTER);
        dashboardLayout.setPadding(new Insets(30));
        dashboardLayout.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        VBox root = new VBox(dashboardLayout);
        root.setBackground(background);
        root.setAlignment(Pos.CENTER);
        Scene adminScene = new Scene(root, 700, 600);
        primaryStage.setScene(adminScene);
    }

    private void openDriverDashboard(Stage primaryStage) {
        Background background = new Background(new BackgroundFill(Color.web("#ecf0f1"), CornerRadii.EMPTY, Insets.EMPTY));

        Text dashboardTitle = new Text("Driver Dashboard");
        dashboardTitle.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        dashboardTitle.setFill(Color.web("#2c3e50"));

        ImageView profileIcon = createProfileIcon(primaryStage, "Driver");

        BorderPane titlePane = new BorderPane();
        titlePane.setCenter(dashboardTitle);
        titlePane.setRight(profileIcon);
        BorderPane.setAlignment(dashboardTitle, Pos.CENTER);
        BorderPane.setAlignment(profileIcon, Pos.CENTER_RIGHT);

        TextArea taskDisplayArea = new TextArea();
        taskDisplayArea.setEditable(false);
        taskDisplayArea.setPrefRowCount(10);
        taskDisplayArea.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #2c3e50; -fx-background-radius: 5; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");

        String loggedInUser = getLoggedInUserByRole("Driver");
        String driverNumber = getDriverNumberFromLoggedInUser(loggedInUser);

        if (driverNumber != null && assignedTasks.containsKey(driverNumber)) {
            taskDisplayArea.setText("Assigned Task:\n" + assignedTasks.get(driverNumber).taskText);
        } else {
            taskDisplayArea.setText("No tasks assigned yet.");
        }

        Button viewPhotoButton = createEnhancedModernButton("View Uploaded Photo", "#3498db");
        Button problemSolvedButton = createEnhancedModernButton("Problem Solved", "#2ecc71");
        Button uploadProofButton = createEnhancedModernButton("Upload Proof", "#3498db");
        Button backButton = createEnhancedModernButton("Back to Home", "#e74c3c");
        Button locationTrackButton = createEnhancedModernButton("Location Track", "#f39c12");
        Button logoutButton = createEnhancedModernLogoutButton(primaryStage, "Driver");

        FileChooser fileChooser = new FileChooser();

        viewPhotoButton.setOnAction(e -> {
            if (driverNumber != null && assignedTasks.containsKey(driverNumber)) {
                TaskWithId task = assignedTasks.get(driverNumber);
                Document issue = MongoDBUtil.findIssueById(task.issueId);
                if (issue != null) {
                    ObjectId photoId = issue.getObjectId("photoId");
                    if (photoId != null) {
                        InputStream photoStream = MongoDBUtil.getPhotoStream(photoId);
                        if (photoStream != null) {
                            Image image = new Image(photoStream);
                            ImageView imageView = new ImageView(image);
                            imageView.setFitHeight(300);
                            imageView.setPreserveRatio(true);

                            Alert imageAlert = new Alert(Alert.AlertType.INFORMATION);
                            imageAlert.setTitle("Uploaded Photo");
                            imageAlert.setGraphic(imageView);
                            imageAlert.setHeaderText(null);
                            imageAlert.showAndWait();
                        } else {
                            showAlert(Alert.AlertType.WARNING, "No Photo", "Failed to retrieve photo.");
                        }
                    } else {
                        showAlert(Alert.AlertType.WARNING, "No Photo", "No photo uploaded for this issue.");
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Could not find the assigned issue.");
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "No Task", "No task assigned to view photo.");
            }
        });

        uploadProofButton.setOnAction(e -> {
            if (driverNumber != null && assignedTasks.containsKey(driverNumber)) {
                File file = fileChooser.showOpenDialog(primaryStage);
                if (file != null) {
                    assignedTasks.get(driverNumber).proofPhoto = file;
                    showAlert(Alert.AlertType.INFORMATION, "Proof Selected", "Proof photo selected: " + file.getName());
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "No Task", "No task assigned to upload proof.");
            }
        });

        problemSolvedButton.setOnAction(e -> {
            if (driverNumber != null && assignedTasks.containsKey(driverNumber)) {
                TaskWithId task = assignedTasks.get(driverNumber);
                if (task.proofPhoto == null) {
                    showAlert(Alert.AlertType.ERROR, "Proof Required", "Please upload a proof photo first.");
                    return;
                }
                Document issue = MongoDBUtil.findIssueById(task.issueId);
                if (issue != null) {
                    String userMobile = issue.getString("mobile");
                    String userName = issue.getString("name");
                    MongoDBUtil.updateIssueWithProof(task.issueId, "Problem Solved", task.proofPhoto);
                    assignedTasks.remove(driverNumber);
                    taskDisplayArea.setText("No tasks assigned yet.");

                    boolean smsSent = sendSmsToUser(userMobile, userName);
                    if (smsSent) {
                        showAlert(Alert.AlertType.INFORMATION, "Problem Solved",
                                "Problem Solved. SMS sent to " + userName + ".");
                    } else {
                        showAlert(Alert.AlertType.INFORMATION, "Problem Solved", "Problem Solved.");
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Could not find the issue to resolve.");
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "No Task", "No task assigned to mark as solved.");
            }
        });

        backButton.setOnAction(e -> start(primaryStage));

        locationTrackButton.setOnAction(e -> {
            if (driverNumber != null && assignedTasks.containsKey(driverNumber)) {
                String task = assignedTasks.get(driverNumber).taskText;
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

        HBox taskButtons = new HBox(15, viewPhotoButton, problemSolvedButton);
        taskButtons.setAlignment(Pos.CENTER);
        taskButtons.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-padding: 10;");

        HBox actionButtons = new HBox(15, uploadProofButton, locationTrackButton);
        actionButtons.setAlignment(Pos.CENTER);
        actionButtons.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-padding: 10;");

        VBox driverLayout = new VBox(20, titlePane, taskDisplayArea, taskButtons, actionButtons, backButton, logoutButton);
        driverLayout.setAlignment(Pos.CENTER);
        driverLayout.setPadding(new Insets(30));
        driverLayout.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        VBox root = new VBox(driverLayout);
        root.setBackground(background);
        root.setAlignment(Pos.CENTER);
        Scene driverScene = new Scene(root, 700, 600);
        primaryStage.setScene(driverScene);
    }

    private Button createModernButton(String text, String baseColor) {
        Button button = new Button(text);
        button.setFont(Font.font("Montserrat", FontWeight.BOLD, 16));
        button.setStyle(
                "-fx-background-color: " + baseColor + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 15 30 15 30; " +
                        "-fx-background-radius: 50; " +
                        "-fx-border-radius: 50; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 3); " +
                        "-fx-cursor: hand;"
        );
        return button;
    }

    private Button createEnhancedModernButton(String text, String baseColor) {
        Button button = new Button(text);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        button.setStyle(
                "-fx-background-color: " + baseColor + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 10 20 10 20; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-radius: 8; " +
                        "-fx-cursor: hand;"
        );

        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: " + darkenColor(baseColor) + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 10 20 10 20; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-radius: 8; " +
                        "-fx-cursor: hand;"
        ));
        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: " + baseColor + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 10 20 10 20; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-radius: 8; " +
                        "-fx-cursor: hand;"
        ));

        return button;
    }

    private String darkenColor(String hexColor) {
        Color color = Color.web(hexColor);
        double factor = 0.8;
        return String.format("#%02x%02x%02x",
                (int) (color.getRed() * 255 * factor),
                (int) (color.getGreen() * 255 * factor),
                (int) (color.getBlue() * 255 * factor));
    }

    private void addButtonAnimation(Button button) {
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(200), button);
        scaleUp.setToX(1.1);
        scaleUp.setToY(1.1);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(200), button);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);

        button.setOnMouseEntered(e -> scaleUp.playFromStart());
        button.setOnMouseExited(e -> scaleDown.playFromStart());
    }

    private Pane createParticleEffectPane() {
        Pane pane = new Pane();
        pane.setStyle("-fx-background-color: transparent;");

        for (int i = 0; i < 20; i++) {
            Circle particle = new Circle(Math.random() * 5 + 2, Color.web("#ffffff", 0.3));
            particle.setCenterX(Math.random() * 700);
            particle.setCenterY(Math.random() * 600);

            TranslateTransition move = new TranslateTransition(Duration.millis(5000 + Math.random() * 5000), particle);
            move.setByX(Math.random() * 100 - 50);
            move.setByY(Math.random() * 100 - 50);
            move.setCycleCount(Animation.INDEFINITE);
            move.setAutoReverse(true);
            move.play();

            FadeTransition fade = new FadeTransition(Duration.millis(2000 + Math.random() * 2000), particle);
            fade.setFromValue(0.2);
            fade.setToValue(0.8);
            fade.setCycleCount(Animation.INDEFINITE);
            fade.setAutoReverse(true);
            fade.play();

            pane.getChildren().add(particle);
        }
        return pane;
    }

    private ImageView createProfileIcon(Stage primaryStage, String role) {
        Image profileImage;
        try {
            profileImage = new Image(getClass().getResourceAsStream("/com/example/javafx/Logo1.png"));
            if (profileImage.isError()) {
                throw new Exception("Failed to load local image: " + profileImage.getException().getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error loading Logo1.png from resources: " + e.getMessage());
            profileImage = new Image("https://via.placeholder.com/30x30.png?text=User");
        }

        ImageView profileIcon = new ImageView(profileImage);
        profileIcon.setFitHeight(30);
        profileIcon.setFitWidth(30);
        profileIcon.setStyle("-fx-cursor: hand;");
        profileIcon.setOnMouseClicked(e -> showProfilePopup(primaryStage, role));

        return profileIcon;
    }

    private String getLoggedInUserByRole(String role) {
        for (HashMap.Entry<String, String> entry : loggedInUsers.entrySet()) {
            if (entry.getValue().equals(role)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private String getDriverNumberFromLoggedInUser(String loggedInUser) {
        if (loggedInUser != null && loggedInUser.contains(":")) {
            return loggedInUser.split(":")[1];
        }
        return null;
    }

    private void loadLoggedInUsersFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(PROFILE_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    loggedInUsers.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            System.out.println("No previous user profile found or error loading: " + e.getMessage());
        }
    }

    private void saveLoggedInUsersToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PROFILE_FILE))) {
            for (HashMap.Entry<String, String> entry : loggedInUsers.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save profile data: " + e.getMessage());
        }
    }

    private void openPasscodeVerification(Stage primaryStage, String role, String emailAfterLogin) {
        Text title = createStyledText(role + " Passcode Verification", 22, Color.DARKBLUE);

        Label passcodeLabel = new Label("Enter Passcode:");
        TextField passcodeField = new TextField();

        Button verifyButton = createEnhancedModernButton("Verify", "#4CAF50");
        Button backButton = createEnhancedModernButton("Back", "#FF5722");

        GridPane passcodeLayout = new GridPane();
        passcodeLayout.setHgap(15);
        passcodeLayout.setVgap(15);
        passcodeLayout.setAlignment(Pos.CENTER);
        passcodeLayout.setStyle("-fx-padding: 30; -fx-background-color: #f8f9fa; -fx-border-color: #cccccc; -fx-border-width: 1px;");

        passcodeLayout.add(title, 0, 0, 2, 1);
        passcodeLayout.add(passcodeLabel, 0, 1);
        passcodeLayout.add(passcodeField, 1, 1);
        passcodeLayout.add(verifyButton, 0, 2);
        passcodeLayout.add(backButton, 1, 2);

        Scene passcodeScene = new Scene(passcodeLayout, 400, 200);
        primaryStage.setScene(passcodeScene);

        verifyButton.setOnAction(e -> {
            String passcode = passcodeField.getText();
            boolean isValidPasscode = role.equals("Administrator") ?
                    passcode.equals(String.valueOf(ADMIN_PASSCODE)) :
                    passcode.equals(String.valueOf(DRIVER_PASSCODE));

            if (isValidPasscode) {
                if (emailAfterLogin == null) {
                    openLoginSignup(primaryStage, role);
                } else {
                    showAlert(Alert.AlertType.INFORMATION, "Login Successful", "Welcome, " + emailAfterLogin + "!");
                    if (role.equals("Administrator")) {
                        openAdminDashboard(primaryStage);
                    } else {
                        openDriverDashboard(primaryStage);
                    }
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Verification Failed", "Invalid passcode.");
            }
        });

        backButton.setOnAction(e -> start(primaryStage));
    }

    private void openLoginSignup(Stage primaryStage, String role) {
        Text title = createStyledText(role + " Login/Signup", 22, Color.DARKBLUE);

        Label emailLabel = new Label("Email:");
        TextField emailField = new TextField();

        Label passwordLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();

        Label driverNumberLabel = new Label("Driver Number:");
        ComboBox<String> driverNumberCombo = new ComboBox<>();
        driverNumberCombo.getItems().addAll(DRIVERS);
        driverNumberCombo.setValue(DRIVERS[0]);

        Button loginButton = createEnhancedModernButton("Login", "#4CAF50");
        Button signupButton = createEnhancedModernButton("Signup", "#2196F3");
        Button forgotPasswordButton = createEnhancedModernButton("Forgot Password?", "#f39c12");
        Button backButton = createEnhancedModernButton("Back to Home", "#FF5722");

        GridPane loginLayout = new GridPane();
        loginLayout.setHgap(15);
        loginLayout.setVgap(15);
        loginLayout.setAlignment(Pos.CENTER);
        loginLayout.setStyle("-fx-padding: 30; -fx-background-color: #f8f9fa; -fx-border-color: #cccccc; -fx-border-width: 1px;");

        loginLayout.add(title, 0, 0, 2, 1);
        loginLayout.add(emailLabel, 0, 1);
        loginLayout.add(emailField, 1, 1);
        loginLayout.add(passwordLabel, 0, 2);
        loginLayout.add(passwordField, 1, 2);
        if (role.equals("Driver")) {
            loginLayout.add(driverNumberLabel, 0, 3);
            loginLayout.add(driverNumberCombo, 1, 3);
            loginLayout.add(loginButton, 0, 4);
            loginLayout.add(signupButton, 1, 4);
            loginLayout.add(forgotPasswordButton, 0, 5, 2, 1);
            loginLayout.add(backButton, 0, 6, 2, 1);
            driverNumberLabel.setVisible(false);
            driverNumberCombo.setVisible(false);
        } else {
            loginLayout.add(loginButton, 0, 3);
            loginLayout.add(signupButton, 1, 3);
            loginLayout.add(forgotPasswordButton, 0, 4, 2, 1);
            loginLayout.add(backButton, 0, 5, 2, 1);
        }

        Scene loginScene = new Scene(loginLayout, 400, role.equals("Driver") ? 350 : 350);
        primaryStage.setScene(loginScene);

        loginButton.setOnAction(e -> {
            String email = emailField.getText().trim();
            String password = passwordField.getText();

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                showAlert(Alert.AlertType.ERROR, "Invalid Email", "Please enter a valid email address.");
                return;
            }

            if (role.equals("Driver")) {
                String driverNumber = MongoDBUtil.validateDriverLogin(email, password);
                if (driverNumber != null) {
                    String loginKey = email + ":" + driverNumber;
                    loggedInUsers.put(loginKey, role);
                    saveLoggedInUsersToFile();
                    showAlert(Alert.AlertType.INFORMATION, "Login Successful", "Welcome, " + email + "!");
                    openDriverDashboard(primaryStage);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid email or password for driver.");
                }
            } else {
                Document user = MongoDBUtil.findUser(email);
                if (user != null && user.getString("password").equals(password)) {
                    String storedRole = user.getString("role");
                    if (!storedRole.equals(role)) {
                        showAlert(Alert.AlertType.ERROR, "Login Failed",
                                "You are not authorized to log in as a " + role + ". Your role is " + storedRole + ".");
                        return;
                    }
                    String loginKey = email;
                    loggedInUsers.put(loginKey, role);
                    saveLoggedInUsersToFile();
                    showAlert(Alert.AlertType.INFORMATION, "Login Successful", "Welcome, " + email + "!");
                    switch (role) {
                        case "General User":
                            openUserForm(primaryStage);
                            break;
                        case "Administrator":
                            openAdminDashboard(primaryStage);
                            break;
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid email or password.");
                }
            }
        });

        signupButton.setOnAction(e -> {
            String email = emailField.getText().trim();
            String password = passwordField.getText();
            String driverNumber = role.equals("Driver") ? driverNumberCombo.getValue() : null;

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                showAlert(Alert.AlertType.ERROR, "Invalid Email", "Please enter a valid email address.");
                return;
            }
            if (!PASSWORD_PATTERN.matcher(password).matches()) {
                showAlert(Alert.AlertType.ERROR, "Invalid Password",
                        "Password must be at least 6 characters, contain 1 uppercase letter and 1 symbol.");
                return;
            }
            if (role.equals("Driver")) {
                driverNumberLabel.setVisible(true);
                driverNumberCombo.setVisible(true);
                if (driverNumber == null) {
                    showAlert(Alert.AlertType.ERROR, "Driver Number Required", "Please select a driver number.");
                    return;
                }
            }

            if (MongoDBUtil.findUser(email) != null) {
                showAlert(Alert.AlertType.WARNING, "Signup Failed", "Email already exists.");
                return;
            }

            if (role.equals("Driver")) {
                Document existingDriver = MongoDBUtil.findUserByDriverNumber(driverNumber);
                if (existingDriver != null) {
                    showAlert(Alert.AlertType.WARNING, "Signup Failed",
                            "Driver number " + driverNumber + " is already assigned to another account.");
                    return;
                }
            }

            MongoDBUtil.insertUser(email, password, role, driverNumber);
            showAlert(Alert.AlertType.INFORMATION, "Signup Successful", "Account created for " + email + ". Please log in.");
            openLoginSignup(primaryStage, role);
        });

        forgotPasswordButton.setOnAction(e -> {
            Dialog<Void> forgotPasswordDialog = new Dialog<>();
            forgotPasswordDialog.setTitle("Forgot Password");
            forgotPasswordDialog.setHeaderText("Reset Your Password");

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20));

            TextField resetEmailField = new TextField();
            resetEmailField.setPromptText("Enter your email");
            ComboBox<String> resetDriverNumberCombo = new ComboBox<>();
            resetDriverNumberCombo.getItems().addAll(DRIVERS);
            resetDriverNumberCombo.setValue(DRIVERS[0]);
            resetDriverNumberCombo.setVisible(role.equals("Driver"));

            Button verifyButton = createEnhancedModernButton("Verify", "#4CAF50");

            grid.add(new Label("Email:"), 0, 0);
            grid.add(resetEmailField, 1, 0);
            if (role.equals("Driver")) {
                grid.add(new Label("Driver Number:"), 0, 1);
                grid.add(resetDriverNumberCombo, 1, 1);
                grid.add(verifyButton, 1, 2);
            } else {
                grid.add(verifyButton, 1, 1);
            }

            forgotPasswordDialog.getDialogPane().setContent(grid);
            forgotPasswordDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            verifyButton.setOnAction(event -> {
                String email = resetEmailField.getText().trim();
                String driverNumber = role.equals("Driver") ? resetDriverNumberCombo.getValue() : null;

                if (!EMAIL_PATTERN.matcher(email).matches()) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Email", "Please enter a valid email address.");
                    return;
                }

                Document user = MongoDBUtil.findUser(email);
                if (user == null) {
                    showAlert(Alert.AlertType.ERROR, "User Not Found", "No account exists with this email.");
                    return;
                }

                if (role.equals("Driver") && !user.getString("driverNumber").equals(driverNumber)) {
                    showAlert(Alert.AlertType.ERROR, "Verification Failed", "Driver number does not match.");
                    return;
                }

                if (!user.getString("role").equals(role)) {
                    showAlert(Alert.AlertType.ERROR, "Role Mismatch", "This email is not registered as a " + role + ".");
                    return;
                }

                Dialog<Void> resetPasswordDialog = new Dialog<>();
                resetPasswordDialog.setTitle("Reset Password");
                resetPasswordDialog.setHeaderText("Enter New Password");

                GridPane resetGrid = new GridPane();
                resetGrid.setHgap(10);
                resetGrid.setVgap(10);
                resetGrid.setPadding(new Insets(20));

                PasswordField newPasswordField = new PasswordField();
                newPasswordField.setPromptText("New Password");
                PasswordField confirmPasswordField = new PasswordField();
                confirmPasswordField.setPromptText("Confirm Password");

                Button updateButton = createEnhancedModernButton("Update Password", "#2ecc71");

                resetGrid.add(new Label("New Password:"), 0, 0);
                resetGrid.add(newPasswordField, 1, 0);
                resetGrid.add(new Label("Confirm Password:"), 0, 1);
                resetGrid.add(confirmPasswordField, 1, 1);
                resetGrid.add(updateButton, 1, 2);

                resetPasswordDialog.getDialogPane().setContent(resetGrid);
                resetPasswordDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

                updateButton.setOnAction(updateEvent -> {
                    String newPassword = newPasswordField.getText();
                    String confirmPassword = confirmPasswordField.getText();

                    if (!newPassword.equals(confirmPassword)) {
                        showAlert(Alert.AlertType.ERROR, "Password Mismatch", "Passwords do not match.");
                        return;
                    }

                    if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
                        showAlert(Alert.AlertType.ERROR, "Invalid Password",
                                "Password must be at least 6 characters, contain 1 uppercase letter and 1 symbol.");
                        return;
                    }

                    MongoDBUtil.updateUserPassword(email, newPassword);
                    showAlert(Alert.AlertType.INFORMATION, "Password Updated", "Your password has been successfully updated.");
                    resetPasswordDialog.close();
                    forgotPasswordDialog.close();
                });

                resetPasswordDialog.showAndWait();
            });

            forgotPasswordDialog.showAndWait();
        });

        backButton.setOnAction(e -> start(primaryStage));
    }

    private void loadIssuesIntoTextArea(TextArea textArea) {
        List<Document> issues = MongoDBUtil.getPendingIssues();
        if (issues.isEmpty()) {
            textArea.setText("No issues reported yet.");
        } else {
            StringBuilder issueText = new StringBuilder();
            for (Document issue : issues) {
                issueText.append("Name: ").append(issue.getString("name"))
                        .append("\nMobile: ").append(issue.getString("mobile"))
                        .append("\nProblem: ").append(issue.getString("problem"))
                        .append("\nLocation: ").append(issue.getString("location"))
                        .append("\n---\n");
            }
            textArea.setText(issueText.toString());
        }
    }

    private boolean sendSmsToUser(String mobileNumber, String userName) {
        try {
            if (mobileNumber == null || !mobileNumber.matches("\\d{10}")) {
                throw new IllegalArgumentException("Invalid mobile number format. Expected 10 digits.");
            }

            String formattedMobile = "+91" + mobileNumber.trim();
            System.out.println("Attempting to send SMS to: " + formattedMobile);

            String messageBody = "Hello, " + userName + "! Your issue has been resolved. Share your feedback here: https://cleanmitra.netlify.app/ ⭐";

            Message message = Message.creator(
                            new PhoneNumber(formattedMobile),
                            new PhoneNumber(TWILIO_PHONE_NUMBER),
                            messageBody)
                    .create();

            System.out.println("SMS sent successfully: " + message.getSid());
            return true;
        } catch (com.twilio.exception.ApiException e) {
            String errorMessage = "Failed to send SMS: " + e.getMessage() + " (Code: " + e.getCode() + ")";
            if (e.getCode() == 21610) {
                errorMessage += "\nNumber is on a blocklist or opted out.";
            } else if (e.getCode() == 21614) {
                errorMessage += "\nNumber is not a valid mobile number.";
            } else if (e.getCode() == 21211) {
                errorMessage += "\nInvalid 'To' phone number. Verify the number in Twilio.";
            }
            showAlert(Alert.AlertType.ERROR, "SMS Failed", errorMessage);
            e.printStackTrace();
            return false;
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.ERROR, "SMS Failed", "Invalid mobile number: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "SMS Failed", "Unexpected error sending SMS: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private Text createStyledText(String content, int fontSize, Color color) {
        Text text = new Text(content);
        text.setFont(Font.font("Arial", fontSize));
        text.setFill(color);
        return text;
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showProfilePopup(Stage primaryStage, String role) {
        String email = getLoggedInUserByRole(role);
        if (email != null) {
            String[] parts = email.split(":");
            String displayEmail = parts[0];
            String driverNumber = role.equals("Driver") && parts.length > 1 ? parts[1] : null;
            Alert profileAlert = new Alert(Alert.AlertType.INFORMATION);
            profileAlert.setTitle("Profile Details");
            profileAlert.setHeaderText("User Profile");
            profileAlert.setContentText("Email: " + displayEmail + "\nRole: " + role +
                    (driverNumber != null ? "\nDriver Number: " + driverNumber : ""));
            profileAlert.showAndWait();
        } else {
            showAlert(Alert.AlertType.WARNING, "Profile Unavailable", "No user logged in for this role.");
        }
    }

    private Button createEnhancedModernLogoutButton(Stage primaryStage, String role) {
        Button logoutButton = new Button("Logout");
        logoutButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        logoutButton.setStyle(
                "-fx-background-color: #e74c3c; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 10 20 10 20; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-radius: 8; " +
                        "-fx-cursor: hand;"
        );

        logoutButton.setOnMouseEntered(e -> logoutButton.setStyle(
                "-fx-background-color: #c0392b; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 10 20 10 20; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-radius: 8; " +
                        "-fx-cursor: hand;"
        ));
        logoutButton.setOnMouseExited(e -> logoutButton.setStyle(
                "-fx-background-color: #e74c3c; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 10 20 10 20; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-radius: 8; " +
                        "-fx-cursor: hand;"
        ));

        logoutButton.setOnAction(e -> {
            String email = getLoggedInUserByRole(role);
            if (email != null) {
                loggedInUsers.remove(email);
                saveLoggedInUsersToFile();
                showAlert(Alert.AlertType.INFORMATION, "Logged Out", "You have been successfully logged out.");
                openLoginSignup(primaryStage, role);
            }
        });
        return logoutButton;
    }

    @Override
    public void stop() throws Exception {
        MongoDBUtil.close();
        saveLoggedInUsersToFile();
        loggedInUsers.clear();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}