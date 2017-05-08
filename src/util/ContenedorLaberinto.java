/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.io.IOException;
import juegos.elementos.Partida;
import laberinto.OntologiaLaberinto;
import mouserun.game.GameUI;

/**
 *
 * @author admin
 */
public class ContenedorLaberinto {

    private String idPartida;
    private Partida partida;

    private GameUI laberintoGUI;

    private int ancho;
    private int alto;
    private int tiempo;
    private int quesosMax;
    private int maxTrampas;

    private boolean objetivoQuesos;

    public ContenedorLaberinto(int t, int mq, int mt, int alt, int anc, String id) throws IOException, InterruptedException {
        this.alto = alt;
        this.ancho = anc;
        this.tiempo = t;
        this.quesosMax = mq;
        this.maxTrampas = mt;

        partida = new Partida(getIdPartida(), OntologiaLaberinto.TIPO_JUEGO);

        laberintoGUI = new GameUI(getAncho(), getAlto(), getQuesosMax(), getTiempo(), getMaxTrampas());
        laberintoGUI.setVisible(true);

        objetivoQuesos = false;
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
