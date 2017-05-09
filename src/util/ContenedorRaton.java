/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import juegos.elementos.Partida;
import juegos.elementos.Posicion;
import laberinto.OntologiaLaberinto;
import laberinto.elementos.EntornoLaberinto;
import laberinto.elementos.Laberinto;

/**
 *
 * @author jcsp0003
 */
public class ContenedorRaton {

    //Elementos de la partida
    private String id;
    private Partida partida;
    private Laberinto tablero;
    private Posicion posicion;
    private EntornoLaberinto entornoActual;

    //Atributos para control del juego
    private boolean muerto;
    private boolean nuevoQueso;

    private Posicion posicionQueso;

    //Elementos para el movimiento
    //Casillas visitadas durante la aparicion de este queso
    private Map<Integer, Posicion> casillasVisitadasQueso;
    //Pila para volver hacia atras
    private Stack<Integer> pila;
    //Clave del raton de la poscion actual
    private int claveActual;
    //Clave del posible movimiento a realizar
    private int auxClave;
    //Clave de la posicion del queso
    private int claveQueso;
    //Contador de pasos para colocar bombas
    private int bombas;
    //Contador de bambas que le quedan por colocar
    private int bombasRestantes;

    public ContenedorRaton(String _id, Partida _partida, Laberinto _tablero, Posicion _posicionQ, EntornoLaberinto _entornoActual, int _bombasRestantes) {
        this.id = _id;
        this.partida = _partida;
        this.tablero = _tablero;
        this.posicion = new Posicion(0, 0);
        this.entornoActual = _entornoActual;
        this.bombasRestantes = _bombasRestantes;
        this.posicionQueso = _posicionQ;

        nuevoQueso = false;
        muerto = false;
        casillasVisitadasQueso = new HashMap<>();
        pila = new Stack<>();
        claveQueso = 0;
        bombas = 0;
    }

    /**
     *
     * @return true si no pongo bomba, false si la pongo
     */
    public boolean moverse() {
        claveActual = funcionDeDispersion(getPosicion().getCoorX(), getPosicion().getCoorY(), 0);
        claveQueso = funcionDeDispersion(this.posicionQueso.getCoorX(), this.posicionQueso.getCoorY(), 0);

        if (nuevoQueso) {
            nuevoQueso = false;
            if (casillasVisitadasQueso.get(claveQueso) != null) {
                reinicio();
            }
        }

        if (muerto) {
            muerto = false;
            reinicio();
        }

        //Colocacion de las bombas
        bombas++;
        if ((bombas == 120 && bombasRestantes != 0) || (claveActual == claveQueso)) {
            bombas = 0;
            --bombasRestantes;
            return false;
        }
        if (casillasVisitadasQueso.get(claveActual) == null) {
            casillasVisitadasQueso.put(claveActual, new Posicion(getPosicion().getCoorX(), getPosicion().getCoorY()));
        }

        //Elegir un movimiento con prioridad
        for (int i = 1; i < 5; i++) {
            auxClave = funcionDeDispersion(getPosicion().getCoorX(), getPosicion().getCoorY(), i);
            if (meAcerco(i) && movimientoValido(i) && casillasVisitadasQueso.get(auxClave) == null) {
                pila.add(retrocede(i));
                aplicar(i);
                return true;
            }
        }

        //Elegir un movimiento sin prioridad
        for (int i = 1; i < 5; i++) {
            auxClave = funcionDeDispersion(getPosicion().getCoorX(), getPosicion().getCoorY(), i);
            if (movimientoValido(i) && casillasVisitadasQueso.get(auxClave) == null) {
                pila.add(retrocede(i));
                aplicar(i);
                return true;
            }
        }

        if (!pila.isEmpty()) {
            aplicar(pila.pop());
        } else {
            reinicio();
            moverse();
        }
        return true;
    }

    public void reinicio() {
        casillasVisitadasQueso.clear();
        pila.clear();
    }

    public int funcionDeDispersion(int x, int y, int direccion) {
        switch (direccion) {
            case 1: //Casilla de arriba
                y = y + 1;
                break;
            case 2: //Casilla de abajo
                y = y - 1;
                break;
            case 3: //Casilla de la izquierda
                x = x - 1;
                break;
            case 4: //Casilla de la derecha
                x = x + 1;
                break;
        }
        return (y << 3) + (x << 2) + (y << 5) + (x << 7);
    }

    public boolean meAcerco(int direccion) {
        switch (direccion) {
            case 1: //Arriba
                return posicionQueso.getCoorY() > getPosicion().getCoorY();
            case 2: //Abajo
                return posicionQueso.getCoorY() < getPosicion().getCoorY();
            case 3: //Izquierda
                return posicionQueso.getCoorX() < getPosicion().getCoorX();
            case 4: //Derecha
                return posicionQueso.getCoorX() > getPosicion().getCoorX();
        }
        return true;
    }

    public boolean movimientoValido(int direccion) {
        switch (direccion) {
            case 1: //Arriba
                return getEntornoActual().getNorte().equals(OntologiaLaberinto.LIBRE);
            case 2: //Abajo
                return getEntornoActual().getSur().equals(OntologiaLaberinto.LIBRE);
            case 3: //Izquierda
                return getEntornoActual().getOeste().equals(OntologiaLaberinto.LIBRE);
            case 4: //Derecha
                return getEntornoActual().getEste().equals(OntologiaLaberinto.LIBRE);
        }
        return false;
    }

    public int retrocede(int movimiento) {
        switch (movimiento) {
            case 1: //Arriba
                return 2;
            case 2: //Abajo
                return 1;
            case 3: //Izquierda
                return 4;
            case 4: //Derecha
                return 3;
        }
        return 0;
    }

    public void aplicar(int m) {
        int x = getPosicion().getCoorX();
        int y = getPosicion().getCoorY();
        switch (m) {
            case 1:
                getPosicion().setCoorY(y + 1);
                break;
            case 2:
                getPosicion().setCoorY(y - 1);
                break;
            case 3:
                getPosicion().setCoorX(x - 1);
                break;
            case 4:
                getPosicion().setCoorX(x + 1);
                break;
        }
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the partida
     */
    public Partida getPartida() {
        return partida;
    }

    /**
     * @return the tablero
     */
    public Laberinto getTablero() {
        return tablero;
    }

    /**
     * @return the posicion
     */
    public Posicion getPosicion() {
        return posicion;
    }

    /**
     * @return the entornoActual
     */
    public EntornoLaberinto getEntornoActual() {
        return entornoActual;
    }

    public void setEntorno(EntornoLaberinto ent) {
        this.entornoActual = ent;
    }

    public void matar() {
        muerto = true;
    }

    public void setPosicion(Posicion p) {
        this.posicion = p;
    }

    public void cambiarQueso(Posicion p) {
        this.posicionQueso = p;
        nuevoQueso = true;
    }
}
