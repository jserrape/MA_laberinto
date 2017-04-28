/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mouserun.game;

import java.awt.Color;
import java.io.IOException;
import javax.swing.JLabel;

/**
 *
 * @author jcsp0003
 */
public class Rata {

    private String nombre;
    private int x;
    private int y;
    private ImagedPanel panel;
    private JLabel label;
    private int GRID_LENGTH = 30;

    /**
     * Constructor parametrizado de la clase Rata
     *
     * @param _nombre Nombre del agente al que representa
     * @param _x Valor del eje x en que se crea
     * @param _y Valor del eje y en que se crea
     * @throws IOException An IOException can occur when the required game
     * assets are missing.
     */
    public Rata(String _nombre, int _x, int _y) throws IOException {
        this.nombre = _nombre;
        this.x = _x;
        this.y = _y;
        panel = new ImagedPanel("assets/mouseright.png", GRID_LENGTH, GRID_LENGTH);
        panel.setBounds(x * GRID_LENGTH, y * GRID_LENGTH, GRID_LENGTH * 2, 20);
        panel.setOpaque(false);
        label = new JLabel(nombre);
        label.setForeground(Color.RED);
        label.setBounds(x * GRID_LENGTH, y * GRID_LENGTH - 5, GRID_LENGTH * 2, 20);
    }

    /**
     * @return panel
     */
    public ImagedPanel getPanel() {
        return panel;
    }

    /**
     * Mueve la rata a una nueva posicion
     *
     * @param _x Nuevo valor en el eje x
     * @param _y Nuevo valor en el eje y
     */
    public void setPosicion(int _x, int _y) {
        this.x = _x;
        this.y = _y;
        panel.setBounds(getX() * GRID_LENGTH, getY() * GRID_LENGTH, GRID_LENGTH * 2, 20);
        label.setBounds(getX() * GRID_LENGTH, getY() * GRID_LENGTH - 5, GRID_LENGTH * 2, 20);
    }

    /**
     * @return label
     */
    public JLabel getJLabel() {
        return label;
    }

    /**
     * @return the x
     */
    public int getX() {
        return x;
    }

    /**
     * @return the y
     */
    public int getY() {
        return y;
    }

}
