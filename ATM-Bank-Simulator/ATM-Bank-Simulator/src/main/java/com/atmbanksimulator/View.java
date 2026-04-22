package com.atmbanksimulator;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

// ===== 🙂 View (Eyes / Ears / Nose / Mouth / Face) =====

// The View class creates the GUI for the application.
// It does not  about business logic;
// it only updates the display when notified by the UIModel.
class View {
    private static final String PAGE_WELCOME = "welcome";
    private static final String PAGE_GOODBYE = "goodbye";
    private static final float SAMPLE_RATE = 22050f;

    int H = 500;         // Height of window pixels
    int W = 500;         // Width  of window pixels

    Controller controller; // Reference to the Controller (part of the MVC setup)

    // Components (controls and layout) of the user interface
    private Label laMsg;        // Message label, e.g. shows "Welcome to ATM" at startup (not the window title)
    private TextField tfInput;  // Input field where numbers typed on the keypad appear
    private TextArea taResult;  // Output area where instructions and results are displayed
    private ScrollPane scrollPane; // Provides scrollbars around the TextArea
    private GridPane grid;      // Main ATM layout container (grid-based)
    private GridPane buttonPane;// Container for ATM keypad buttons
    private BorderPane welcomePane;
    private StackPane rootPane;
    private HBox welcomeHeader;
    private Label welcomeBrand;
    private Label welcomeTitle;
    private Label welcomeSubtitle;
    private Label welcomeStatus;
    private Label welcomeActions;
    private VBox welcomeCenterBox;
    private Button startButton;
    private Button createButton;
    private Button exitButton;
    private Button returnButton;
    private Stage mainWindow;

    // start() is called from Main to set up the UI.
    // Important: create controls here (not in the constructor or as field initializers),
    // so that everything is initialized in the correct order.
    public void start(Stage window) {
        mainWindow = window;
        rootPane = new StackPane();
        rootPane.setPadding(new Insets(18));

        buildWelcomePane();
        buildAtmPane();

        rootPane.getChildren().addAll(welcomePane, grid);

        // add the complete GUI to the window and display it
        Scene scene = new Scene(rootPane, W, H);
        scene.getStylesheets().add("atm.css"); // tell to use our css file
        window.setScene(scene);
        window.setTitle("ATM-Bank Simulator"); //set window title
        window.show();
    }

    private void buildAtmPane() {
        grid = new GridPane(); // top layout
        grid.setId("Layout");  // assign an id to be used in css file
        buttonPane = new GridPane();
        buttonPane.setId("Buttons"); // assign an id to be used in css file

        Button backButton = new Button("←");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(event -> controller.process("Back"));

        laMsg = new Label("Welcome to Bank-ATM");  // Message bar at the top
        laMsg.getStyleClass().add("header-label");

        HBox headerRow = new HBox(12, backButton, laMsg);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        grid.add(headerRow, 0, 0);         // Add to GUI at the top

        tfInput = new TextField();     // text field for numbers
        tfInput.setEditable(false);     // Read only (user can't type in)
        grid.add(tfInput, 0, 1);    // Add to GUI on second row

        taResult = new TextArea();         // text area for instructions, transaction results
        taResult.setEditable(false);       // Read only
        taResult.setWrapText(true);
        scrollPane  = new ScrollPane();    // create a scrolling window
        scrollPane.setContent(taResult);   // put the text area 'inside' the scrolling window
        scrollPane.setFitToWidth(true);
        grid.add( scrollPane, 0, 2);    // add the scrolling window to GUI on third row

        // Define the button layout as a 2D array of text labels.
        // Empty strings ("") represent blank spaces in the grid.
        String buttonTexts[][] = {
                {"7", "8", "9", "Pwd", "Dep", "Trf"},
                {"4", "5", "6", "W/D", "Bal", "Fin"},
                {"1", "2", "3", "CLR", "0", "Ent"},
                {"Mini", "", "", "", "", ""} };

        for (int row = 0; row < buttonTexts.length; row++) {
            for (int col = 0; col < buttonTexts[row].length; col++) {
                String text = buttonTexts[row][col];
                if (text.length() >= 1) {
                    Button btn = new Button( text );
                    btn.setOnAction( this::buttonClicked );
                    if (text.equals("Ent")) {
                        btn.getStyleClass().add("btn-primary");
                    } else if (text.equals("Fin")) {
                        btn.getStyleClass().add("btn-danger");
                    }
                    buttonPane.add(btn, col, row);
                } else {
                    buttonPane.add(new Text(), col, row);
                }
            }
        }
        grid.add(buttonPane,0,3); // add the tiled pane of buttons to the main grid
    }

