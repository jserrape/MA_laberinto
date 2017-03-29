package mouserun.game;

import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Class GameUI is the Game Interface of the game. It uses standard JFrame etc
 * components in this implementation.
 */
public class GameUI extends JFrame {

    private Maze maze;
    private int GRID_LENGTH = 30;
    private ImagedPanel[][] mazePanels;
    private JLayeredPane container;

    //Ratas y queso
    private Queso quesito;
    private ArrayList<Rata> arrayRatas;

    /**
     * Creates an instance of the GameUI.
     *
     * @param width The width of the user interface.
     * @param height The height of the user interface.
     * @throws IOException An IOException can occur when the required game assets are missing.
     * @throws java.lang.InterruptedException
     */
    public GameUI(int width, int height) throws IOException, InterruptedException {
        super("Agente raton de UJAtaco");
        GRID_LENGTH = 30;

        this.mazePanels = new ImagedPanel[width][height];
        this.maze = new Maze(width, height);

        initialiseUI();
    }

    /**
     * Se inicializa la interfaz, y se busca a los agentes que participan
     * @throws IOException  can occur if the required game assets are missing.
     * @throws InterruptedException  can occur if the required game assets are missing.
     */
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
        iniciarQuesoYRatas();

    }

    /**
     * Busca los agentes Raton, los mete en el juego, y genera un queso
     * @throws IOException  can occur if the required game assets are missing.
     */
    public void iniciarQuesoYRatas() throws IOException {
        //Busco las ratas, las almaceno en un arraylist y las meto al contenedor
        arrayRatas = new ArrayList<>();
        Rata rata = new Rata("Nombre", 1, 0);
        arrayRatas.add(rata);

        container.add(rata.getPanel());
        container.moveToFront(rata.getPanel());
        container.add(rata.getJLabel());
        container.moveToFront(rata.getJLabel());

        quesito = new Queso(5, 0);
        container.add(quesito.getPanel());
        container.moveToFront(quesito.getPanel());

    }
 
    /**
     * Converts the Maze X value to the Left value of the Game Interface
     * @param x Valor de la casilla
     * @return Left value
     */
    private int getGridLeft(int x) {
        return x * GRID_LENGTH;
    }

    /**
     * Converts the Maze Y value to the Top value of the Game Interface
     * @param y Valor de la casilla
     * @return Top value
     */
    private int getGridTop(int y) {
        return (maze.getHeight() - y - 1) * GRID_LENGTH;
    }
}
