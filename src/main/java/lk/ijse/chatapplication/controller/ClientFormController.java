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

import javax.imageio.ImageIO;
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
                        receiveAttachment(getExtension(message.substring(8)));
                    } else if (message.startsWith("**msg:@")) {
                        handleMessage(message.substring(7));
                    } else if (message.startsWith("**emoji:@")) {
                        handleEmoji(message.substring(9));
                    }


                    Platform.runLater(() -> {
//                        TextFlow text = new TextFlow(new Text(s));
//                        text.setTextAlignment(TextAlignment.LEFT);
//                        text.setMaxWidth(maxWidth);
//                        text.setPadding(new Insets(2, 5, 2, 5));
//
//                        text.getStyleClass().add("msg-box");
//
//                        HBox hBox = new HBox();
//                        hBox.setPadding(new Insets(5, 0, 0, 0));
//                        hBox.setAlignment(Pos.CENTER_LEFT);
//                        hBox.getChildren().add(text);
//                        vbox.getChildren().add(hBox);
                    });

                }
            });

            receiveThread.start();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleEmoji(String message) {
        try {
            Image image = new Image(imgEmoji.getImage().getUrl().replaceAll("(icons8-smile-64.png)", message+".png"));
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

        HBox hBox = new HBox();
        hBox.setPadding(new Insets(5, 0, 0, 0));
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.getChildren().add(text);
        vbox.getChildren().add(hBox);
    }

    private String getExtension(String message) {
        int index = message.indexOf("file:@");
        if (index != -1) {
            return message.substring(index + 6).trim(); // Skip "file:@" and the colon, trim spaces
        } else {
            return ""; // No match found
        }
    }

    private void receiveAttachment(String extension) {
        System.out.println("from receive");
        try (InputStream inputStream = socket.getInputStream();
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream("./lk/ijse/chatapplication/assets/font/" + LocalDate.now() + LocalTime.now() + "." + extension))) {

            byte[] buffer = new byte[1024];
            int bytesRead = 0;
            System.out.println("waiting");
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                bufferedOutputStream.write(buffer, 0, bytesRead);
            }
            System.out.println("attachment received from server.");

        } catch (IOException e) {
            e.printStackTrace();
        }
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
        sendString(message);

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
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Select File");
                    //fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"));
                    File selectedFile = fileChooser.showOpenDialog(null);
                    System.out.println(selectedFile.getPath());
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(selectedFile.getPath()));

                    if (selectedFile != null) {
                        String extension = "";
                        int dotIndex = selectedFile.getName().lastIndexOf('.');
                        if (dotIndex >= 0) {
                            extension = selectedFile.getName().substring(dotIndex + 1).toLowerCase();
                        }

                        System.out.println("Selected file extension: " + extension);

                        String message = "**file:@" + extension;
                        outputStream.write(message.getBytes());
                        byte[] buffer = new byte[1024];
                        int bytesRead;

                        while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }

                        System.out.println("attachment sent to client.");
                        // Proceed with file operations based on the extension
                    }

                    break;
                case "imgEmoji":
                    emojiPane.setVisible(!isEmojiPaneVisible);
                    isEmojiPaneVisible = !isEmojiPaneVisible;
                    break;

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

    /*private String convertImageToString(Image image) throws IOException {
        double maxWidth = 600;
        double maxHeight = 400;
        double width = image.getWidth();
        double height = image.getHeight();

        if (width > maxWidth || height > maxHeight) {
            double scaleFactor = Math.min(maxWidth / width, maxHeight / height);
            width *= scaleFactor;
            height *= scaleFactor;
        }

        BufferedImage resizedImage = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(SwingFXUtils.fromFXImage(image, null), 0, 0, (int) width, (int) height, null);
        g.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "png", outputStream); // Use "png" instead of "jpg"
        byte[] imageBytes = outputStream.toByteArray();

        return Base64.getEncoder().encodeToString(imageBytes);
    }

    private Image convertStringToImage(String imageAsString) throws IOException {
        byte[] imageBytes = Base64.getDecoder().decode(imageAsString);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
        inputStream.reset();  // Reset the position to the beginning

        BufferedImage bufferedImage=null;
        try {
           bufferedImage = ImageIO.read(inputStream);
            // Rest of the code
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception, log details, or show an error message
        }

        if (bufferedImage == null) {
            throw new IOException("Unable to decode the image.");
        }

        return SwingFXUtils.toFXImage(bufferedImage, null);
    }*/

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
