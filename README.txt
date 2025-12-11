LittleShipGame - 2 Players (ESP32 + Keyboard)
============================================

Structure:
- src\
    ControllerListener.java
    Esp32ControllerServer.java
    SimpleGame.java
    SpaceGameApp.java
- lib\javafx-17.0.12\lib\
    (put your JavaFX SDK JARs here)
- play.bat

Controls:
- Player 1 (ESP32 wireless controller):
    BTN_A -> RIGHT (hold = rotate right)
    BTN_C -> LEFT  (hold = rotate left)
    BTN_B -> SHOOT (hold = auto-fire)
    Messages sent over TCP:
        RIGHT_DOWN / RIGHT_UP
        LEFT_DOWN  / LEFT_UP
        SHOOT_DOWN / SHOOT_UP

- Player 2 (Keyboard):
    LEFT  arrow  -> rotate left (hold)
    RIGHT arrow  -> rotate right (hold)
    SPACE        -> shoot (hold)

Gameplay:
- Both ships shoot bullets that can destroy bouncing targets.
- When a target is hit:
    - An explosion animation is shown.
    - A new target is spawned.
    - Score is added to the player who fired the bullet.
- Star field background with twinkling stars.

How to run:
1. Install JDK (17 or 21 recommended) and ensure 'java' and 'javac' are on PATH.
2. Download JavaFX SDK 17.x and copy all JARs from its 'lib' folder into:
       lib\javafx-17.0.12\lib\
3. Open Command Prompt in this folder and run:
       play.bat

Enjoy!