    private void buildWelcomePane() {
        welcomePane = new BorderPane();
        welcomePane.getStyleClass().add("welcome-pane");
        welcomePane.setPrefSize(560, 650);

        welcomeBrand = new Label("ATM Bank Simulator");
        welcomeBrand.getStyleClass().add("welcome-brand");
        welcomeHeader = new HBox(welcomeBrand);
        welcomeHeader.getStyleClass().add("welcome-header");
        welcomeHeader.setAlignment(Pos.CENTER_LEFT);

        Label welcomeIcon = new Label("ATM");
        welcomeIcon.getStyleClass().add("welcome-icon");

        welcomeTitle = new Label("Welcome!");
        welcomeTitle.getStyleClass().add("welcome-title");
        welcomeTitle.setWrapText(true);

        welcomeSubtitle = new Label("Your secure banking experience.\nWhat would you like to do today?");
        welcomeSubtitle.getStyleClass().add("welcome-subtitle");
        welcomeSubtitle.setWrapText(true);

        welcomeStatus = new Label("Welcome to the ATM");
        welcomeStatus.getStyleClass().add("welcome-status");
        welcomeStatus.setWrapText(true);

        welcomeActions = new Label("Shortcuts: Ent = Start ATM, New = Create Account, Fin = Exit");
        welcomeActions.getStyleClass().add("welcome-actions");
        welcomeActions.setWrapText(true);

        startButton = new Button("Start Login");
        startButton.getStyleClass().add("welcome-shortcut");
        startButton.getStyleClass().add("btn-primary");
        startButton.setOnAction(event -> controller.process("Ent"));
        startButton.setPrefWidth(326);

        createButton = new Button("Create Account");
        createButton.getStyleClass().add("welcome-shortcut");
        createButton.getStyleClass().add("btn-success");
        createButton.setOnAction(event -> controller.process("New"));
        createButton.setPrefWidth(326);

        exitButton = new Button("Exit");
        exitButton.getStyleClass().add("welcome-shortcut");
        exitButton.getStyleClass().add("welcome-outline");
        exitButton.setOnAction(event -> controller.process("Fin"));
        exitButton.setPrefWidth(326);

        returnButton = new Button("← Return to welcome screen");
        returnButton.getStyleClass().add("welcome-link");
        returnButton.setOnAction(event -> controller.process("Ent"));

        VBox actionButtons = new VBox(18, startButton, createButton, exitButton);
        actionButtons.setAlignment(Pos.CENTER);

        welcomeCenterBox = new VBox(18, welcomeIcon, welcomeTitle, welcomeSubtitle, welcomeStatus, welcomeActions, actionButtons, returnButton);
        welcomeCenterBox.getStyleClass().add("welcome-center");
        welcomeCenterBox.setAlignment(Pos.CENTER);

        welcomePane.setTop(welcomeHeader);
        welcomePane.setCenter(welcomeCenterBox);
        BorderPane.setAlignment(welcomeCenterBox, Pos.CENTER);
        BorderPane.setMargin(welcomeCenterBox, new Insets(24, 36, 36, 36));
    }

    // This is how the View talks to the Controller
    // This method is called when a button is pressed
    // It fetches the label on the button and passes it to the controller's process method
    private void buttonClicked(ActionEvent event) {
        // this line asks the event to provide the actual Button object that was clicked
        Button b = ((Button) event.getSource());
        String text = b.getText();   // get the button label
        playButtonPressSound();
        System.out.println( "View::buttonClicked: label = "+ text );
        controller.process( text );  // Pass it to the controller's process method
    }

    public void playSuccessSound() {
        playTonePattern(new int[] {880, 1175}, new int[] {110, 150});
    }

