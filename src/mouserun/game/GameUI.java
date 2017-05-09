package mouserun.game;

import GUI.ClasificacionJframe;
import agentes.AgenteLaberinto;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.lang.acl.ACLMessage;
import jade.proto.SubscriptionResponder.Subscription;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegos.elementos.DetalleInforme;
import juegos.elementos.Partida;
import juegos.elementos.Posicion;
import laberinto.OntologiaLaberinto;
import laberinto.elementos.EntornoLaberinto;
import laberinto.elementos.JugadaEntregada;
import laberinto.elementos.PosicionQueso;
import laberinto.elementos.ResultadoJugada;
import util.ContenedorLaberinto;
import util.GestorSuscripciones;
import util.ResultadoRaton;

/**
 * Class GameUI is the Game Interface of the game. It uses standard JFrame etc
 * components in this implementation.
 */
public class GameUI extends JFrame {

    /**
     * @return the quesito
     */
    public Queso getQuesito() {
        return quesito;
    }

    private Maze maze;
    private int GRID_LENGTH = 30;
    private ImagedPanel[][] mazePanels;
    private JLayeredPane container;

    private ClasificacionJframe clasificacionGUI;
    private ContenedorLaberinto contenedor;

    //Ratas y queso
    private Queso quesito;
    private ArrayList<Rata> arrayRatas;
    private ArrayList<Bomba> arrayBombas;

    private int ancho;
    private int alto;

    private int maxQuesos;

    private GestorSuscripciones gestor;
    private Codec codec;
    private Ontology ontology;
    private final ContentManager manager;

    /**
     * Creates an instance of the GameUI.
     *
     * @param ancho The width of the user interface.
     * @param alto The height of the user interface.
     * @param mQuesos
     * @param tiempo
     * @param bombasM
     * @param cont
     * @param ge
     * @throws IOException An IOException can occur when the required game
     * assets are missing.
     * @throws java.lang.InterruptedException
     */
    public GameUI(int ancho, int alto, int mQuesos, int tiempo, int bombasM, ContenedorLaberinto cont, GestorSuscripciones ge, Codec co, Ontology ont, ContentManager ma) throws IOException, InterruptedException {
        super("Agente raton de UJAtaco");
        GRID_LENGTH = 30;

        arrayBombas = new ArrayList();

        this.codec = co;
        this.ontology = ont;
        this.manager = ma;

        this.ancho = ancho;
        this.alto = alto;

        this.contenedor = cont;
        this.gestor = ge;

        this.maxQuesos = mQuesos;

        this.mazePanels = new ImagedPanel[ancho][alto];
        this.maze = new Maze(ancho, alto);

        clasificacionGUI = new ClasificacionJframe(ancho, alto, mQuesos, tiempo, bombasM);

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

    public java.util.List<ResultadoJugada> hacerJugadas(java.util.List<JugadaEntregada> jugadas, Partida part) throws IOException, Codec.CodecException, OntologyException {
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
        comprobarQueso(jugadas, part);
        return nuevosEntornos;
    }

    public void nuevoQueso() throws IOException {
        int x = (int) (Math.random() * alto);
        int y = (int) (Math.random() * ancho);
        quesito = new Queso(x, alto - 1 - y);
        container.add(getQuesito().getPanel());
        container.moveToFront(getQuesito().getPanel());
    }

    public void nuevaTrampa(int x, int y, String creador) throws IOException {
        Bomba bomb = new Bomba(x, y, creador);
        container.add(bomb.getPanel());
        container.moveToFront(bomb.getPanel());
        container.add(bomb.getLabel());
        container.moveToFront(bomb.getLabel());
        arrayBombas.add(bomb);
    }

    public void generarRatones(ArrayList<ResultadoRaton> ratonesPartida) throws IOException {
        Rata rata;
        for (int i = 0; i < ratonesPartida.size(); i++) {
            rata = new Rata(ratonesPartida.get(i).getNombre(), 0, alto - 1 - 0);
            arrayRatas.add(rata);
            container.add(rata.getPanel());
            container.moveToFront(rata.getPanel());
            container.add(rata.getJLabel());
            container.moveToFront(rata.getJLabel());
        }
        clasificacionGUI.crearClarificacion(arrayRatas);
    }

    public void comprobarQueso(java.util.List<JugadaEntregada> jugadas, Partida part) throws Codec.CodecException, OntologyException {
        for (int j = 0; j < arrayRatas.size(); j++) {
            if (arrayRatas.get(j).getX() == getQuesito().getX() && arrayRatas.get(j).getY() == getQuesito().getY()) {

                arrayRatas.get(j).incrementaQueso();
                int x = (int) (Math.random() * alto);
                int y = (int) (Math.random() * ancho);
                getQuesito().setPosicion(x, alto - 1 - y);

                PosicionQueso posicion = null;
                for (int i = 0; i < jugadas.size(); i++) {
                    if (jugadas.get(i).getJugador().getNombre().equals(arrayRatas.get(j).getJLabel().getText())) {
                        posicion = new PosicionQueso(part, new Posicion(getQuesito().getX(), alto - 1 -getQuesito().getY()), jugadas.get(i).getJugador());
                        i = jugadas.size();
                    }
                }

                Subscription suscripcion;
                DetalleInforme quesoLogrado = new DetalleInforme(part, posicion);

                posicion = new PosicionQueso();
                for (int i = 0; i < arrayRatas.size(); i++) {
                    suscripcion = gestor.getSuscripcion(arrayRatas.get(i).getJLabel().getText());

                    // Creamos el mensaje para enviar a los jugadores
                    ACLMessage msgQueso = new ACLMessage(ACLMessage.INFORM);
                    msgQueso.setLanguage(codec.getName());
                    msgQueso.setOntology(ontology.getName());

                    manager.fillContent(msgQueso, quesoLogrado);

                    if (suscripcion != null) {
                        suscripcion.notify(msgQueso);
                    }
                }

                if (arrayRatas.get(j).getQuesos() == this.maxQuesos) {
                    mostrarFIN();
                }
                clasificacionGUI.crearClarificacion(arrayRatas);
            }
        }
    }

    public void anunciarQueso() {

    }

    public void mostrarFIN() {
        this.contenedor.completarObjetivoQuesos();
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

    private int getGridLeft(int x) {
        return x * GRID_LENGTH;
    }

    private int getGridTop(int y) {
        return (maze.getHeight() - y - 1) * GRID_LENGTH;
    }
}
