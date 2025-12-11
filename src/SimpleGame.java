// SimpleGame.java
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Game with:
 * - Player 1 ship (center) controlled by ESP32 (RIGHT/LEFT/SHOOT *_DOWN/_UP)
 * - Player 2 ship (bottom) controlled by keyboard (left/right arrows + SPACE)
 * - Bouncing targets (bubbles)
 * - Explosions when targets are hit
 * - Twinkling star background
 */
public class SimpleGame implements ControllerListener {

    private final double width;
    private final double height;

    // === Ship model ===
    private static class Ship {
        double x, y;
        double angleDeg;      // 0 = up; positive = rotate right (clockwise)
        final double size;

        // input state flags
        boolean rotateRight = false;
        boolean rotateLeft  = false;
        boolean shooting    = false;

        // shooting timer
        double shootCooldown = 0.0;

        Ship(double x, double y, double size) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.angleDeg = 0;
        }
    }

    private final Ship player1;
    private final Ship player2;

    private static final double ROT_SPEED = 120; // degrees per second

    // === Bullets ===
    private static class Bullet {
        double x, y;
        double dx, dy;
        double speed = 300;  // pixels per second
        double radius = 5;
        int owner; // 1 or 2

        Bullet(double x, double y, double dx, double dy, int owner) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.owner = owner;
        }

        void update(double dt) {
            x += dx * speed * dt;
            y += dy * speed * dt;
        }
    }

    // === Targets (bouncing circles) ===
    private static class Target {
        double x, y;
        double dx, dy;
        double radius;

        Target(double x, double y, double dx, double dy, double radius) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.radius = radius;
        }

        void update(double dt, double width, double height) {
            x += dx * dt;
            y += dy * dt;

            // Bounce from walls
            if (x - radius < 0) {
                x = radius;
                dx = -dx;
            } else if (x + radius > width) {
                x = width - radius;
                dx = -dx;
            }

            if (y - radius < 0) {
                y = radius;
                dy = -dy;
            } else if (y + radius > height) {
                y = height - radius;
                dy = -dy;
            }
        }
    }

    // === Explosions ===
    private static class Explosion {
        double x, y;
        double age = 0.0;      // seconds since start
        double duration = 0.4; // total time
        double maxRadius = 40;

        Explosion(double x, double y) {
            this.x = x;
            this.y = y;
        }

        void update(double dt) {
            age += dt;
        }

        boolean isDone() {
            return age >= duration;
        }

        void render(GraphicsContext gc) {
            double t = age / duration;
            if (t < 0) t = 0;
            if (t > 1) t = 1;
            double radius = maxRadius * t;
            double alpha = 1.0 - t;

            gc.setFill(Color.color(1.0, 0.5, 0.0, alpha)); // orange with fade
            gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);

            gc.setStroke(Color.color(1.0, 1.0, 0.0, alpha));
            gc.strokeOval(x - radius * 1.2, y - radius * 1.2,
                          radius * 2.4, radius * 2.4);
        }
    }

    // === Stars ===
    private static class Star {
        double x, y;
        double baseBrightness;
        double phase; // for twinkle
    }

    private final List<Star> stars = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Target> targets = new ArrayList<>();
    private final List<Explosion> explosions = new ArrayList<>();
    private int scoreP1 = 0;
    private int scoreP2 = 0;
    private final Random random = new Random();

    public SimpleGame(double width, double height) {
        this.width = width;
        this.height = height;

        double shipSize = 40;
        // Player 1 in the middle
        this.player1 = new Ship(width / 2.0, height / 2.0, shipSize);
        // Player 2 at bottom center, looking up
        this.player2 = new Ship(width / 2.0, height - 80, shipSize);
        //this.player2.angleDeg = 0; // up

        spawnInitialTargets();
        initStars(100);
    }

    private void spawnInitialTargets() {
        int numTargets = 7;
        for (int i = 0; i < numTargets; i++) {
            spawnTarget();
        }
    }

    private void spawnTarget() {
        double radius = 6 + random.nextDouble() * 20; // 26–40 px
        double x = radius + random.nextDouble() * (width - 2 * radius);
        double y = radius + random.nextDouble() * (height - 2 * radius);
        double speed = 50 + random.nextDouble() * 80;  // 50–130 px/sec
        double angle = random.nextDouble() * 2 * Math.PI;
        double dx = Math.cos(angle) * speed;
        double dy = Math.sin(angle) * speed;
        targets.add(new Target(x, y, dx, dy, radius));
    }

    private void initStars(int count) {
        for (int i = 0; i < count; i++) {
            Star s = new Star();
            s.x = random.nextDouble() * width;
            s.y = random.nextDouble() * height;
            s.baseBrightness = 0.3 + random.nextDouble() * 0.7;
            s.phase = random.nextDouble() * Math.PI * 2;
            stars.add(s);
        }
    }

    // === ControllerListener for Player 1 (ESP32) ===
    @Override
    public void onRightDown() {
        player1.rotateRight = true;
    }

    @Override
    public void onRightUp() {
        player1.rotateRight = false;
    }

    @Override
    public void onLeftDown() {
        player1.rotateLeft = true;
    }

    @Override
    public void onLeftUp() {
        player1.rotateLeft = false;
    }

    @Override
    public void onShootDown() {
        player1.shooting = true;
        fireBullet(player1, 1); // immediate shot
        player1.shootCooldown = 0.2;
    }

    @Override
    public void onShootUp() {
        player1.shooting = false;
    }

    // === Keyboard controls for Player 2 ===
    public void p2RightDown() { player2.rotateRight = true; }
    public void p2RightUp()   { player2.rotateRight = false; }

    public void p2LeftDown()  { player2.rotateLeft = true; }
    public void p2LeftUp()    { player2.rotateLeft = false; }

    public void p2ShootDown() {
        player2.shooting = true;
        fireBullet(player2, 2);
        player2.shootCooldown = 0.2;
    }

    public void p2ShootUp() {
        player2.shooting = false;
    }
    
    // === Core game logic ===

    private void updateShip(Ship ship, double dt) {
        if (ship.rotateRight && !ship.rotateLeft) {
            ship.angleDeg += ROT_SPEED * dt;
        } else if (ship.rotateLeft && !ship.rotateRight) {
            ship.angleDeg -= ROT_SPEED * dt;
        }

        ship.angleDeg = (ship.angleDeg % 360 + 360) % 360;

        if (ship.shooting) {
            ship.shootCooldown -= dt;
            if (ship.shootCooldown <= 0) {
                fireBullet(ship, ship == player1 ? 1 : 2);
                ship.shootCooldown = 0.2;
            }
        }
    }

    private void fireBullet(Ship ship, int owner) {
        double rad = Math.toRadians(ship.angleDeg);
        double dirX = Math.sin(rad);
        double dirY = -Math.cos(rad);
        double tipX = ship.x + dirX * ship.size;
        double tipY = ship.y + dirY * ship.size;
        bullets.add(new Bullet(tipX, tipY, dirX, dirY, owner));
    }

    public void update(double dt) {
        // Update ships
        updateShip(player1, dt);
        updateShip(player2, dt);

        // Update bullets
        Iterator<Bullet> bulletIt = bullets.iterator();
        while (bulletIt.hasNext()) {
            Bullet b = bulletIt.next();
            b.update(dt);
            if (b.x < 0 || b.x > width || b.y < 0 || b.y > height) {
                bulletIt.remove();
            }
        }

        // Update targets
        for (Target t : targets) {
            t.update(dt, width, height);
        }

        // Update explosions
        Iterator<Explosion> expIt = explosions.iterator();
        while (expIt.hasNext()) {
            Explosion e = expIt.next();
            e.update(dt);
            if (e.isDone()) {
                expIt.remove();
            }
        }

        // Bullet-target collisions
        bulletIt = bullets.iterator();
        while (bulletIt.hasNext()) {
            Bullet b = bulletIt.next();
            Iterator<Target> targetIt = targets.iterator();
            boolean removedBullet = false;

            while (targetIt.hasNext()) {
                Target t = targetIt.next();
                double dx = b.x - t.x;
                double dy = b.y - t.y;
                double dist2 = dx * dx + dy * dy;
                double hitDist = b.radius + t.radius;
                if (dist2 <= hitDist * hitDist) {
                    // Hit
                    bulletIt.remove();
                    targetIt.remove();
                    explosions.add(new Explosion(t.x, t.y));
                    if (b.owner == 1) {
                        scoreP1 += 10;
                    } else if (b.owner == 2) {
                        scoreP2 += 10;
                    }
                    spawnTarget();
                    removedBullet = true;
                    break;
                }
            }

            if (removedBullet) {
                // go to next bullet
            }
        }
    }

    public void render(GraphicsContext gc) {
        // Background
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, width, height);

        // Stars with twinkle
        double timeFactor = System.currentTimeMillis() / 1000.0;
        for (Star s : stars) {
            double twinkle = 0.4 + 0.6 * Math.sin(timeFactor * 2 + s.phase);
            double b = s.baseBrightness * twinkle;
            if (b < 0) b = 0;
            if (b > 1) b = 1;
            gc.setFill(Color.color(1, 1, 1, b));
            gc.fillOval(s.x, s.y, 2, 2);
        }

        // Draw targets (bubbles) – THIS is what was missing
        gc.setFill(Color.DEEPSKYBLUE);
        for (Target t : targets) {
            gc.fillOval(t.x - t.radius, t.y - t.radius,
                        t.radius * 2, t.radius * 2);
        }

        // Explosions
        for (Explosion e : explosions) {
            e.render(gc);
        }

        // Ships
        drawShip(gc, player1, Color.CYAN);
        drawShip(gc, player2, Color.LIME);

        // Bullets
        for (Bullet b : bullets) {
            if (b.owner == 1) {
                gc.setFill(Color.YELLOW);
            } else {
                gc.setFill(Color.ORANGE);
            }
            gc.fillOval(b.x - b.radius, b.y - b.radius,
                        b.radius * 2, b.radius * 2);
        }

        // Scores
        gc.setFill(Color.WHITE);
        gc.fillText("P1 (ESP32) Score: " + scoreP1, 10, 20);
        gc.fillText("P2 (Keyboard) Score: " + scoreP2, 10, 40);
    }

    private void drawShip(GraphicsContext gc, Ship ship, Color color) {
        gc.save();
        gc.translate(ship.x, ship.y);
        gc.rotate(ship.angleDeg);
        gc.setFill(color);
        double halfBase = ship.size * 0.5;
        double[] xPoints = {0, -halfBase, halfBase};
        double[] yPoints = {-ship.size, ship.size * 0.5, ship.size * 0.5};
        gc.fillPolygon(xPoints, yPoints, 3);
        gc.restore();
    }
}