    public void playErrorSound() {
        playTonePattern(new int[] {320, 240}, new int[] {180, 220});
    }

    public void showMiniStatementWindow(String title, String content) {
        Stage statementWindow = new Stage();
        statementWindow.initOwner(mainWindow);
        statementWindow.initModality(Modality.NONE);
        statementWindow.setTitle(title);

        Label heading = new Label(title);
        heading.getStyleClass().add("header-label");

        TextArea statementArea = new TextArea(content);
        statementArea.setEditable(true);
        statementArea.setWrapText(true);

        Button closeButton = new Button("Close");
        closeButton.getStyleClass().add("btn-primary");
        closeButton.setOnAction(event -> statementWindow.close());

        VBox layout = new VBox(12, heading, statementArea, closeButton);
        layout.setPadding(new Insets(16));
        layout.setPrefSize(430, 360);
        layout.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(statementArea, Priority.ALWAYS);

        Scene scene = new Scene(layout, 430, 360);
        scene.getStylesheets().add("atm.css");
        statementWindow.setScene(scene);
        statementWindow.show();
    }

    private void playButtonPressSound() {
        playTonePattern(new int[] {660}, new int[] {70});
    }

    private void playTonePattern(int[] frequencies, int[] durationsMs) {
        Thread soundThread = new Thread(() -> {
            for (int i = 0; i < frequencies.length; i++) {
                playTone(frequencies[i], durationsMs[i], 0.25);
                if (i < frequencies.length - 1) {
                    try {
                        Thread.sleep(45);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        });
        soundThread.setDaemon(true);
        soundThread.start();
    }

    private void playTone(int frequency, int durationMs, double volume) {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
        try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format);
            line.start();

            byte[] buffer = new byte[1];
            int totalSamples = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
            for (int i = 0; i < totalSamples; i++) {
                double angle = 2.0 * Math.PI * i * frequency / SAMPLE_RATE;
                buffer[0] = (byte) (Math.sin(angle) * 127.0 * volume);
                line.write(buffer, 0, 1);
            }

            line.drain();
        } catch (LineUnavailableException ignored) {
            // Leave the UI responsive if audio is unavailable.
        }
    }

    // This method is called by the UIModel whenever the UIModel changes.
    // It receives updated information from the UIModel and displays them in the GUI.
    // - msg → shown in the top message label
    // - tfInputMsg → shown in the text field (user input area)
    // - taResultMsg → shown in the text area (instructions / results)
    public void update(String msg,String tfInputMsg,String taResultMsg, String pageMode)
    {
        boolean showLandingPage = PAGE_WELCOME.equals(pageMode) || PAGE_GOODBYE.equals(pageMode);
        boolean showGoodbyePage = PAGE_GOODBYE.equals(pageMode);

        welcomePane.setVisible(showLandingPage);
        welcomePane.setManaged(showLandingPage);
        grid.setVisible(!showLandingPage);
        grid.setManaged(!showLandingPage);

        if (showLandingPage) {
            if (showGoodbyePage) {
                welcomeTitle.setText("Goodbye!");
                welcomeSubtitle.setText("Thank you for using our ATM\nsimulator. Have a wonderful day!");
                welcomeStatus.setText(msg);
                welcomeActions.setText("");
                startButton.setVisible(false);
                startButton.setManaged(false);
                createButton.setVisible(false);
                createButton.setManaged(false);
                exitButton.setVisible(false);
                exitButton.setManaged(false);
                returnButton.setVisible(true);
                returnButton.setManaged(true);
            } else {
                welcomeTitle.setText("Welcome!");
                welcomeSubtitle.setText("Your secure banking experience.\nWhat would you like to do today?");
                welcomeStatus.setText(msg);
                welcomeActions.setText(taResultMsg);
                startButton.setVisible(true);
                startButton.setManaged(true);
                createButton.setVisible(true);
                createButton.setManaged(true);
                exitButton.setVisible(true);
                exitButton.setManaged(true);
                returnButton.setVisible(false);
                returnButton.setManaged(false);
            }
        }

        laMsg.setText(msg);
        tfInput.setText(tfInputMsg);
        taResult.setText(taResultMsg);
    }
}
