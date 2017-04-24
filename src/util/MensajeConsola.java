/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

/**
 *
 * @author pedroj
 * Clase para el envío de mensajes a la consola del agente
 */
public class MensajeConsola {
    private String nombreAgente;
    private String contenido;

    public MensajeConsola(String nomAgente, String conenido) {
        this.nombreAgente = nomAgente;
        this.contenido = conenido;
    }

    public String getNombreAgente() {
        return nombreAgente;
    }

    public void setNombreAgente(String nombreAgente) {
        this.nombreAgente = nombreAgente;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    @Override
    public String toString() {
        return "Mensaje enviado por: " + nombreAgente + 
                "\nContenido: " + contenido + "\n";
    }
}
