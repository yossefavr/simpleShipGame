// SpaceGameApp.java
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

public class SpaceGameApp extends Application {

    private static final double WIDTH = 800;
    private static final double HEIGHT = 600;

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        SimpleGame game = new SimpleGame(WIDTH, HEIGHT);

        // Start ESP32 controller server for Player 1
        Esp32ControllerServer server = new Esp32ControllerServer(5000, game);
        Thread serverThread = new Thread(server);
        serverThread.setDaemon(true);
        serverThread.start();

        Group root = new Group(canvas);
        Scene scene = new Scene(root);

        // Keyboard controls for Player 2:
        // LEFT / RIGHT arrows + SPACE for shoot
        scene.setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            switch (code) {
                case LEFT -> game.p2LeftDown();
                case RIGHT -> game.p2RightDown();
                case SPACE -> game.p2ShootDown();
            }
        });

        scene.setOnKeyReleased(e -> {
            KeyCode code = e.getCode();
            switch (code) {
                case LEFT -> game.p2LeftUp();
                case RIGHT -> game.p2RightUp();
                case SPACE -> game.p2ShootUp();
            }
        });

        stage.setTitle("ESP32 Little Ship Game - 2 Players");
        stage.setScene(scene);
        stage.show();

        // Animation loop
        AnimationTimer timer = new AnimationTimer() {
            private long lastTime = 0;

            @Override
            public void handle(long now) {
                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }
                double dt = (now - lastTime) / 1_000_000_000.0; // seconds
                lastTime = now;

                game.update(dt);
                game.render(gc);
            }
        };
        timer.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
