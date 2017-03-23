/**
 * MouseRun. A programming game to practice building intelligent things.
 * Copyright (C) 2013  Muhammad Mustaqim
 *
 * This file is part of MouseRun.
 *
 * MouseRun is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MouseRun is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MouseRun.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package mouserun.game;

import mouserun.game.common.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;

/**
 * Class GameUI is the Game Interface of the game. It uses standard JFrame etc
 * components in this implementation.
 */
public class GameUI extends JFrame implements GameControllerAdapter {

    private Maze maze;
    private GameController controller;
    private int GRID_LENGTH = 30;
    private ImagedPanel[][] mazePanels;
    private JLayeredPane container;
    private JLabel countDownLabel;
    private SequencingThread sequencer;

    /**
     * Creates an instance of the GameUI.
     *
     * @param width The width of the user interface.
     * @param height The height of the user interface.
     * @param numberOfCheese The number of cheese this game is playing for.
     * @throws IOException An IOException can occur when the required game
     * assets are missing.
     */
    public GameUI(int width, int height, int numberOfCheese)
            throws IOException {
        super(GameConfig.GAME_TITLE);
        GRID_LENGTH = GameConfig.GRID_LENGTH;

        this.controller = new GameController(this, width, height, GRID_LENGTH, numberOfCheese);
        this.mazePanels = new ImagedPanel[width][height];
        this.maze = this.controller.getMaze();
        this.sequencer = new SequencingThread();

        initialiseUI();
        controller.start();

    }

    // Loads and defines the frame of the user interface, the maze, the mouse
    // and the objects.
    // @throws IOException An IOException can occur if the required game assets are missing.
    private void initialiseUI()
            throws IOException {
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

        countDownLabel = new JLabel("");
        countDownLabel.setForeground(Color.WHITE);
        countDownLabel.setFont(new Font("San Serif", Font.PLAIN, GameConfig.COUNT_DOWN_FONT_SIZE));
        container.add(countDownLabel);

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

    }

    /*
	 * (non-Javadoc)
	 * @see mouserun.game.GameControllerAdapter#start()
     */
    public void start() {
        sequencer.start();
    }


    // Converts the Maze X value to the Left value of the Game Interface
    private int getGridLeft(int x) {
        return x * GRID_LENGTH;
    }

    // Converts the Maze Y value to the Top value of the Game Interface
    private int getGridTop(int y) {
        return (maze.getHeight() - y - 1) * GRID_LENGTH;
    }


    @Override
    public void stop() {
       
    }

    @Override
    public void displayCountDown(int seconds) {
       
    }

}
