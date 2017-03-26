package mouserun.game;

import java.io.*;
import javax.swing.*;
import java.awt.*;
import static java.lang.Thread.sleep;

/**
 * Class GameUI is the Game Interface of the game. It uses standard JFrame etc
 * components in this implementation.
 */
public class GameUI extends JFrame {

    private Maze maze;
    private GameController controller;
    private int GRID_LENGTH = 30;
    private ImagedPanel[][] mazePanels;
    private JLayeredPane container;

    /**
     * Creates an instance of the GameUI.
     *
     * @param width The width of the user interface.
     * @param height The height of the user interface.
     * @throws IOException An IOException can occur when the required game
     * assets are missing.
     */
    public GameUI(int width, int height)
            throws IOException, InterruptedException {
        super(GameConfig.GAME_TITLE);
        GRID_LENGTH = GameConfig.GRID_LENGTH;

        this.controller = new GameController(width, height, GRID_LENGTH);
        this.mazePanels = new ImagedPanel[width][height];
        this.maze = this.controller.getMaze();

        initialiseUI();
        controller.start();

    }

    // Loads and defines the frame of the user interface, the maze, the mouse
    // and the objects.
    // @throws IOException An IOException can occur if the required game assets are missing.
    private void initialiseUI() throws IOException, InterruptedException {
        JFrame frame = new JFrame();
        frame.setResizable(false);
        frame.pack();

        Insets insets = frame.getInsets();
        container = new JLayeredPane();
        container.setSize(new Dimension((maze.getWidth() * GRID_LENGTH), (maze.getHeight() * GRID_LENGTH)));

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(Color.BLACK);
        this.setSize((maze.getWidth() * GRID_LENGTH) + insets.left + insets.right, (maze.getHeight() * GRID_LENGTH) + insets.top + insets.bottom);
        this.setLayout(null);
        this.add(container);
        this.setResizable(false);

        for (int x = 0; x < maze.getWidth(); x++) {
            for (int y = 0; y < maze.getHeight(); y++) {
                Grid grid = maze.getGrid(x, y);
                String assetAddress = "assets/" + grid.getAssetName();

                ImagedPanel panel = new ImagedPanel(assetAddress, GRID_LENGTH, GRID_LENGTH);
                mazePanels[x][y] = panel;

                panel.setBounds(getGridLeft(x), getGridTop(y), GRID_LENGTH, GRID_LENGTH);
                container.add(panel);
            }
        }
        
        //AQUI PINTO UNA RATA
        ImagedPanel rata = new ImagedPanel("assets/mouseright.png", GRID_LENGTH, GRID_LENGTH);
        rata.setBounds(getGridLeft(1), getGridTop(0), GRID_LENGTH * 2, 20);
        rata.setOpaque(false);
        container.add(rata);
        container.moveToFront(rata);
    }

    public void newMouse()
            throws IOException {
        String assetAddress = GameConfig.ASSETS_MOUSEUP;
        ImagedPanel mousePanel = new ImagedPanel(assetAddress, GRID_LENGTH, GRID_LENGTH);
        mousePanel.setOpaque(false);

        JLabel label = new JLabel("009");
        label.setForeground(Color.RED);
        label.setBounds(getGridLeft(0), getGridTop(0), GRID_LENGTH * 2, 20);
        label.setOpaque(false);

        JLabel cheeselabel = new JLabel("009");
        cheeselabel.setForeground(Color.ORANGE);
        cheeselabel.setBackground(Color.ORANGE);
        cheeselabel.setBounds(getGridLeft(0), getGridTop(0) - 20, GRID_LENGTH, 20);
        cheeselabel.setOpaque(false);

        mousePanel.setBounds(getGridLeft(0), getGridTop(0), GRID_LENGTH, GRID_LENGTH);
        container.add(mousePanel);
        container.add(label);
        container.add(cheeselabel);
        container.moveToFront(mousePanel);
        container.moveToFront(label);
        container.moveToFront(cheeselabel);

        MouseRepresent mouseInstance = new MouseRepresent(controller, mousePanel, label, cheeselabel, GameConfig.ASSETS_MOUSEUP, GameConfig.ASSETS_MOUSEDOWN,
                GameConfig.ASSETS_MOUSELEFT, GameConfig.ASSETS_MOUSERIGHT);

    }

    // Converts the Maze X value to the Left value of the Game Interface
    private int getGridLeft(int x) {
        return x * GRID_LENGTH;
    }

    // Converts the Maze Y value to the Top value of the Game Interface
    private int getGridTop(int y) {
        return (maze.getHeight() - y - 1) * GRID_LENGTH;
    }
}
