package lk.ijse.chatapplication.controller;

import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lk.ijse.chatapplication.AppInitializer;
import lk.ijse.chatapplication.MyFile;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import static lk.ijse.chatapplication.controller.ClientFormController.getFileExtension;

public class SeverFormController {
    @FXML
    public Label lblCount;
    @FXML
    public TextArea txtDetail;
    @FXML
    public Label lblUptime;
    @FXML
    public JFXTextField txtClientName;
    @FXML
    public Button btnNewClient;
    private static final int PORT = 12345;
    private static Set<Socket> clientSockets = new HashSet<>();

    public void initialize() {
        txtClientName.setVisible(false);
        lblUptime.setText("00:00:00");
        lblCount.setText("00");
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                Platform.runLater(() ->
                        txtDetail.appendText("Server started on port " + PORT + "\n"));
                System.out.println("Server started on port " + PORT);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    Platform.runLater(() -> txtDetail.appendText("New client connected: " + clientSocket.getPort() + "\n"));
                    System.out.println("New client connected: " + clientSocket.getPort());

                    clientSockets.add(clientSocket);
                    Platform.runLater(() -> updateClientCount());
                    Thread clientHandlerThread = new Thread(() -> handleClient(clientSocket));
                    clientHandlerThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void handleClient(Socket clientSocket) {
        try (InputStream inputStream = clientSocket.getInputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;

            //wait for client massage
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                String message = new String(buffer, 0, bytesRead);
                System.out.println("Received from " + clientSocket.getPort() + ": " + message);
                if (message.equals("exit")) {
                    message = "User disconnected";
                    break;
                } else if (message.startsWith("**file:@")) {
                    handleFileTransfer(message, clientSocket);
                } else {
                    // Broadcast the message to all connected clients
                    broadcast(message, clientSocket);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Client disconnected: " + clientSocket.getInetAddress());
            clientSockets.remove(clientSocket);
        }
    }

    private static void handleFileTransfer(String startMessage, Socket sender) {
        try {
            DataInputStream dataInputStream = new DataInputStream(sender.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(sender.getOutputStream());


            int fileNameLength = dataInputStream.readInt();
            if (fileNameLength > 0) {
                byte[] fileNameBytes = new byte[fileNameLength];
                dataInputStream.readFully(fileNameBytes, 0, fileNameBytes.length);
                String fileName = new String(fileNameBytes);
                int fileContentLength = dataInputStream.readInt();
                if (fileContentLength > 0) {
                    byte[] fileContentBytes = new byte[fileContentLength];
                    dataInputStream.readFully(fileContentBytes, 0, fileContentBytes.length);
                    MyFile file = new MyFile(fileName, fileContentBytes, getFileExtension(fileName));

                    byte[] sendfileNameBytes = file.getName().getBytes();

                    for (Socket clientSocket : clientSockets) {
                        if (!clientSocket.equals(sender)) {

                            try {

                                OutputStream outputStream=clientSocket.getOutputStream();
                                outputStream.write("**file:@".getBytes());
                                outputStream.flush();

                                dataOutputStream.writeInt(sendfileNameBytes.length);
                                dataOutputStream.write(fileNameBytes);
                                dataOutputStream.writeInt(file.getData().length);
                                dataOutputStream.write(file.getData());
                                dataOutputStream.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void broadcast(String message, Socket sender) {
        for (Socket clientSocket : clientSockets) {
            if (!clientSocket.equals(sender)) {
                try {
                    clientSocket.getOutputStream().write(message.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateClientCount() {
        int count = (int) clientSockets.stream().count();
        lblCount.setText(count < 10 ? "0" + count : String.valueOf(count));
    }

    @FXML
    public void openNewClient(ActionEvent actionEvent) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/lk/ijse/chatapplication/view/client-form.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        scene.setFill(Color.TRANSPARENT);
        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Client");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void createClient(ActionEvent actionEvent) {
        txtClientName.setVisible(true);
        txtClientName.requestFocus();
    }
}
