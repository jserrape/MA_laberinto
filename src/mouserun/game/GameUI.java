package mouserun.game;

import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import juegos.elementos.Partida;
import juegos.elementos.Posicion;
import laberinto.OntologiaLaberinto;
import laberinto.elementos.EntornoLaberinto;
import laberinto.elementos.JugadaEntregada;
import laberinto.elementos.ResultadoJugada;
import util.ResultadoRaton;

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

    private int ancho;
    private int alto;

    /**
     * Creates an instance of the GameUI.
     *
     * @param ancho The width of the user interface.
     * @param alto The height of the user interface.
     * @throws IOException An IOException can occur when the required game
     * assets are missing.
     * @throws java.lang.InterruptedException
     */
    public GameUI(int ancho, int alto) throws IOException, InterruptedException {
        super("Agente raton de UJAtaco");
        GRID_LENGTH = 30;

        this.ancho = ancho;
        this.alto = alto;

        this.mazePanels = new ImagedPanel[ancho][alto];
        this.maze = new Maze(ancho, alto);

        initialiseUI();
    }

    /**
     * Se inicializa la interfaz, y se busca a los agentes que participan
     *
     * @throws IOException can occur if the required game assets are missing.
     * @throws InterruptedException can occur if the required game assets are
     * missing.
     */
    private void initialiseUI() throws IOException, InterruptedException {
        JFrame frame = new JFrame();
        frame.setResizable(false);
        frame.pack();

        arrayRatas = new ArrayList<>();

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

    }

    public EntornoLaberinto getEntorno(int x, int y) {
        return maze.getGrid(x, y).getEntorno();
    }
    
    public java.util.List<ResultadoJugada> hacerJugadas(java.util.List<JugadaEntregada> jugadas,Partida part){
        java.util.List<ResultadoJugada> nuevosEntornos;
        nuevosEntornos = new ArrayList();
        for(int i=0;i<jugadas.size();i++){
            for(int j=0;j<arrayRatas.size();j++){
                if(jugadas.get(i).getJugador().getNombre().equals(arrayRatas.get(j).getJLabel().getText())){
                    int x=arrayRatas.get(j).getX();
                    int y=arrayRatas.get(j).getY();
                    if(jugadas.get(i).getAccion().getJugada().equals(OntologiaLaberinto.MOVIMIENTO)){
                        arrayRatas.get(j).setPosicion(jugadas.get(i).getAccion().getPosicion().getCoorX(), alto-1-jugadas.get(i).getAccion().getPosicion().getCoorY());
                    }else{
                    
                    }
                    EntornoLaberinto ent=getEntorno(arrayRatas.get(j).getX(),alto-1-arrayRatas.get(j).getY());
                    Posicion pos=new Posicion(arrayRatas.get(j).getX(),alto-1-arrayRatas.get(j).getY());
                    ResultadoJugada resultadoJug=new ResultadoJugada(part,ent,pos);
                    nuevosEntornos.add(resultadoJug);
                }
            }
        }
        return nuevosEntornos;
    }


    public void nuevoQueso() throws IOException {
        int x = (int) (Math.random() * alto);
        int y = (int) (Math.random() * ancho);
        quesito = new Queso(x, alto-1-y);
        container.add(quesito.getPanel());
        container.moveToFront(quesito.getPanel());
    }

    public void generarRatones(Posicion posicionInicio, ArrayList<ResultadoRaton> ratonesPartida) throws IOException {
        Rata rata;
        for (int i = 0; i < ratonesPartida.size(); i++) {
            rata = new Rata(ratonesPartida.get(i).getNombre(), posicionInicio.getCoorX(), alto-1-posicionInicio.getCoorY());
            arrayRatas.add(rata);
            container.add(rata.getPanel());
            container.moveToFront(rata.getPanel());
            container.add(rata.getJLabel());
            container.moveToFront(rata.getJLabel());
        }
    }

    /**
     * Converts the Maze X value to the Left value of the Game Interface
     *
     * @param x Valor de la casilla
     * @return Left value
     */
    private int getGridLeft(int x) {
        return x * GRID_LENGTH;
    }

    /**
     * Converts the Maze Y value to the Top value of the Game Interface
     *
     * @param y Valor de la casilla
     * @return Top value
     */
    private int getGridTop(int y) {
        return (maze.getHeight() - y - 1) * GRID_LENGTH;
    }
}
