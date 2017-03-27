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

    public ImagedPanel getPanel() {
        return panel;
    }

    public void setPosicion(int _x, int _y) {
        this.x = _x;
        this.y = _y;
        panel.setBounds(x * GRID_LENGTH, y * GRID_LENGTH, GRID_LENGTH * 2, 20);
        label.setBounds(x * GRID_LENGTH, y * GRID_LENGTH - 5, GRID_LENGTH * 2, 20);
    }

    public JLabel getJLabel() {
        return label;
    }

}
