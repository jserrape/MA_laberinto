/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

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

    /**
     * @return the ratonesPartida
     */
    public ArrayList<ResultadoRaton> getRatonesPartida() {
        return ratonesPartida;
    }

    private String idPartida;
    private Partida partida;

    private GameUI laberintoGUI;
    private ArrayList<ResultadoRaton> ratonesPartida;

    private int ancho;
    private int alto;
    private int tiempo;
    private int quesosMax;
    private int maxTrampas;

    private boolean objetivoQuesos;

    public ContenedorLaberinto(int t, int mq, int mt, int alt, int anc, String _id) throws IOException, InterruptedException {
        this.alto = alt;
        this.ancho = anc;
        this.tiempo = t;
        this.quesosMax = mq;
        this.maxTrampas = mt;
        
        this.idPartida=_id;

        partida = new Partida(idPartida, OntologiaLaberinto.TIPO_JUEGO);

        laberintoGUI = new GameUI(getAncho(), getAlto(), getQuesosMax(), getTiempo(), getMaxTrampas(),this);
        laberintoGUI.setVisible(true);

        objetivoQuesos = false;
        ratonesPartida = new ArrayList();
    }

    public void insertarRaton(ResultadoRaton rata) {
        getRatonesPartida().add(rata);
    }

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
}
