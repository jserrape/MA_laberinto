package mouserun.game;

import GUI.ClasificacionJframe;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.lang.acl.ACLMessage;
import jade.proto.SubscriptionResponder.Subscription;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import juegos.elementos.DetalleInforme;
import juegos.elementos.GanadorPartida;
import juegos.elementos.Jugador;
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

    private final Maze maze;
    private int GRID_LENGTH = 30;
    private final ImagedPanel[][] mazePanels;
    private JLayeredPane container;

    private final ClasificacionJframe clasificacionGUI;
    private final ContenedorLaberinto contenedor;

    //Ratas y queso
    private Queso quesito;
    private ArrayList<Rata> arrayRatas;
    private final ArrayList<Bomba> arrayBombas;

    private final int ancho;
    private final int alto;

    private final int maxQuesos;

    private final GestorSuscripciones gestor;
    private final Codec codec;
    private final Ontology ontology;
    private final ContentManager manager;

    private final ArrayList<String> jugadasFichero;

    private boolean yaAcabado;

    /**
     * Creates an instance of the GameUI.
     *
     * @param id Identificador de la partida
     * @param ancho Ancho del laberinto
     * @param alto Alto del laberinto
     * @param mQuesos Objetivo de quesos a lograr en la partida
     * @param tiempo Tiempo maximo de la partida
     * @param bombasM Bombas maximas que puede colocar un raton
     * @param cont Estyructura de control del laberinto
     * @param ge Gestor de uscripciones
     * @param co Codec
     * @param ont Ontologia del laberinto
     * @param ma Content Manager
     * @throws IOException An IOException can occur when the required game
     * assets are missing.
     * @throws java.lang.InterruptedException
     */
    public GameUI(String id, int ancho, int alto, int mQuesos, int tiempo, int bombasM, ContenedorLaberinto cont, GestorSuscripciones ge, Codec co, Ontology ont, ContentManager ma) throws IOException, InterruptedException {
        super("Interfaz: " + id);
        this.GRID_LENGTH = 30;
        
        this.arrayBombas = new ArrayList();
        this.jugadasFichero = new ArrayList();

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

        this.yaAcabado = false;

        clasificacionGUI = new ClasificacionJframe(ancho, alto, mQuesos, tiempo, bombasM, id);

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

    /**
     *
     * @param x Posicion x
     * @param y Posicion y
     * @return Entorno de una posicion del laberinto
     */
    public EntornoLaberinto getEntorno(int x, int y) {
        return maze.getGrid(x, y).getEntorno();
    }

    /**
     * Gestion del los turnos
     *
     * @param jugadas Lista con las jugadas de los ratones en este turno
     * @param part Partida a la que le corresponde
     * @return Resultado de las jugadas
     * @throws IOException
     * @throws jade.content.lang.Codec.CodecException
     * @throws OntologyException
     */
    public java.util.List<ResultadoJugada> hacerJugadas(java.util.List<JugadaEntregada> jugadas, Partida part) throws IOException, Codec.CodecException, OntologyException {
        java.util.List<ResultadoJugada> nuevosEntornos;
        nuevosEntornos = new ArrayList();
        for (int i = 0; i < jugadas.size(); i++) {
            for (int j = 0; j < arrayRatas.size(); j++) {
                if (jugadas.get(i).getJugador().getNombre().equals(arrayRatas.get(j).getJLabel().getText())) {
                    jugadasFichero.add(jugadas.get(i).toString());
                    int x = arrayRatas.get(j).getX();
                    int y = arrayRatas.get(j).getY();
                    if (jugadas.get(i).getAccion().getJugada().equals(OntologiaLaberinto.MOVIMIENTO)) {
                        arrayRatas.get(j).setPosicion(jugadas.get(i).getAccion().getPosicion().getCoorX(), alto - 1 - jugadas.get(i).getAccion().getPosicion().getCoorY());
                    } else {
                        nuevaTrampa(x, y, jugadas.get(i).getJugador().getNombre());
                        arrayRatas.get(j).incrementaBombasColocadas();
                        clasificacionGUI.crearClarificacion(arrayRatas);
                        jugadasFichero.add("BOMBA");
                    }
                    for (int z = 0; z < arrayBombas.size(); z++) {
                        if (!arrayBombas.get(z).getLabel().getText().equals(arrayRatas.get(j).getJLabel().getText())) {
                            if (arrayBombas.get(z).getX() == arrayRatas.get(j).getX() && arrayBombas.get(z).getY() == arrayRatas.get(j).getY()) { //Esta rata debe morir
                                int xx = (int) (Math.random() * ancho);
                                int yy = (int) (Math.random() * alto);
                                arrayRatas.get(j).setPosicion(xx, yy);
                                arrayBombas.get(z).explotar();
                                arrayBombas.remove(z);
                                --z;
                                clasificacionGUI.crearClarificacion(arrayRatas);
                                jugadasFichero.add("MUERTE");
                            }
                        }
                    }

                    if (arrayRatas.get(j).getX() < 0) {
                        arrayRatas.get(j).setPosicion(0, arrayRatas.get(j).getY());
                        jugadasFichero.add("TRAMPOSO");
                    }
                    if (alto - 1 - arrayRatas.get(j).getY() < 0) {
                        arrayRatas.get(j).setPosicion(arrayRatas.get(j).getX(), 0);
                        jugadasFichero.add("TRAMPOSO");
                    }
                    if (arrayRatas.get(j).getX() >= ancho) {
                        arrayRatas.get(j).setPosicion(ancho - 1, arrayRatas.get(j).getY());
                        jugadasFichero.add("TRAMPOSO");
                    }
                    if (alto - 1 - arrayRatas.get(j).getY() >= alto) {
                        arrayRatas.get(j).setPosicion(arrayRatas.get(j).getX(), alto - 1);
                        jugadasFichero.add("TRAMPOSO");
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

    /**
     * Creacion de un nuevo queso
     *
     * @throws IOException
     */
    public void nuevoQueso() throws IOException {
        jugadasFichero.add("NUEVO QUESO");
        int x = (int) (Math.random() * alto);
        int y = (int) (Math.random() * ancho);
        quesito = new Queso(x, alto - 1 - y);
        container.add(getQuesito().getPanel());
        container.moveToFront(getQuesito().getPanel());
    }

    /**
     * Creacion de una nueva trampa
     *
     * @param x Posicion x
     * @param y Posicion y
     * @param creador Identificador del creador de la trampa
     * @throws IOException
     */
    public void nuevaTrampa(int x, int y, String creador) throws IOException {
        Bomba bomb = new Bomba(x, y, creador);
        container.add(bomb.getPanel());
        container.moveToFront(bomb.getPanel());
        container.add(bomb.getLabel());
        container.moveToFront(bomb.getLabel());
        arrayBombas.add(bomb);
        jugadasFichero.add("NUEVA TRAMPA");
    }

    /**
     * Funcion para crear los ratrones que participan en la partida
     *
     * @param ratonesPartida Lista con todos los ratones
     * @throws IOException
     */
    public void generarRatones(ArrayList<ResultadoRaton> ratonesPartida) throws IOException {
        Rata rata;
        for (int i = 0; i < ratonesPartida.size(); i++) {
            jugadasFichero.add("Nueva rata: " + ratonesPartida.get(i).toString());
            rata = new Rata(ratonesPartida.get(i).getNombre(), 0, alto - 1 - 0, ratonesPartida.get(i).getAidRaton());
            arrayRatas.add(rata);
            container.add(rata.getPanel());
            container.moveToFront(rata.getPanel());
            container.add(rata.getJLabel());
            container.moveToFront(rata.getJLabel());
        }
        clasificacionGUI.crearClarificacion(arrayRatas);
    }

    /**
     * Comprueba si algun raton ha logrado un queso, e informa a los demas de
     * ello
     *
     * @param jugadas Jugadas de los ratones
     * @param part Identificador de la partida
     * @throws jade.content.lang.Codec.CodecException
     * @throws OntologyException
     */
    public void comprobarQueso(java.util.List<JugadaEntregada> jugadas, Partida part) throws Codec.CodecException, OntologyException {
        for (int j = 0; j < arrayRatas.size(); j++) {
            if (arrayRatas.get(j).getX() == getQuesito().getX() && arrayRatas.get(j).getY() == getQuesito().getY()) {
                jugadasFichero.add("QUESO COGIDO");
                arrayRatas.get(j).incrementaQueso();
                int x = (int) (Math.random() * alto);
                int y = (int) (Math.random() * ancho);
                getQuesito().setPosicion(x, alto - 1 - y);

                PosicionQueso posicion = null;
                for (int i = 0; i < jugadas.size(); i++) {
                    if (jugadas.get(i).getJugador().getNombre().equals(arrayRatas.get(j).getJLabel().getText())) {
                        posicion = new PosicionQueso(part, new Posicion(getQuesito().getX(), alto - 1 - getQuesito().getY()), jugadas.get(i).getJugador());
                        i = jugadas.size();
                    }
                }

                Subscription suscripcion;
                DetalleInforme quesoLogrado = new DetalleInforme(part, posicion);

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
                    mostrarFIN(part);
                }
                clasificacionGUI.crearClarificacion(arrayRatas);
            }
        }
    }

    /**
     * Funcion pàra informar a los ratones de que alguien ha logrado un queso
     *
     * @param partida Partida en la que se juega
     * @throws jade.content.lang.Codec.CodecException
     * @throws OntologyException
     */
    public void anunciarGanador(Partida partida) throws Codec.CodecException, OntologyException {
        GanadorPartida ganador = new GanadorPartida(new Jugador(arrayRatas.get(0).getAid().getName(), arrayRatas.get(0).getAid()));
        int puntos = arrayRatas.get(0).getQuesos();
        for (int i = 1; i < arrayRatas.size(); i++) {
            if (puntos < arrayRatas.get(i).getQuesos()) {
                ganador = new GanadorPartida(new Jugador(arrayRatas.get(i).getAid().getName(), arrayRatas.get(i).getAid()));
                puntos = arrayRatas.get(i).getQuesos();
            }
        }
        Subscription suscripcion;
        DetalleInforme ganadorPartida = new DetalleInforme(partida, ganador);
        for (int i = 0; i < arrayRatas.size(); i++) {
            suscripcion = gestor.getSuscripcion(arrayRatas.get(i).getJLabel().getText());

            // Creamos el mensaje para enviar a los jugadores
            ACLMessage msgGanador = new ACLMessage(ACLMessage.INFORM);
            msgGanador.setLanguage(codec.getName());
            msgGanador.setOntology(ontology.getName());

            manager.fillContent(msgGanador, ganadorPartida);

            if (suscripcion != null) {
                suscripcion.notify(msgGanador);
            }
        }

    }

    /**
     * Finaliza la partida
     *
     * @param partida Elemento partida
     * @throws jade.content.lang.Codec.CodecException
     * @throws OntologyException
     */
    public void mostrarFIN(Partida partida) throws Codec.CodecException, OntologyException {
        if (!yaAcabado) {
            jugadasFichero.add("FIN DE PARTIDA");
            yaAcabado = true;
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
            anunciarGanador(partida);
            crearFichero();
        }
    }

    private void crearFichero() {
        java.util.Date utilDate = new java.util.Date();
        long lnMilisegundos = utilDate.getTime();
        java.sql.Date sqlDate = new java.sql.Date(lnMilisegundos);
        java.sql.Time sqlTime = new java.sql.Time(lnMilisegundos);
        String nombreFichero = sqlDate + "--" + sqlTime;
        nombreFichero = nombreFichero.replace(":", "-");
        try {
            try (BufferedWriter ficheroSalida = new BufferedWriter(new FileWriter(new File("partidasAnteriores/" + nombreFichero + ".txt")))) {
                for (int i = 0; i < jugadasFichero.size(); i++) {
                    ficheroSalida.write(jugadasFichero.get(i));
                    ficheroSalida.newLine();
                }
            }
        } catch (IOException errorDeFichero) {
            System.out.println("Ha habido problemas: " + errorDeFichero.getMessage());
        }
    }

    private int getGridLeft(int x) {
        return x * GRID_LENGTH;
    }

    private int getGridTop(int y) {
        return (maze.getHeight() - y - 1) * GRID_LENGTH;
    }
}
