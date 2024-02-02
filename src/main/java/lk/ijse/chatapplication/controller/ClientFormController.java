package lk.ijse.chatapplication.controller;

import com.jfoenix.controls.JFXTextField;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import javafx.embed.swing.SwingFXUtils;
import lk.ijse.chatapplication.MyFile;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.util.Base64;
import java.awt.image.BufferedImage;


import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;

public class ClientFormController {
    @FXML
    public JFXTextField txtMessage;
    @FXML
    public ImageView imgSend;
    @FXML
    public ImageView imgfile;
    @FXML
    public ImageView imgEmoji;
    public Label lblUserName;

    @FXML
    private VBox vbox;

    @FXML
    private ImageView imgClose;

    @FXML
    private Pane emojiPane;

    private int maxWidth = 380;

    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;

    private OutputStream outputStream;
    private InputStream inputStream;
    private Thread receiveThread;
    private Socket socket;
    private double xOffset = 0;
    private double yOffset = 0;
    private boolean isEmojiPaneVisible = false;

    public void initialize() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            System.out.println("Connected to server.");

            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            System.out.println(vbox);
            Scene scene = vbox.getScene();
            System.out.println(scene);

            receiveThread = new Thread(() -> {
                byte[] buffer = new byte[1024];
                int bytesRead = 0;

                while (true) {
                    try {
                        if (!((bytesRead = inputStream.read(buffer)) != -1)) break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    String message = new String(buffer, 0, bytesRead);
                    System.out.println(message);

                    if (message.startsWith("**file:@")) {
                        System.out.println("recive");
                        receiveFile();
                    } else if (message.startsWith("**msg:@")) {
                        handleMessage(message.substring(7));
                    } else if (message.startsWith("**emoji:@")) {
                        handleEmoji(message.substring(9));
                    }

                }
            });

            receiveThread.start();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveFile() {

        try {

            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

            System.out.println(" try");
            // Read the size of the file name so know when to stop reading.
            int fileNameLength = dataInputStream.readInt();
            Thread.sleep(100);
            System.out.println("length " + fileNameLength);
            // If the file exists
            if (fileNameLength > 0) {
                // Byte array to hold name of file.
                byte[] fileNameBytes = new byte[fileNameLength];
                // Read from the input stream into the byte array.
                dataInputStream.readFully(fileNameBytes, 0, fileNameBytes.length);
                // Create the file name from the byte array.
                String fileName = new String(fileNameBytes);
                // Read how much data to expect for the actual content of the file.
                int fileContentLength = dataInputStream.readInt();
                System.out.println("file created");
                // If the file exists.
                if (fileContentLength > 0) {
                    // Array to hold the file data.
                    byte[] fileContentBytes = new byte[fileContentLength];
                    // Read from the input stream into the fileContentBytes array.
                    dataInputStream.readFully(fileContentBytes, 0, fileContentBytes.length);
                    //MyFile file = new MyFile(fileName, fileContentBytes, getFileExtension(fileName)));
                    File fileToDownload = new File(fileName);

                    try {
                        // Create a stream to write data to the file.
                        FileOutputStream fileOutputStream = new FileOutputStream(fileToDownload);
                        // Write the actual file data to the file.
                        fileOutputStream.write(fileContentBytes);
                        // Close the stream.
                        fileOutputStream.close();


                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                    if (getFileExtension(fileName).equals("jpg") || getFileExtension(fileName).equals("png")) {
                        ImageView imageView = new ImageView(new Image(fileToDownload.getAbsolutePath()));
                        imageView.setFitWidth(200);
                        imageView.setFitHeight(200);
                        imageView.setPreserveRatio(true);


                        Platform.runLater(() -> {
                            HBox hBox = new HBox();
                            hBox.setPadding(new Insets(5, 0, 0, 0));
                            hBox.setAlignment(Pos.CENTER_LEFT);
                            hBox.getChildren().add(imageView);
                            vbox.getChildren().add(hBox);
                        });
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getFileExtension(String fileName) {
        // Get the file type by using the last occurence of . (for example aboutMe.txt returns txt).
        // Will have issues with files like myFile.tar.gz.
        int i = fileName.lastIndexOf('.');
        // If there is an extension.
        if (i > 0) {
            // Set the extension to the extension of the filename.
            return fileName.substring(i + 1);
        } else {
            return "No extension found.";
        }
    }

    private void handleEmoji(String message) {
        try {
            Image image = new Image(imgEmoji.getImage().getUrl().replaceAll("(icons8-smile-64.png)", message + ".png"));
            ImageView imageView = createAndConfigureImageView(image);

            Platform.runLater(() -> {
                HBox hBox = new HBox();
                hBox.setPadding(new Insets(5, 0, 0, 0));
                hBox.setAlignment(Pos.CENTER_LEFT);
                hBox.getChildren().add(imageView);
                vbox.getChildren().add(hBox);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ImageView createAndConfigureImageView(Image image) {
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(45);
        imageView.setFitHeight(45);
        return imageView;
    }

    private void handleMessage(String msg) {
        TextFlow text = new TextFlow(new Text(msg));
        text.setTextAlignment(TextAlignment.LEFT);
        text.setMaxWidth(maxWidth);
        text.setPadding(new Insets(2, 5, 2, 5));

        text.getStyleClass().add("msg-box");

        Platform.runLater(() -> {
            HBox hBox = new HBox();
            hBox.setPadding(new Insets(5, 0, 0, 0));
            hBox.setAlignment(Pos.CENTER_LEFT);
            hBox.getChildren().add(text);
            vbox.getChildren().add(hBox);
        });

    }


    @FXML
    private void exitProgramAction(Event exitProgramEvent) {

        /*try {
            inputStream.close();
            outputStream.close();
            receiveThread.stop();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }*/
        ImageView btn = (ImageView) exitProgramEvent.getSource();
        Stage window = (Stage) btn.getScene().getWindow();
        window.close();
    }


    @FXML
    private void onMousePressed(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    private void onMouseDragged(MouseEvent event) {
        Pane pane = (Pane) event.getSource();
        Stage window = (Stage) pane.getScene().getWindow();
        window.setX(event.getScreenX() - xOffset);
        window.setY(event.getScreenY() - yOffset);
    }

    @FXML
    public void sendOnAction(Event actionEvent) {

        String message = txtMessage.getText();
        sendString("**msg:@" + message);

        Text text1 = new Text(message);
        text1.setFont(Font.font("NotoColorEmoji", 16));
        TextFlow text = new TextFlow(text1);
        text.setTextAlignment(TextAlignment.LEFT);
        text.setMaxWidth(maxWidth);

        text.setPadding(new Insets(2, 5, 2, 5));

        text.getStyleClass().add("msg-box");

        HBox hBox = new HBox();
        hBox.setPadding(new Insets(5, 0, 0, 0));
        hBox.setAlignment(Pos.CENTER_RIGHT);
        hBox.getChildren().add(text);
        vbox.getChildren().add(hBox);


    }

    @FXML
    public void mouseEntered(MouseEvent mouseEvent) {
        if (mouseEvent.getSource() instanceof ImageView) {
            ImageView icon = (ImageView) mouseEvent.getSource();

            ScaleTransition scaleT = new ScaleTransition(Duration.millis(200), icon);
            scaleT.setToX(1.2);
            scaleT.setToY(1.2);
            scaleT.play();

            DropShadow glow = new DropShadow();
//            glow.setColor(Color.valueOf("#EF233C"));
            glow.setColor(Color.CORNFLOWERBLUE);
            glow.setWidth(15);
            glow.setHeight(15);
            glow.setRadius(15);
            icon.setEffect(glow);
        }
    }

    @FXML
    public void mouseExited(MouseEvent mouseEvent) {
        if (mouseEvent.getSource() instanceof ImageView) {
            ImageView icon = (ImageView) mouseEvent.getSource();
            ScaleTransition scaleT = new ScaleTransition(Duration.millis(200), icon);
            scaleT.setToX(1);
            scaleT.setToY(1);
            scaleT.play();
            icon.setEffect(null);
        }
    }

    @FXML
    public void mouseClicked(MouseEvent mouseEvent) throws IOException {
        if (mouseEvent.getSource() instanceof ImageView) {
            ImageView icon = (ImageView) mouseEvent.getSource();
            switch (icon.getId()) {
                case "imgfile":
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Select File");
                    fileChooser.setDialogTitle("Choose a file to send.");
                    File selectedFile = null;

                    if (fileChooser.showOpenDialog(null)  == JFileChooser.APPROVE_OPTION) {
                       selectedFile  = fileChooser.getSelectedFile();
                       sendFile(selectedFile);
                    }

                    if (selectedFile != null) {
                        sendFile(selectedFile);
                    }
                    break;
                case "imgEmoji":
                    emojiPane.setVisible(!isEmojiPaneVisible);
                    isEmojiPaneVisible = !isEmojiPaneVisible;
                    break;

            }
        }

    }

    private void sendFile(File selectedFile) {
        if (selectedFile == null) {
            new Alert(Alert.AlertType.ERROR, "Please choose a file to send first!").show();
            // If a file has been selected then do the following.
        } else {
            try {
                // Create an input stream into the file you want to send.
                FileInputStream fileInputStream = new FileInputStream(selectedFile.getAbsolutePath());
                // Create a socket connection to connect with the server.
                // Create an output stream to write to write to the server over the socket connection.
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                // Get the name of the file you want to send and store it in filename.
                String fileName = selectedFile.getName();
                // Convert the name of the file into an array of bytes to be sent to the server.
                byte[] fileNameBytes = fileName.getBytes();
                // Create a byte array the size of the file so don't send too little or too much data to the server.
                byte[] fileBytes = new byte[(int) selectedFile.length()];
                // Put the contents of the file into the array of bytes to be sent so these bytes can be sent to the server.
                fileInputStream.read(fileBytes);

                sendString("**file:@");


                // Send the length of the name of the file so server knows when to stop reading.
                dataOutputStream.writeInt(fileNameBytes.length);
                // Send the file name.
                dataOutputStream.write(fileNameBytes);
                // Send the length of the byte array so the server knows when to stop reading.
                dataOutputStream.writeInt(fileBytes.length);
                // Send the actual file.
                dataOutputStream.write(fileBytes);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void sendString(String msg) {
        try {
            outputStream.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void emojiClicked(MouseEvent event) throws IOException {
        ImageView emoji = (ImageView) event.getSource();
        sendString("**emoji:@" + emoji.getId());
        System.out.println(emoji.getImage().getUrl());

        ImageView imageView = createAndConfigureImageView(emoji.getImage());

        HBox hBox = new HBox();
        hBox.setPadding(new Insets(5, 0, 0, 0));
        hBox.setAlignment(Pos.CENTER_RIGHT);
        hBox.getChildren().add(imageView);
        vbox.getChildren().add(hBox);
        emojiPane.setVisible(false);
    }
}
