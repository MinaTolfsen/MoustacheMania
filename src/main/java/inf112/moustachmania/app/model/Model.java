package inf112.moustachmania.app.model;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import inf112.moustachmania.app.MoustacheMania;
import inf112.moustachmania.app.controller.SoundController;
import inf112.moustachmania.app.player.Player;
import inf112.moustachmania.app.screens.GameOverScreen;
import inf112.moustachmania.app.screens.StartScreen;

public class Model implements IModel {

    private final Player player;
    private final MoustacheMania game;
    private TiledMapTileLayer collisionMap;
    private Array<Rectangle> tiles = new Array<Rectangle>();
    private static final float GRAVITY = -0.005f;

    private Pool<Rectangle> rectPool = new Pool<Rectangle>() {
        @Override
        protected Rectangle newObject() {
            return new Rectangle();
        }
    };


    public Model(final MoustacheMania game, Player player) {
        this.game = game;
        this.player = player;
    }

    /**
     * Updates the model
     * @param deltaTime The time since the last update
     */
    public void update(float deltaTime) {
        // Continuously try to make player fall
        player.velocity.add(0, GRAVITY);
        checkYCollision(player);
        player.position.add(player.velocity);

        // Updating players stateTime. Important for the player animations.
        player.stateTime += deltaTime;

        // If player is slowing down, set speed to 0 and change frame
        if (Math.abs(player.velocity.x) < 1) {
            player.velocity.x = 0;
            if (player.grounded) player.state = Player.State.Standing;
        }

        // Check if player is in bounds of the screen
        checkPlayerOutOfBounds(player);

        // multiply by delta time, so we know how far we go in this frame
        player.velocity.scl(deltaTime);
        player.velocity.scl(1 / deltaTime);
    }

    /**
     * When called upon from the controller, the model moves the player to the desired position.
     * @param x input speed from controller. Always 1, but is multiplied with MAX_VELOCITY from Player.java
     */
    public void movePlayer(int x) {
        player.velocity.x = x * Player.MAX_VELOCITY;
        checkXCollision(player);
        player.position.add(player.velocity);
    }

    /**
     * When called upon from the controller, the model makes the player jump
     * and moves the player upwards based on Player.JUMP_VELOCITY.
     * Sets Player.grounded = false.
     */
    public void jumpPlayer() {
        if (player.grounded) {
            player.velocity.y += Player.JUMP_VELOCITY;
            player.grounded = false;
        }
    }

    /**
     * Check for collision in the x-axis.
     * @param player checks collision for the current player object in regard to the collision layer from the Tiled map.
     */
    private void checkXCollision(Player player) {
        Rectangle playerRect = rectPool.obtain();
        playerRect.set(player.position.x, player.position.y, Player.WIDTH, Player.HEIGHT);
        int startX, startY, endX, endY;
        // finds the x-position of the player - both if the player is moving and standing still
        if (player.velocity.x > 0) {
            startX = endX = (int)(player.position.x + Player.WIDTH + player.velocity.x);
        } else {
            startX = endX = (int)(player.position.x + player.velocity.x);
        }
        startY = (int)(player.position.y);
        endY = (int)(player.position.y + Player.HEIGHT);
        getTiles(startX, startY, endX, endY, tiles);
        playerRect.x += player.velocity.x;
        for (Rectangle tile : tiles) {
            if (playerRect.overlaps(tile)) {
                player.velocity.x = 0;
                break;
            }
        }
        playerRect.x = player.position.x;
        rectPool.free(playerRect);
        checkYCollision(player);
    }

    /**
     * Checks collision in the y-axis for the current player object in regard to the collision layer from the Tiled map.
     * @param player Current player object for the game
     */
    private void checkYCollision(Player player) {
        Rectangle playerRect = rectPool.obtain();
        playerRect.set(player.position.x, player.position.y, Player.WIDTH, Player.HEIGHT);
        int startX, startY, endX, endY;
        if (player.velocity.y > 0) {
            startY = endY = (int)(player.position.y + Player.HEIGHT + player.velocity.y);
        } else {
            startY = endY = (int)(player.position.y + player.velocity.y);
        }
        startX = (int)(player.position.x);
        endX = (int)(player.position.x + Player.WIDTH);
        getTiles(startX, startY, endX, endY, tiles);
        playerRect.y += player.velocity.y;
        for (Rectangle tile : tiles) {
            if (playerRect.overlaps(tile)) {
                // we actually reset the players y-position here
                // So its just below/above the tile we collided with this should remove bouncing.

                // If the player jumps up into a block:
                if (player.velocity.y > 0) {
                    player.position.y = tile.y - Player.HEIGHT;
                    // TODO: implement breaking blocks.
                    // we hit a block jumping upwards, let's destroy it!
                    //collisionMap.setCell((int)tile.x, (int)tile.y, null);
                } else {
                    player.position.y = tile.y + tile.height;
                    // if we hit the ground, mark us as grounded, so we can jump again
                    player.grounded = true;
                }
                player.velocity.y = 0;
                break;
            }
        }
        rectPool.free(playerRect);
    }

    private void checkPlayerOutOfBounds(Player player) {
        float playerX = player.position.x;
        float playerY = player.position.y;
        float mapWidth = collisionMap.getWidth();
        float mapHeight = collisionMap.getHeight();

        // Check if the player is outside the map boundaries
        if (playerX < -2 || playerY < -2 || playerX > mapWidth || playerY > mapHeight) {
            // Trigger game over event or state
            //game.setScreen(new StartScreen(game));
            game.setScreen(new GameOverScreen(game));
        }
    }

    /**
     * Sets the collision map
     * @param collisionLayer The collision map
     */
    public void setCollisionMap(TiledMapTileLayer collisionLayer) {
        collisionMap = collisionLayer;
    }

    /**
     * Gets the collision map
     * @return The collision map
     */
    public void getTiles(int startX, int startY, int endX, int endY, Array<Rectangle> tiles) {
        rectPool.freeAll(tiles);
        tiles.clear();

        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                TiledMapTileLayer.Cell cell = collisionMap.getCell(x, y);
                if (cell != null) {
                    Rectangle rect = rectPool.obtain();
                    rect.set(x, y, 1, 1);
                    tiles.add(rect);
                }
            }
        }
    }

    /**
     * Getter for the controller and view to fetch the width of the level.
     * @return width of the level in int.
     */
    public int getLevelWidth() {
        return collisionMap.getWidth();
    }


    /**
     * Gets the player
     * @return The player
     */
    public Player getPlayer() {
        return player;
    }
}
