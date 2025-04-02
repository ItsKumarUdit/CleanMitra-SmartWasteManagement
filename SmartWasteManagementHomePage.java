package com.example.javafx;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
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
    private static final HashMap<String, String> loggedInUsers = new HashMap<>();
    private static final String PROFILE_FILE = "user_profile.txt";
    private static final String[] DRIVERS = {"Driver 1", "Driver 2", "Driver 3", "Driver 4"};

    // Twilio credentials
    private static final String TWILIO_ACCOUNT_SID = "AC***************************59";
    private static final String TWILIO_AUTH_TOKEN = "aa***************************39";
    private static final String TWILIO_PHONE_NUMBER = "+15*******77";

    // Initialize Twilio
    static {
        Twilio.init(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN);
    }

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

        Text title = new Text("Smart Waste Management System");
        title.setFont(Font.font("Arial", 28));
        title.setFill(Color.DARKBLUE);

        Button adminButton = createStyledButton("Administrator", "#4CAF50");
        Button driverButton = createStyledButton("Driver", "#2196F3");
        Button userButton = createStyledButton("General User", "#FF5722");

        VBox buttonLayout = new VBox(20, adminButton, driverButton, userButton);
        buttonLayout.setAlignment(Pos.CENTER);

        Text footer = new Text("© 2025 Smart Waste Management System. All rights reserved.");
        footer.setFont(Font.font("Arial", 12));
        footer.setFill(Color.GRAY);

        VBox mainLayout = new VBox(30, title, buttonLayout, footer);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setStyle("-fx-padding: 30; -fx-background-color: linear-gradient(to bottom, #ffffff, #e0e0e0);");

        Scene scene = new Scene(mainLayout, 500, 400);
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

        Button verifyButton = createStyledButton("Verify", "#4CAF50");
        Button backButton = createStyledButton("Back", "#FF5722");

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
        driverNumberCombo.setVisible(role.equals("Driver"));
        driverNumberCombo.setValue(DRIVERS[0]);

        Button loginButton = createStyledButton("Login", "#4CAF50");
        Button signupButton = createStyledButton("Signup", "#2196F3");
        Button backButton = createStyledButton("Back to Home", "#FF5722");

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
            loginLayout.add(backButton, 0, 5, 2, 1);
        } else {
            loginLayout.add(loginButton, 0, 3);
            loginLayout.add(signupButton, 1, 3);
            loginLayout.add(backButton, 0, 4, 2, 1);
        }

        Scene loginScene = new Scene(loginLayout, 400, role.equals("Driver") ? 350 : 300);
        primaryStage.setScene(loginScene);

        loginButton.setOnAction(e -> {
            String email = emailField.getText().trim();
            String password = passwordField.getText();
            String driverNumber = role.equals("Driver") ? driverNumberCombo.getValue() : null;

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                showAlert(Alert.AlertType.ERROR, "Invalid Email", "Please enter a valid email address.");
                return;
            }

            Document user = MongoDBUtil.findUser(email);
            if (user != null && user.getString("password").equals(password)) {
                String storedRole = user.getString("role");
                String storedDriverNumber = user.getString("driverNumber");

                if (!storedRole.equals(role)) {
                    showAlert(Alert.AlertType.ERROR, "Login Failed",
                            "You are not authorized to log in as a " + role + ". Your role is " + storedRole + ".");
                    return;
                }

                if (role.equals("Driver") && !storedDriverNumber.equals(driverNumber)) {
                    showAlert(Alert.AlertType.ERROR, "Login Failed",
                            "Incorrect driver number selected.");
                    return;
                }

                String loginKey = email + (role.equals("Driver") ? ":" + driverNumber : "");
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
                    case "Driver":
                        openDriverDashboard(primaryStage);
                        break;
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid email or password.");
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
            if (role.equals("Driver") && driverNumber == null) {
                showAlert(Alert.AlertType.ERROR, "Driver Number Required", "Please select a driver number.");
                return;
            }
            if (MongoDBUtil.findUser(email) != null) {
                showAlert(Alert.AlertType.WARNING, "Signup Failed", "Email already exists.");
            } else {
                MongoDBUtil.insertUser(email, password, role, driverNumber);
                showAlert(Alert.AlertType.INFORMATION, "Signup Successful", "Account created for " + email + ". Please log in.");
                openLoginSignup(primaryStage, role); // Redirect to login instead of auto-login
            }
        });

        backButton.setOnAction(e -> start(primaryStage));
    }

    private void openUserForm(Stage primaryStage) {
        Text formTitle = createStyledText("User Issue Submission", 22, Color.DARKBLUE);

        ImageView profileIcon = createProfileIcon(primaryStage, "General User");

        BorderPane titlePane = new BorderPane();
        titlePane.setCenter(formTitle);
        titlePane.setRight(profileIcon);
        BorderPane.setAlignment(formTitle, Pos.CENTER);
        BorderPane.setAlignment(profileIcon, Pos.CENTER_RIGHT);

        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField();

        Label mobileLabel = new Label("Mobile:");
        TextField mobileField = new TextField();

        Label problemLabel = new Label("Problem:");
        TextArea problemArea = new TextArea();
        problemArea.setPrefRowCount(3);

        Label locationLabel = new Label("Location:");
        TextField locationField = new TextField();

        Label photoLabel = new Label("Upload Photo (Optional):");
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
        Button logoutButton = createModernLogoutButton(primaryStage, "General User");

        GridPane formLayout = new GridPane();
        formLayout.setHgap(15);
        formLayout.setVgap(15);
        formLayout.setAlignment(Pos.CENTER);
        formLayout.setStyle("-fx-padding: 30; -fx-background-color: #f8f9fa; -fx-border-color: #cccccc; -fx-border-width: 1px;");

        formLayout.add(titlePane, 0, 0, 2, 1);
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
        formLayout.add(logoutButton, 0, 8, 2, 1);

        Scene userFormScene = new Scene(formLayout, 500, 500);
        primaryStage.setScene(userFormScene);

        submitButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String mobile = mobileField.getText().trim();
            String problem = problemArea.getText().trim();
            String location = locationField.getText().trim();

            if (name.isEmpty() || mobile.isEmpty() || problem.isEmpty() || location.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Form Incomplete", "Please fill all required fields.");
                return;
            }

            if (!mobile.matches("\\d{10}")) {
                showAlert(Alert.AlertType.ERROR, "Invalid Mobile Number", "Please enter a valid 10-digit mobile number.");
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
            start(primaryStage);
        });

        backToHomeButton.setOnAction(e -> start(primaryStage));
    }

    private void openAdminDashboard(Stage primaryStage) {
        Text dashboardTitle = createStyledText("Administrator Dashboard", 22, Color.DARKBLUE);

        ImageView profileIcon = createProfileIcon(primaryStage, "Administrator");

        BorderPane titlePane = new BorderPane();
        titlePane.setCenter(dashboardTitle);
        titlePane.setRight(profileIcon);
        BorderPane.setAlignment(dashboardTitle, Pos.CENTER);
        BorderPane.setAlignment(profileIcon, Pos.CENTER_RIGHT);

        TextArea issueDisplayArea = new TextArea();
        issueDisplayArea.setEditable(false);
        issueDisplayArea.setPrefRowCount(10);
        issueDisplayArea.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px; -fx-padding: 10;");

        loadIssuesIntoTextArea(issueDisplayArea);

        Button viewPhotoButton = createStyledButton("View Uploaded Photo", "#2196F3");
        Button assignTaskButton = createStyledButton("Assign Task to Driver", "#4CAF50");
        Button clearIssuesButton = createStyledButton("Clear Issues", "#F44336");
        Button backButton = createStyledButton("Back to Home", "#FF5722");
        Button logoutButton = createModernLogoutButton(primaryStage, "Administrator");

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

        HBox topButtons = new HBox(10, viewPhotoButton, assignTaskButton);
        topButtons.setAlignment(Pos.CENTER);

        HBox bottomButtons = new HBox(10, clearIssuesButton, backButton);
        bottomButtons.setAlignment(Pos.CENTER);

        VBox dashboardLayout = new VBox(20, titlePane, issueDisplayArea, topButtons, bottomButtons, logoutButton);
        dashboardLayout.setAlignment(Pos.CENTER);
        dashboardLayout.setStyle("-fx-padding: 30; -fx-background-color: #f8f9fa;");

        Scene adminScene = new Scene(dashboardLayout, 600, 500);
        primaryStage.setScene(adminScene);
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

    private void openDriverDashboard(Stage primaryStage) {
        Text dashboardTitle = createStyledText("Driver Dashboard", 22, Color.DARKBLUE);

        ImageView profileIcon = createProfileIcon(primaryStage, "Driver");

        BorderPane titlePane = new BorderPane();
        titlePane.setCenter(dashboardTitle);
        titlePane.setRight(profileIcon);
        BorderPane.setAlignment(dashboardTitle, Pos.CENTER);
        BorderPane.setAlignment(profileIcon, Pos.CENTER_RIGHT);

        TextArea taskDisplayArea = new TextArea();
        taskDisplayArea.setEditable(false);
        taskDisplayArea.setPrefRowCount(10);
        taskDisplayArea.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px; -fx-padding: 10;");

        String loggedInUser = getLoggedInUserByRole("Driver");
        String driverNumber = getDriverNumberFromLoggedInUser(loggedInUser);

        if (driverNumber != null && assignedTasks.containsKey(driverNumber)) {
            taskDisplayArea.setText("Assigned Task:\n" + assignedTasks.get(driverNumber).taskText);
        } else {
            taskDisplayArea.setText("No tasks assigned yet.");
        }

        Button viewPhotoButton = createStyledButton("View Uploaded Photo", "#2196F3");
        Button problemSolvedButton = createStyledButton("Problem Solved", "#4CAF50");
        Button uploadProofButton = createStyledButton("Upload Proof", "#2196F3");
        Button backButton = createStyledButton("Back to Home", "#FF5722");
        Button locationTrackButton = createStyledButton("Location Track", "#FF9800");
        Button logoutButton = createModernLogoutButton(primaryStage, "Driver");

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

        HBox taskButtons = new HBox(10, viewPhotoButton, problemSolvedButton);
        taskButtons.setAlignment(Pos.CENTER);

        HBox actionButtons = new HBox(10, uploadProofButton, locationTrackButton);
        actionButtons.setAlignment(Pos.CENTER);

        VBox driverLayout = new VBox(20, titlePane, taskDisplayArea, taskButtons, actionButtons, backButton, logoutButton);
        driverLayout.setAlignment(Pos.CENTER);
        driverLayout.setStyle("-fx-padding: 30; -fx-background-color: #f8f9fa;");

        Scene driverScene = new Scene(driverLayout, 600, 500);
        primaryStage.setScene(driverScene);
    }

    private boolean sendSmsToUser(String mobileNumber, String userName) {
        try {
            if (mobileNumber == null || !mobileNumber.matches("\\d{10}")) {
                throw new IllegalArgumentException("Invalid mobile number format. Expected 10 digits.");
            }

            String formattedMobile = "+91" + mobileNumber.trim();
            System.out.println("Attempting to send SMS to: " + formattedMobile);

            String messageBody = "Hello " + userName + ", your waste management issue has been resolved by our team. " +
                    "Thank you for using Smart Waste Management System!";

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

    private Button createModernLogoutButton(Stage primaryStage, String role) {
        Button logoutButton = new Button("Logout");
        logoutButton.setStyle("-fx-font-size: 14px; -fx-background-color: linear-gradient(to right, #ff416c, #ff4b2b); " +
                "-fx-text-fill: white; -fx-padding: 10; -fx-background-radius: 20; -fx-border-radius: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);");
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
