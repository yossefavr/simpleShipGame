// Esp32ControllerServer.java
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Esp32ControllerServer implements Runnable {

    private final int port;
    private final ControllerListener listener;

    public Esp32ControllerServer(int port, ControllerListener listener) {
        this.port = port;
        this.listener = listener;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("ESP32 Controller server started on port " + port);

            while (true) {
                System.out.println("Waiting for ESP32 to connect...");
                try (Socket socket = serverSocket.accept()) {
                    System.out.println("ESP32 connected from " + socket.getInetAddress());

                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream())
                    );

                    String line;
                    while ((line = in.readLine()) != null) {
                        String msg = line.trim();
                        System.out.println("ESP32 -> " + msg);

                        switch (msg) {
                            case "RIGHT_DOWN" -> Platform.runLater(listener::onRightDown);
                            case "RIGHT_UP"   -> Platform.runLater(listener::onRightUp);
                            case "LEFT_DOWN"  -> Platform.runLater(listener::onLeftDown);
                            case "LEFT_UP"    -> Platform.runLater(listener::onLeftUp);
                            case "SHOOT_DOWN" -> Platform.runLater(listener::onShootDown);
                            case "SHOOT_UP"   -> Platform.runLater(listener::onShootUp);
                            default -> System.out.println("Unknown command: " + msg);
                        }
                    }

                    System.out.println("ESP32 disconnected.");
                } catch (IOException e) {
                    System.err.println("Connection error: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("Could not start server on port " + port + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
