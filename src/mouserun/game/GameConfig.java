package mouserun.game;

/**
 * Class GameConfiguration is a common location to edit Fixed game settings.
 */
public class GameConfig {

    /**
     * UI *
     */
    public static final String GAME_TITLE = "Agente raton de UJAtaco";
    public static final int GRID_LENGTH = 30;
    public static final int MOUSE_CHEESE_DISPLAY_LENGTH = 40;
    public static final int COUNT_DOWN_FONT_SIZE = 200;
    public static final String CHEESE_NUMBER_FORMAT = "00";

    /**
     * Assets *
     */
    public static final String ASSETS_BOMB = "assets/bomb.png";
    public static final String ASSETS_EXPLODED = "assets/exploded.png";
    public static final String ASSETS_CHEESE = "assets/cheese.png";
    public static final String ASSETS_MOUSEUP = "assets/mouseup.png";
    public static final String ASSETS_MOUSEDOWN = "assets/mousedown.png";
    public static final String ASSETS_MOUSELEFT = "assets/mouseleft.png";
    public static final String ASSETS_MOUSERIGHT = "assets/mouseright.png";

    /**
     * Logic *
     */
    public static final double RATIO_BOMBS_TO_CHEESE = 0.1;
    public static final double RATIO_CLOSED_WALL_TO_OPEN = 0.03;
    public static final int PIXELS_PER_TURN = 10;
    public static final int PIXELS_ON_TARGET_LEEWAY = 5;
    public static final int DISEASES_TO_RETIRE = 5;
    public static final int MOUSE_RESPONSE_TIMEOUT = 50;
    public static final int ROUND_SLEEP_TIME = 80;

}
