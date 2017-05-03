package mouserun.game;

import GUI.ClasificacionJframe;
import agentes.AgenteLaberinto;
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

    private AgenteLaberinto agente;

    private Maze maze;
    private int GRID_LENGTH = 30;
    private ImagedPanel[][] mazePanels;
    private JLayeredPane container;

    private ClasificacionJframe clasificacionGUI;

    //Ratas y queso
    private Queso quesito;
    private ArrayList<Rata> arrayRatas;
    private ArrayList<Bomba> arrayBombas;

    private int ancho;
    private int alto;

    private int maxQuesos;

    /**
     * Creates an instance of the GameUI.
     *
     * @param ancho The width of the user interface.
     * @param alto The height of the user interface.
     * @param mQuesos
     * @param agent
     * @param tiempo
     * @param bombasM
     * @throws IOException An IOException can occur when the required game
     * assets are missing.
     * @throws java.lang.InterruptedException
     */
    public GameUI(int ancho, int alto, int mQuesos, AgenteLaberinto agent,int tiempo,int bombasM) throws IOException, InterruptedException {
        super("Agente raton de UJAtaco");
        GRID_LENGTH = 30;

        arrayBombas = new ArrayList();

        this.ancho = ancho;
        this.alto = alto;

        this.agente = agent;
        this.maxQuesos = mQuesos;

        this.mazePanels = new ImagedPanel[ancho][alto];
        this.maze = new Maze(ancho, alto);

        clasificacionGUI = new ClasificacionJframe(ancho,alto,mQuesos,tiempo,bombasM);

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

    public java.util.List<ResultadoJugada> hacerJugadas(java.util.List<JugadaEntregada> jugadas, Partida part) throws IOException {
        java.util.List<ResultadoJugada> nuevosEntornos;
        nuevosEntornos = new ArrayList();
        boolean muerto;
        for (int i = 0; i < jugadas.size(); i++) {
            for (int j = 0; j < arrayRatas.size(); j++) {
                if (jugadas.get(i).getJugador().getNombre().equals(arrayRatas.get(j).getJLabel().getText())) {
                    muerto = false;
                    int x = arrayRatas.get(j).getX();
                    int y = arrayRatas.get(j).getY();
                    if (jugadas.get(i).getAccion().getJugada().equals(OntologiaLaberinto.MOVIMIENTO)) {
                        arrayRatas.get(j).setPosicion(jugadas.get(i).getAccion().getPosicion().getCoorX(), alto - 1 - jugadas.get(i).getAccion().getPosicion().getCoorY());
                    } else {
                        nuevaTrampa(x, y, jugadas.get(i).getJugador().getNombre());
                        arrayRatas.get(j).incrementaBombasColocadas();
                        clasificacionGUI.crearClarificacion(arrayRatas);
                    }
                    for (int z = 0; z < arrayBombas.size(); z++) {
                        if (!arrayBombas.get(z).getLabel().getText().equals(arrayRatas.get(j).getJLabel().getText())) {
                            if (arrayBombas.get(z).getX() == arrayRatas.get(j).getX() && arrayBombas.get(z).getY() == arrayRatas.get(j).getY()) { //Esta rata debe morir
                                int xx = (int) (Math.random() * alto);
                                int yy = (int) (Math.random() * ancho);
                                arrayRatas.get(j).setPosicion(xx, yy);
                                arrayBombas.get(z).explotar();
                                arrayBombas.remove(z);
                                --z;
                                clasificacionGUI.crearClarificacion(arrayRatas);
                            }
                        }
                    }
                    EntornoLaberinto ent = getEntorno(arrayRatas.get(j).getX(), alto - 1 - arrayRatas.get(j).getY());
                    Posicion pos = new Posicion(arrayRatas.get(j).getX(), alto - 1 - arrayRatas.get(j).getY());
                    ResultadoJugada resultadoJug = new ResultadoJugada(part, ent, pos);
                    nuevosEntornos.add(resultadoJug);
                }
            }
        }
        comprobarQueso();
        return nuevosEntornos;
    }

    public void nuevoQueso() throws IOException {
        int x = (int) (Math.random() * alto);
        int y = (int) (Math.random() * ancho);
        quesito = new Queso(x, alto - 1 - y);
        container.add(quesito.getPanel());
        container.moveToFront(quesito.getPanel());
    }

    public void nuevaTrampa(int x, int y, String creador) throws IOException {
        Bomba bomb = new Bomba(x, y, creador);
        container.add(bomb.getPanel());
        container.moveToFront(bomb.getPanel());
        container.add(bomb.getLabel());
        container.moveToFront(bomb.getLabel());
        arrayBombas.add(bomb);
    }

    public void generarRatones(Posicion posicionInicio, ArrayList<ResultadoRaton> ratonesPartida) throws IOException {
        Rata rata;
        for (int i = 0; i < ratonesPartida.size(); i++) {
            rata = new Rata(ratonesPartida.get(i).getNombre(), posicionInicio.getCoorX(), alto - 1 - posicionInicio.getCoorY());
            arrayRatas.add(rata);
            container.add(rata.getPanel());
            container.moveToFront(rata.getPanel());
            container.add(rata.getJLabel());
            container.moveToFront(rata.getJLabel());
        }
        clasificacionGUI.crearClarificacion(arrayRatas);
    }

    public void comprobarQueso() {
        for (int j = 0; j < arrayRatas.size(); j++) {
            if (arrayRatas.get(j).getX() == quesito.getX() && arrayRatas.get(j).getY() == quesito.getY()) {
                arrayRatas.get(j).incrementaQueso();
                int x = (int) (Math.random() * alto);
                int y = (int) (Math.random() * ancho);
                quesito.setPosicion(x, alto - 1 - y);
                if (arrayRatas.get(j).getQuesos() == this.maxQuesos) {
                    mostrarFIN();
                }
                clasificacionGUI.crearClarificacion(arrayRatas);
            }
        }
    }

    public void comprobarBombas(java.util.List<ResultadoJugada> nuevosEntornos, java.util.List<JugadaEntregada> jugadas) throws IOException {
        for (int i = 0; i < arrayRatas.size(); i++) {
            for (int j = 0; j < arrayBombas.size(); j++) {
                if (!arrayBombas.get(j).getLabel().getText().equals(arrayRatas.get(i).getJLabel().getText())) {
                    if (arrayBombas.get(j).getX() == arrayRatas.get(i).getX() && arrayBombas.get(j).getY() == arrayRatas.get(i).getY()) { //Esta rata debe morir
                        int x = (int) (Math.random() * alto);
                        int y = (int) (Math.random() * ancho);
                        arrayRatas.get(i).setPosicion(x, y);
                        for (int z = 0; z < jugadas.size(); z++) {
                            if (jugadas.get(z).getJugador().getNombre().equals(arrayRatas.get(i).getJLabel().getText())) {
                                //Posicion pos = new Posicion(arrayRatas.get(i).getX(), alto - 1 - arrayRatas.get(i).getY());
                                //EntornoLaberinto ent = getEntorno(arrayRatas.get(i).getX(), alto - 1 - arrayRatas.get(i).getY());
                                //nuevosEntornos.get(z).setNuevaPosicion(pos);
                                //nuevosEntornos.get(z).setEntorno(ent);
                            }
                        }
                    }
                }
            }
        }
    }

    public void mostrarFIN() {
        this.agente.lograrObjetivoQuesos();
        JLabel countDownLabel = new JLabel("");
        countDownLabel.setForeground(Color.WHITE);
        countDownLabel.setFont(new Font("San Serif", Font.PLAIN, 100));
        container.add(countDownLabel);
        countDownLabel.setText("Final");
        Dimension preferred = countDownLabel.getPreferredSize();
        int yy = (int) ((container.getHeight() - preferred.getHeight()) / 2);
        int xx = (int) ((container.getWidth() - preferred.getWidth()) / 2);

        countDownLabel.setBounds(xx, yy, (int) preferred.getWidth(), (int) preferred.getHeight());
        container.moveToFront(countDownLabel);
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
