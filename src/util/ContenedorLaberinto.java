/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.onto.Ontology;
import java.io.IOException;
import java.util.ArrayList;
import juegos.elementos.Partida;
import laberinto.OntologiaLaberinto;
import mouserun.game.GameUI;

/**
 *
 * @author admin
 */
public class ContenedorLaberinto {

    private final String idPartida;
    private final Partida partida;

    private final GameUI laberintoGUI;
    private final ArrayList<ResultadoRaton> ratonesPartida;

    private final int ancho;
    private final int alto;
    private final int tiempo;
    private final int quesosMax;
    private final int maxTrampas;

    private boolean objetivoQuesos;

    /**
     * Constructor parametrizado
     * @param t Tiempo maximo de la partida
     * @param mq Maximo de quesos que puede recoger un raton de la partida
     * @param mt Maximo de trampas que puede colocar un raton
     * @param alt Alto del laberinto
     * @param anc Ancho del laberinto
     * @param _id Id representativo de la partida
     * @param ge Gestor de la suscripciones a la partida
     * @param co Codec
     * @param ont Ontologia
     * @param ma Content Manager
     * @throws IOException
     * @throws InterruptedException 
     */
    public ContenedorLaberinto(int t, int mq, int mt, int alt, int anc, String _id, GestorSuscripciones ge, Codec co, Ontology ont, ContentManager ma) throws IOException, InterruptedException {
        this.alto = alt;
        this.ancho = anc;
        this.tiempo = t;
        this.quesosMax = mq;
        this.maxTrampas = mt;

        this.idPartida = _id;

        partida = new Partida(idPartida, OntologiaLaberinto.TIPO_JUEGO);

        laberintoGUI = new GameUI(idPartida, getAncho(), getAlto(), getQuesosMax(), getTiempo(), getMaxTrampas(), this, ge, co, ont, ma);
        laberintoGUI.setVisible(true);

        objetivoQuesos = false;
        ratonesPartida = new ArrayList();
    }

    /**
     * Añade un raton a la partida
     * @param rata Objeto de control de cada raton
     */
    public void insertarRaton(ResultadoRaton rata) {
        getRatonesPartida().add(rata);
    }

    /**
     * Establece que se ha logrado el objetivo de quesos
     */
    public void completarObjetivoQuesos() {
        objetivoQuesos = true;
    }

    /**
     * @return the idPartida
     */
    public String getIdPartida() {
        return idPartida;
    }

    /**
     * @return the partida
     */
    public Partida getPartida() {
        return partida;
    }

    /**
     * @return the laberintoGUI
     */
    public GameUI getLaberintoGUI() {
        return laberintoGUI;
    }

    /**
     * @return the ancho
     */
    public int getAncho() {
        return ancho;
    }

    /**
     * @return the alto
     */
    public int getAlto() {
        return alto;
    }

    /**
     * @return the tiempo
     */
    public int getTiempo() {
        return tiempo;
    }

    /**
     * @return the quesosMax
     */
    public int getQuesosMax() {
        return quesosMax;
    }

    /**
     * @return the maxTrampas
     */
    public int getMaxTrampas() {
        return maxTrampas;
    }

    /**
     * @return the objetivoQuesos
     */
    public boolean isObjetivoQuesos() {
        return objetivoQuesos;
    }

    /**
     * @return the ratonesPartida
     */
    public ArrayList<ResultadoRaton> getRatonesPartida() {
        return ratonesPartida;
    }
}
