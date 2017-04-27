/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import jade.core.AID;
import juegos.elementos.Posicion;

/**
 *
 * @author jcsp0003
 */
public class ResultadoRaton {

    private final AID aidRaton;
    private final String nombre;
    private Posicion pos;
    private int quesos;

    public ResultadoRaton(AID aidRaton, String nombre, int quesos) {
        this.aidRaton = aidRaton;
        this.nombre = nombre;
        this.quesos = quesos;
        this.pos = new Posicion(0, 0);
    }

    @Override
    public String toString() {
        return "     El raton " + getNombre() + "tiene " + getQuesos() + " quesos.";
    }

    public void incrementaQuesos() {
        ++quesos;
    }

    /**
     * @return the aidRaton
     */
    public AID getAidRaton() {
        return aidRaton;
    }

    /**
     * @return the nombre
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * @return the quesos
     */
    public int getQuesos() {
        return quesos;
    }

}
